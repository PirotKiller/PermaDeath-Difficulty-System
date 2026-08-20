package me.pirot.permaDeathDifficultySystem.listeners;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles environmental mechanics that interact with player position and blocks:
 * - Torch damage (Day 14)
 * - Spawner light source destruction (Day 1)
 * These are standalone mechanics not tied to a single day handler.
 */
public class EnvironmentalEffectsListener implements Listener {

    private final PermaDeathDifficultySystem plugin;

    public EnvironmentalEffectsListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the periodic task for torch damage and spawner light destruction.
     */
    public void startTasks() {
        // Torch damage check — every 2 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkTorchDamage, 40L, 40L);

        // Spawner light source destruction — every 5 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkSpawnerLightDestruction, 100L, 100L);
    }

    /**
     * Day 14: Torches damage/burn players within 16 blocks.
     */
    private void checkTorchDamage() {
        DayManager dm = plugin.getDayManager();
        if (dm == null || !dm.isDayActive(14)) return;

        int radius = plugin.getConfigManager().getDaySetting(14, "torch-damage-radius", 16);
        double damage = plugin.getConfigManager().getDaySetting(14, "torch-damage-amount", 2.0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check if player has Olympic Torch relic (immune to torch damage)
            if (player.hasMetadata("pdds_torch_immune")) continue;

            Location pLoc = player.getLocation();
            // Search for torches within radius
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block block = pLoc.getWorld().getBlockAt(
                                pLoc.getBlockX() + x, pLoc.getBlockY() + y, pLoc.getBlockZ() + z);
                        Material mat = block.getType();
                        if (mat == Material.TORCH || mat == Material.WALL_TORCH
                                || mat == Material.SOUL_TORCH || mat == Material.SOUL_WALL_TORCH) {
                            player.damage(damage);
                            player.setFireTicks(40); // Set on fire for 2 seconds
                            return; // One hit per check
                        }
                    }
                }
            }
        }
    }

    /**
     * Day 1: Spawners disable/destroy nearby light sources within 16 blocks.
     */
    private void checkSpawnerLightDestruction() {
        DayManager dm = plugin.getDayManager();
        if (dm == null || !dm.isDayActive(1)) return;

        int radius = plugin.getConfigManager().getDaySetting(1, "spawner-light-destroy-radius", 16);

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                // Check for spawners in loaded chunks
                for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                    if (state.getType() == Material.SPAWNER) {
                        Location spawnerLoc = state.getLocation();
                        // Destroy light sources within radius
                        for (int x = -radius; x <= radius; x++) {
                            for (int y = -radius; y <= radius; y++) {
                                for (int z = -radius; z <= radius; z++) {
                                    Block block = world.getBlockAt(
                                            spawnerLoc.getBlockX() + x,
                                            spawnerLoc.getBlockY() + y,
                                            spawnerLoc.getBlockZ() + z);
                                    if (isLightSource(block.getType())) {
                                        block.setType(Material.AIR);
                                        world.playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isLightSource(Material mat) {
        return mat == Material.TORCH || mat == Material.WALL_TORCH
                || mat == Material.SOUL_TORCH || mat == Material.SOUL_WALL_TORCH
                || mat == Material.LANTERN || mat == Material.SOUL_LANTERN
                || mat == Material.GLOWSTONE || mat == Material.SEA_LANTERN
                || mat == Material.SHROOMLIGHT || mat == Material.JACK_O_LANTERN
                || mat == Material.CAMPFIRE || mat == Material.SOUL_CAMPFIRE
                || mat == Material.REDSTONE_TORCH || mat == Material.REDSTONE_WALL_TORCH
                || mat == Material.END_ROD;
    }

    /**
     * Day 14: Dangerous Redstone Torch mechanic — redstone torches near players deal extra damage.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerNearRedstoneTorch(PlayerMoveEvent event) {
        DayManager dm = plugin.getDayManager();
        if (dm == null || !dm.isDayActive(14)) return;

        Player player = event.getPlayer();
        // Throttle check
        if (player.hasMetadata("pdds_redstone_cd")) return;
        player.setMetadata("pdds_redstone_cd", new FixedMetadataValue(plugin, true));
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.removeMetadata("pdds_redstone_cd", plugin), 40L);

        Location loc = player.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block block = loc.getWorld().getBlockAt(
                            loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (block.getType() == Material.REDSTONE_TORCH || block.getType() == Material.REDSTONE_WALL_TORCH) {
                        // Redstone torches cause an explosion-like effect
                        player.damage(4.0);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 0, false, false));
                        loc.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
                        return;
                    }
                }
            }
        }
    }
}
