package me.pirot.permaDeathDifficultySystem.listeners;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * General mob spawn listener that applies Quantum Creeper logic,
 * Ender Quantum Creeper logic, and Enderman upgrades across multiple days.
 */
public class MobSpawnListener implements Listener {

    private final PermaDeathDifficultySystem plugin;

    public MobSpawnListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMobSpawn(CreatureSpawnEvent event) {
        DayManager dm = plugin.getDayManager();
        if (dm == null) return;

        LivingEntity entity = event.getEntity();

        // Day 11: Quantum Creepers — Creepers that teleport
        if (dm.isDayActive(11) && entity instanceof Creeper creeper) {
            if (plugin.getConfigManager().getDaySetting(11, "quantum-creepers", true)) {
                creeper.setCustomName("§d§lQuantum Creeper");
                creeper.setCustomNameVisible(true);
                creeper.setMetadata("pdds_quantum_creeper", new FixedMetadataValue(plugin, true));
                creeper.setPowered(true); // Charged creeper
                creeper.setExplosionRadius(5);

                AttributeInstance health = creeper.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(40.0);
                    creeper.setHealth(40.0);
                }
                creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            }
        }

        // Day 23: Ender Quantum Creepers — teleporting creepers that can appear in the End
        if (dm.isDayActive(23) && entity instanceof Creeper creeper) {
            if (plugin.getConfigManager().getDaySetting(23, "ender-quantum-creepers", true)) {
                creeper.setCustomName("§5§lEnder Quantum Creeper");
                creeper.setCustomNameVisible(true);
                creeper.setMetadata("pdds_ender_quantum_creeper", new FixedMetadataValue(plugin, true));
                creeper.setPowered(true);
                creeper.setExplosionRadius(7);

                AttributeInstance health = creeper.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(60.0);
                    creeper.setHealth(60.0);
                }
                creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));
                creeper.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
            }
        }

        // Day 6/17: Upgraded Endermen
        if (entity instanceof Enderman enderman) {
            if (dm.isDayActive(17)) {
                AttributeInstance health = enderman.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(60.0);
                    enderman.setHealth(60.0);
                }
                AttributeInstance damage = enderman.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (damage != null) {
                    damage.setBaseValue(15.0);
                }
                enderman.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
            } else if (dm.isDayActive(6)) {
                AttributeInstance health = enderman.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(50.0);
                    enderman.setHealth(50.0);
                }
            }
        }

        // Day 6: Upgraded Outpost Illagers
        if (dm.isDayActive(6)) {
            if (entity instanceof Pillager pillager) {
                AttributeInstance health = pillager.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(30.0);
                    pillager.setHealth(30.0);
                }
                pillager.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            }
        }

        // Day 7: Witch special potions
        if (dm.isDayActive(7) && entity instanceof Witch witch) {
            AttributeInstance health = witch.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40.0);
                witch.setHealth(40.0);
            }
            witch.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            witch.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        }
    }
}
