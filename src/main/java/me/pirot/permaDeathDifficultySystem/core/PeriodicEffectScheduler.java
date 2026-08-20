package me.pirot.permaDeathDifficultySystem.core;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Schedules periodic debuff effects that trigger every 30 minutes based on the current day.
 * Day 3: Hunger
 * Day 8: Mining Fatigue
 * Day 12: Weakness
 * Day 16: Slowness
 * Day 18: Poison II
 * Day 21: Wither
 */
public class PeriodicEffectScheduler {

    private final PermaDeathDifficultySystem plugin;
    private BukkitTask schedulerTask;

    // Default interval: 30 minutes = 36000 ticks
    private static final long DEFAULT_INTERVAL_TICKS = 36000L;
    // Effect duration: 30 seconds
    private static final int EFFECT_DURATION_TICKS = 600;

    public PeriodicEffectScheduler(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the periodic effect scheduler. Runs every 30 minutes (configurable per-effect).
     */
    public void start() {
        stop();

        // Check every 30 seconds and apply the appropriate periodic effects
        // This allows different intervals per effect if configured
        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyPeriodicEffects,
                DEFAULT_INTERVAL_TICKS, DEFAULT_INTERVAL_TICKS);

        plugin.getLogger().info("Periodic effect scheduler started.");
    }

    /**
     * Stops the periodic effect scheduler.
     */
    public void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
    }

    /**
     * Applies all applicable periodic effects to online players based on the current day.
     */
    private void applyPeriodicEffects() {
        DayManager dayManager = plugin.getDayManager();
        if (dayManager == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Day 3: Hunger
            if (dayManager.isDayActive(3)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, EFFECT_DURATION_TICKS, 0, false, true, true));
            }

            // Day 8: Mining Fatigue
            if (dayManager.isDayActive(8)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, EFFECT_DURATION_TICKS, 0, false, true, true));
            }

            // Day 12: Weakness
            if (dayManager.isDayActive(12)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, EFFECT_DURATION_TICKS, 0, false, true, true));
            }

            // Day 16: Slowness
            if (dayManager.isDayActive(16)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, EFFECT_DURATION_TICKS, 0, false, true, true));
            }

            // Day 18: Poison II
            if (dayManager.isDayActive(18)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, EFFECT_DURATION_TICKS, 1, false, true, true));
            }

            // Day 21: Wither
            if (dayManager.isDayActive(21)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, EFFECT_DURATION_TICKS, 0, false, true, true));
            }
        }
    }
}
