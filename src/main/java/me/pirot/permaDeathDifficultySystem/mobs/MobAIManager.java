package me.pirot.permaDeathDifficultySystem.mobs;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * The single shared targeting service. Both the standalone "intelligent targeting" toggle and
 * the day-progression range increases (Day 10, Day 20) go through here.
 *
 * <p>Long-range targeting is delegated to the vanilla AI by setting each mob's
 * {@code GENERIC_FOLLOW_RANGE} attribute on spawn (see {@link MobAttributeManager}). This
 * repeating task exists only for the part vanilla cannot do: acquiring a target through walls.
 * Its radius is deliberately capped well below the Day 20 follow range of 256, because a
 * wall-ignoring sweep at that distance would pull every loaded mob onto a player at once.
 */
public class MobAIManager implements Listener {

    /** Upper bound on the through-wall sweep, independent of the vanilla follow range. */
    private static final int MAX_WALL_TARGET_RADIUS = 48;

    private final PermaDeathDifficultySystem plugin;
    private final MobAttributeManager attributeManager;

    public MobAIManager(PermaDeathDifficultySystem plugin, MobAttributeManager attributeManager) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
    }

    public void startTasks() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::forceTargeting, 60L, 60L);
    }

    /**
     * Applies the day's attribute scaling — including follow range, which is what actually
     * implements the Day 10 / Day 20 targeting-range increases.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        attributeManager.applyDayModifiers(event.getEntity());
    }

    /**
     * Gives nearby mobs a target even without line of sight.
     *
     * <p>Iterates players and asks for entities near each one, rather than walking every entity
     * in every world — the latter scales with world size instead of with player count.
     */
    private void forceTargeting() {
        if (!plugin.getConfigManager().isWallTargeting()) return;

        DayManager dayManager = plugin.getDayManager();
        if (dayManager == null) return;

        int radius = Math.min(plugin.getConfigManager().getDefaultDetectionRange(), MAX_WALL_TARGET_RADIUS);
        boolean invisibilityCounter = plugin.getConfigManager().isInvisibilityCounter();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (invisibilityCounter && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
            if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof Monster mob)) continue;
                if (mob.getTarget() != null) continue;
                mob.setTarget(player);
            }
        }
    }

    /**
     * Invisibility breaks an existing lock-on, per the brief.
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
