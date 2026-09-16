package me.pirot.permaDeathDifficultySystem.core;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spatial index of light-source blocks, so proximity queries don't have to scan a
 * 33x33x33 volume on a timer.
 *
 * <p>A naive "is there a torch within 16 blocks" check costs ~36k block reads per query.
 * Run per player every two seconds, that alone will flatten a server's TPS. Instead each
 * 16x16x16 chunk section is scanned once, off the main thread via a {@link ChunkSnapshot},
 * and the result cached. Queries then only walk the handful of light sources actually present.
 *
 * <p>The cache is kept honest by block events; a section that has not been scanned yet simply
 * reports nothing until its async scan lands, which is fine for the hazard mechanics that use it.
 */
public final class BlockIndex implements Listener {

    /** Blocks worth remembering. Callers filter this down to what they care about. */
    public static final Set<Material> LIGHT_SOURCES = EnumSet.of(
            Material.TORCH, Material.WALL_TORCH,
            Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH,
            Material.LANTERN, Material.SOUL_LANTERN,
            Material.GLOWSTONE, Material.SEA_LANTERN,
            Material.SHROOMLIGHT, Material.JACK_O_LANTERN,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.END_ROD, Material.FIRE, Material.SOUL_FIRE);

    /** Plain torches only — the Day 14 burn mechanic. */
    public static final Set<Material> TORCHES = EnumSet.of(
            Material.TORCH, Material.WALL_TORCH,
            Material.SOUL_TORCH, Material.SOUL_WALL_TORCH);

    /** Redstone torches only — the Day 14 redstone mechanic. */
    public static final Set<Material> REDSTONE_TORCHES = EnumSet.of(
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH);

    private record SectionKey(UUID world, int chunkX, int sectionY, int chunkZ) {}

    private final PermaDeathDifficultySystem plugin;

    /** Scanned sections: section -> (packed block position -> material). */
    private final Map<SectionKey, Map<Long, Material>> index = new ConcurrentHashMap<>();
    /** Sections with an async scan in flight, so we never queue the same one twice. */
    private final Set<SectionKey> inFlight = ConcurrentHashMap.newKeySet();

    public BlockIndex(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    // ==================== Position packing ====================
    // x: bits 38-63 (26 signed), z: bits 12-37 (26 signed), y: bits 0-11 (12 signed)

    private static long pack(int x, int y, int z) {
        return ((long) x << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    private static int unpackZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    private static int unpackY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    // ==================== Queries ====================

    /**
     * Returns true if any block of one of the given types sits within {@code radius} of
     * {@code center}. Cheaper than {@link #findNear} when only presence matters.
     */
    public boolean isAnyNear(Location center, int radius, Set<Material> types) {
        return !collect(center, radius, types, true).isEmpty();
    }

    /**
     * Returns every indexed block of one of the given types within {@code radius} of {@code center}.
     */
    public List<Location> findNear(Location center, int radius, Set<Material> types) {
        return collect(center, radius, types, false);
    }

    private List<Location> collect(Location center, int radius, Set<Material> types, boolean stopAtFirst) {
        List<Location> hits = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return hits;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), cy - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, cy + radius);
        long radiusSq = (long) radius * radius;

        for (int chunkX = (cx - radius) >> 4; chunkX <= (cx + radius) >> 4; chunkX++) {
            for (int chunkZ = (cz - radius) >> 4; chunkZ <= (cz + radius) >> 4; chunkZ++) {
                ensureIndexed(world, chunkX, chunkZ, minY, maxY);

                for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                    Map<Long, Material> section =
                            index.get(new SectionKey(world.getUID(), chunkX, sectionY, chunkZ));
                    if (section == null) continue;

                    for (Map.Entry<Long, Material> entry : section.entrySet()) {
                        if (!types.contains(entry.getValue())) continue;

                        long packed = entry.getKey();
                        int x = unpackX(packed);
                        int y = unpackY(packed);
                        int z = unpackZ(packed);
                        long dx = x - cx, dy = y - cy, dz = z - cz;
                        if (dx * dx + dy * dy + dz * dz > radiusSq) continue;

                        hits.add(new Location(world, x, y, z));
                        if (stopAtFirst) return hits;
                    }
                }
            }
        }
        return hits;
    }

    // ==================== Indexing ====================

    /**
     * Schedules an async scan of any not-yet-indexed sections of this chunk covering [minY, maxY].
     * Returns immediately; results land on a later tick.
     */
    private void ensureIndexed(World world, int chunkX, int chunkZ, int minY, int maxY) {
        UUID worldId = world.getUID();
        List<Integer> needed = new ArrayList<>();

        for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
            SectionKey key = new SectionKey(worldId, chunkX, sectionY, chunkZ);
            if (index.containsKey(key)) continue;
            if (inFlight.add(key)) needed.add(sectionY);
        }
        if (needed.isEmpty()) return;

        // A snapshot can only be taken on the main thread, but is safe to read off it.
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            for (int sectionY : needed) {
                inFlight.remove(new SectionKey(worldId, chunkX, sectionY, chunkZ));
            }
            return;
        }
        ChunkSnapshot snapshot = world.getChunkAt(chunkX, chunkZ).getChunkSnapshot(false, false, false);
        int worldMin = world.getMinHeight();
        int worldMax = world.getMaxHeight();

        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> scanSections(worldId, chunkX, chunkZ, needed, snapshot, worldMin, worldMax));
    }

    private void scanSections(UUID worldId, int chunkX, int chunkZ, List<Integer> sections,
                              ChunkSnapshot snapshot, int worldMin, int worldMax) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        Map<SectionKey, Map<Long, Material>> results = new HashMap<>();

        for (int sectionY : sections) {
            Map<Long, Material> found = new HashMap<>();
            int y0 = Math.max(worldMin, sectionY << 4);
            int y1 = Math.min(worldMax - 1, (sectionY << 4) + 15);

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = y0; y <= y1; y++) {
                        Material material = snapshot.getBlockType(x, y, z);
                        if (LIGHT_SOURCES.contains(material)) {
                            found.put(pack(baseX + x, y, baseZ + z), material);
                        }
                    }
                }
            }
            results.put(new SectionKey(worldId, chunkX, sectionY, chunkZ), found);
        }

        // Commit on the main thread. Publishing straight from here would race the block-event
        // updates and could resurrect a section that unloaded while the scan was running.
        if (!plugin.isEnabled()) {
            results.keySet().forEach(inFlight::remove);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            World world = Bukkit.getWorld(worldId);
            boolean stillLoaded = world != null && world.isChunkLoaded(chunkX, chunkZ);

            for (SectionKey key : results.keySet()) {
                if (stillLoaded) {
                    index.put(key, results.get(key));
                }
                inFlight.remove(key);
            }
        });
    }

    /** Applies a single block change to the cache, if that section has been scanned. */
    private void update(Location location, Material material) {
        World world = location.getWorld();
        if (world == null) return;

        SectionKey key = new SectionKey(world.getUID(),
                location.getBlockX() >> 4, location.getBlockY() >> 4, location.getBlockZ() >> 4);
        Map<Long, Material> section = index.get(key);
        if (section == null) return; // not scanned yet; the eventual scan will see the new state

        long packed = pack(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (LIGHT_SOURCES.contains(material)) {
            section.put(packed, material);
        } else {
            section.remove(packed);
        }
    }

    /** Records a block the plugin itself changed, keeping the cache in step. */
    public void onBlockChanged(Block block, Material newType) {
        update(block.getLocation(), newType);
    }

    // ==================== Cache maintenance ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        update(event.getBlock().getLocation(), event.getBlock().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        update(event.getBlock().getLocation(), Material.AIR);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        update(event.getBlock().getLocation(), Material.AIR);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        update(event.getBlock().getLocation(), event.getNewState().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            update(block.getLocation(), Material.AIR);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        World world = event.getWorld();
        UUID worldId = world.getUID();
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();

        // Drop this chunk's sections by direct key. Chunk unloads are frequent, so scanning
        // the whole index with removeIf would make eviction cost scale with total cache size.
        for (int sectionY = world.getMinHeight() >> 4; sectionY <= (world.getMaxHeight() - 1) >> 4; sectionY++) {
            SectionKey key = new SectionKey(worldId, chunkX, sectionY, chunkZ);
            index.remove(key);
            inFlight.remove(key);
        }
    }

    /** Drops the entire cache. Used on config reload. */
    public void clear() {
        index.clear();
        inFlight.clear();
    }
}
