package me.pirot.permaDeathDifficultySystem.mobs;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;

/**
 * Applies general attribute scaling to mobs based on the current day.
 * Handles the progressive stat increases that aren't specific to a mob type.
 */
public class MobAttributeManager {

    private final PermaDeathDifficultySystem plugin;

    public MobAttributeManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Applies day-appropriate attribute modifiers to a mob.
     * Called by the mob spawn listener for general scaling.
     */
    public void applyDayModifiers(LivingEntity entity) {
        DayManager dm = plugin.getDayManager();
        if (dm == null) return;

        if (!(entity instanceof Monster)) return;

        int currentDay = dm.getCurrentDay();

        // Base health scaling: +2% per day
        AttributeInstance health = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) {
            double baseHealth = health.getBaseValue();
            double dayBonus = 1.0 + (currentDay * 0.02);
            health.setBaseValue(baseHealth * dayBonus);
            entity.setHealth(health.getBaseValue());
        }

        // Base damage scaling: +1% per day
        AttributeInstance damage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damage != null) {
            double baseDamage = damage.getBaseValue();
            double dayBonus = 1.0 + (currentDay * 0.01);
            damage.setBaseValue(baseDamage * dayBonus);
        }

        // Speed scaling after day 15: +0.5% per day
        if (currentDay >= 15) {
            AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speed != null) {
                double baseSpeed = speed.getBaseValue();
                double dayBonus = 1.0 + ((currentDay - 14) * 0.005);
                speed.setBaseValue(baseSpeed * dayBonus);
            }
        }

        // Follow range scaling
        AttributeInstance followRange = entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (followRange != null) {
            if (dm.isDayActive(20)) {
                followRange.setBaseValue(256.0);
            } else if (dm.isDayActive(10)) {
                followRange.setBaseValue(100.0);
            } else if (currentDay >= 5) {
                followRange.setBaseValue(Math.min(48.0 + currentDay * 2.0, 100.0));
            }
        }
    }
}
