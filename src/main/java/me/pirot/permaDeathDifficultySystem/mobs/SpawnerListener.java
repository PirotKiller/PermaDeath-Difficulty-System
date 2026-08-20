package me.pirot.permaDeathDifficultySystem.mobs;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import java.util.Random;

/**
 * Handles spawner-specific mechanics:
 * Day 1: Light source destruction (handled in EnvironmentalEffectsListener)
 * Day 8: Spawners summon 5-10 mobs at once
 * Day 14: Harder spawners (reduced delay)
 * Day 29: Spawners increase spawning speed when being mined
 */
public class SpawnerListener implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final Random random = new Random();

    public SpawnerListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Day 8: When a spawner activates, spawn extra mobs (5-10 burst).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        DayManager dm = plugin.getDayManager();
        if (dm == null) return;

        // Day 8: Burst spawning
        if (dm.isDayActive(8)) {
            int min = plugin.getConfigManager().getDaySetting(8, "spawner-burst-min", 5);
            int max = plugin.getConfigManager().getDaySetting(8, "spawner-burst-max", 10);
            int extraSpawns = min + random.nextInt(max - min + 1) - 1; // -1 because one already spawned

            CreatureSpawner spawner = event.getSpawner();
            EntityType type = spawner.getSpawnedType();
            Location loc = event.getLocation();

            for (int i = 0; i < extraSpawns; i++) {
                Location spawnLoc = loc.clone().add(
                        random.nextDouble() * 4 - 2,
                        random.nextDouble() * 2,
                        random.nextDouble() * 4 - 2
                );
                // Ensure the location is safe
                if (spawnLoc.getBlock().getType() == Material.AIR) {
                    loc.getWorld().spawnEntity(spawnLoc, type);
                }
            }
        }

        // Day 14: Make spawners harder (this is handled by modifying the spawner delay)
        if (dm.isDayActive(14)) {
            CreatureSpawner spawner = event.getSpawner();
            spawner.setDelay(Math.max(100, spawner.getDelay() - 100)); // Reduce delay
            spawner.setMaxNearbyEntities(spawner.getMaxNearbyEntities() + 10);
        }
    }

    /**
     * Day 29: Mining a spawner increases its spawning speed temporarily.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerMine(BlockBreakEvent event) {
        DayManager dm = plugin.getDayManager();
        if (dm == null || !dm.isDayActive(29)) return;

        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        if (!plugin.getConfigManager().getDaySetting(29, "spawner-mine-speed-increase", true)) return;

        // Instead of breaking, the spawner spawns a burst of mobs
        event.setCancelled(true);

        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        EntityType type = spawner.getSpawnedType();
        Location loc = block.getLocation().add(0.5, 1, 0.5);

        // Spawn 5-10 mobs rapidly
        int count = 5 + random.nextInt(6);
        for (int i = 0; i < count; i++) {
            Location spawnLoc = loc.clone().add(
                    random.nextDouble() * 6 - 3,
                    random.nextDouble() * 2,
                    random.nextDouble() * 6 - 3
            );
            if (spawnLoc.getBlock().getType() == Material.AIR) {
                loc.getWorld().spawnEntity(spawnLoc, type);
            }
        }

        // Now reduce spawner health (damage it — after 3 attempts, it breaks)
        if (!block.hasMetadata("pdds_mine_count")) {
            block.setMetadata("pdds_mine_count",
                    new org.bukkit.metadata.FixedMetadataValue(plugin, 1));
        } else {
            int count2 = block.getMetadata("pdds_mine_count").get(0).asInt() + 1;
            if (count2 >= 3) {
                // Break the spawner after 3 attempts
                block.setType(Material.AIR);
                event.getPlayer().sendMessage(
                        net.kyori.adventure.text.Component.text("§a§lSpawner destroyed after intense battle!"));
            } else {
                block.setMetadata("pdds_mine_count",
                        new org.bukkit.metadata.FixedMetadataValue(plugin, count2));
            }
        }

        block.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 0.5f);
        event.getPlayer().sendMessage(
                net.kyori.adventure.text.Component.text("§c§lThe spawner fights back! Mobs pour out!"));
    }
}
