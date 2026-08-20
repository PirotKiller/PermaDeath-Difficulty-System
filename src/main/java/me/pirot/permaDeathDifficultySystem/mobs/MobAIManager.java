package me.pirot.permaDeathDifficultySystem.mobs;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Manages custom AI targeting behaviors:
 * - Wall-targeting (mobs can detect through walls)
 * - Invisibility counter (Invisibility causes mobs to lose target)
 * - Extended detection ranges
 */
public class MobAIManager implements Listener {

    private final PermaDeathDifficultySystem plugin;

    public MobAIManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Start periodic AI targeting tasks.
     */
    public void startTasks() {
        // Force-target nearest player periodically (wall-targeting)
        Bukkit.getScheduler().runTaskTimer(plugin, this::forceTargeting, 60L, 60L);
    }

    /**
     * Forces mobs to target the nearest player even through walls.
     */
    private void forceTargeting() {
        if (!plugin.getConfigManager().isWallTargeting()) return;

        DayManager dm = plugin.getDayManager();
        if (dm == null) return;

        int detectionRange = plugin.getConfigManager().getDefaultDetectionRange();
        if (dm.isDayActive(20)) {
            detectionRange = 256;
        } else if (dm.isDayActive(10)) {
            detectionRange = 100;
        }

        final int range = detectionRange;

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Monster mob)) continue;
                if (mob.getTarget() != null) continue; // Already has a target

                // Find nearest player
                Player nearest = null;
                double nearestDist = range * range;
                for (Player p : world.getPlayers()) {
                    // Invisibility counter: invisible players are ignored
                    if (plugin.getConfigManager().isInvisibilityCounter()
                            && p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        continue;
                    }

                    double dist = p.getLocation().distanceSquared(mob.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = p;
                    }
                }

                if (nearest != null) {
                    mob.setTarget(nearest);
                }
            }
        }
    }

    /**
     * Invisibility counter: When a player gains Invisibility, mobs lose their target.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!plugin.getConfigManager().isInvisibilityCounter()) return;
        if (!(event.getTarget() instanceof Player player)) return;

        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            event.setCancelled(true);
        }
    }
}
