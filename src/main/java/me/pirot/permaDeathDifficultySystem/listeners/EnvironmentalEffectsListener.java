package me.pirot.permaDeathDifficultySystem.listeners;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.BlockIndex;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Environmental hazards that depend on nearby blocks:
 * <ul>
 *   <li>Day 1 — spawners destroy nearby light sources</li>
 *   <li>Day 14 — torches burn players within range</li>
 *   <li>Day 14 — redstone torches detonate on nearby players</li>
 * </ul>
 *
 * All three resolve "what blocks are near X" through {@link BlockIndex} rather than
 * scanning the raw block volume, which would otherwise cost tens of thousands of
 * block reads per player per tick cycle.
 */
public class EnvironmentalEffectsListener implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final List<BukkitTask> tasks = new ArrayList<>();

    public EnvironmentalEffectsListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    public void startTasks() {
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::checkTorchDamage, 40L, 40L));
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::checkRedstoneTorches, 40L, 40L));
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::checkSpawnerLightDestruction, 100L, 100L));
    }

    public void stopTasks() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    /** Day 14: torches burn players within range. */
    private void checkTorchDamage() {
        DayManager dayManager = plugin.getDayManager();
        if (dayManager == null || !dayManager.isDayActive(14)) return;

        int radius = plugin.getConfigManager().getDaySetting(14, "torch-damage-radius", 16);
        double damage = plugin.getConfigManager().getDaySetting(14, "torch-damage-amount", 2.0);
        BlockIndex blockIndex = plugin.getBlockIndex();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // The Olympic Torch relic exempts its holder.
            if (player.hasMetadata("pdds_torch_immune")) continue;
            if (player.isDead() || !player.getWorld().isChunkLoaded(
                    player.getLocation().getBlockX() >> 4, player.getLocation().getBlockZ() >> 4)) continue;

            if (blockIndex.isAnyNear(player.getLocation(), radius, BlockIndex.TORCHES)) {
                player.damage(damage);
                player.setFireTicks(40);
            }
        }
    }

    /** Day 14: redstone torches detonate on players who get close. */
    private void checkRedstoneTorches() {
        DayManager dayManager = plugin.getDayManager();
        if (dayManager == null || !dayManager.isDayActive(14)) return;

        int radius = plugin.getConfigManager().getDaySetting(14, "redstone-torch-radius", 3);
        double damage = plugin.getConfigManager().getDaySetting(14, "redstone-torch-damage", 4.0);
        BlockIndex blockIndex = plugin.getBlockIndex();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) continue;

            List<Location> torches =
                    blockIndex.findNear(player.getLocation(), radius, BlockIndex.REDSTONE_TORCHES);
            if (torches.isEmpty()) continue;

            player.damage(damage);
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 0, false, false, true));
            Location torch = torches.get(0);
            player.getWorld().playSound(torch, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        }
    }

    /**
     * Day 1: spawners snuff out light sources around them.
     *
     * <p>Only chunks near a player are considered — an unobserved spawner darkening its
     * surroundings has no gameplay effect, and sweeping every loaded chunk is what made
     * the original version unusable.
     */
    private void checkSpawnerLightDestruction() {
        DayManager dayManager = plugin.getDayManager();
        if (dayManager == null || !dayManager.isDayActive(1)) return;

        int radius = plugin.getConfigManager().getDaySetting(1, "spawner-light-destroy-radius", 16);
        BlockIndex blockIndex = plugin.getBlockIndex();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Chunk center = player.getLocation().getChunk();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!player.getWorld().isChunkLoaded(center.getX() + dx, center.getZ() + dz)) continue;
                    Chunk chunk = player.getWorld().getChunkAt(center.getX() + dx, center.getZ() + dz);

                    for (BlockState state : chunk.getTileEntities()) {
                        if (state.getType() != Material.SPAWNER) continue;
                        snuffLightsAround(blockIndex, state.getLocation(), radius);
                    }
                }
            }
        }
    }

    private void snuffLightsAround(BlockIndex blockIndex, Location spawner, int radius) {
        for (Location light : blockIndex.findNear(spawner, radius, BlockIndex.LIGHT_SOURCES)) {
            Block block = light.getBlock();
            if (!BlockIndex.LIGHT_SOURCES.contains(block.getType())) continue;

            block.setType(Material.AIR);
            blockIndex.onBlockChanged(block, Material.AIR);
            block.getWorld().playSound(light, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
        }
    }
}
