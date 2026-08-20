package me.pirot.permaDeathDifficultySystem.mobs;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles proximity-based aura effects from mobs:
 * Day 4: Wolves apply Hunger within 16 blocks
 * Day 5: Cats apply Darkness within 16 blocks
 * Day 13: Phantoms apply extreme Levitation nearby
 * Day 15: Axolotls apply Poison II within 16 blocks
 * Day 17: Warden applies extreme Slowness within 20 blocks
 * Day 19: Bats cause Blindness within 16 blocks
 */
public class AuraEffectListener {

    private final PermaDeathDifficultySystem plugin;

    public AuraEffectListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the periodic aura check task.
     */
    public void startTask() {
        // Check every 2 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAuras, 40L, 40L);
    }

    private void checkAuras() {
        DayManager dm = plugin.getDayManager();
        if (dm == null) return;

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {

                // Day 4: Wolf Hunger aura
                if (dm.isDayActive(4)) {
                    int radius = plugin.getConfigManager().getDaySetting(4, "wolf-hunger-radius", 16);
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof Wolf) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 0, false, false, true));
                            break;
                        }
                    }
                }

                // Day 5: Cat Darkness aura
                if (dm.isDayActive(5)) {
                    int radius = plugin.getConfigManager().getDaySetting(5, "cat-darkness-radius", 16);
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof Cat) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false, true));
                            break;
                        }
                    }
                }

                // Day 13: Phantom Levitation aura
                if (dm.isDayActive(13)) {
                    int radius = plugin.getConfigManager().getDaySetting(13, "phantom-levitation-radius", 16);
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof Phantom) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 4, false, false, true));
                            break;
                        }
                    }
                }

                // Day 15: Axolotl Poison II aura
                if (dm.isDayActive(15)) {
                    int radius = plugin.getConfigManager().getDaySetting(15, "axolotl-poison-radius", 16);
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof Axolotl) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1, false, false, true));
                            break;
                        }
                    }
                }

                // Day 17: Warden extreme Slowness aura
                if (dm.isDayActive(17)) {
                    int radius = plugin.getConfigManager().getDaySetting(17, "warden-slowness-radius", 20);
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof Warden) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 4, false, false, true));
                            break;
                        }
                    }
                }

                // Day 19: Bat Blindness aura
                if (dm.isDayActive(19)) {
                    int radius = plugin.getConfigManager().getDaySetting(19, "bat-blindness-radius", 16);
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof Bat) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false, true));
                            break;
                        }
                    }
                }
            }
        }
    }
}
