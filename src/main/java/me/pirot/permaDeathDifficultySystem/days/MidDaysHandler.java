package me.pirot.permaDeathDifficultySystem.days;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Handles mechanics for Days 8–14:
 * Day 8: Nether open; Mining Fatigue; spawner burst 5-10; Giant Slimes; Slime upgrades
 * Day 9: Salmon→Drowned; raids harder; Drowned/Evoker/Ravager/Vindicator/Vex upgrades
 * Day 10: Neutral/passive mobs hostile; targeting range 100; Blaze/Piglin upgrades
 * Day 11: Lightning on bed carriers; Totem 10% failure; Quantum Creepers; Wither Skeleton upgrades
 * Day 12: Weakness every 30min; mobs +30% speed; Zombie/Skeleton upgrades
 * Day 13: Squid→Elder Guardian; Cod→Guardian; Giant Magma Cubes; Phantom Levitation
 * Day 14: Diamond armor lightning; natural regen disabled; torch damage; harder spawners; spider upgrades
 */
public class MidDaysHandler extends DayHandler {

    private final Random random = new Random();

    public MidDaysHandler(PermaDeathDifficultySystem plugin) {
        super(plugin);
    }

    @Override
    public boolean handlesDay(int day) {
        return day >= 8 && day <= 14;
    }

    @Override
    public void onDayStart(int day) {
        // Day 14: Disable natural regeneration
        if (day >= 14 && isDayActive(14)) {
            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.NATURAL_REGENERATION, false);
            }
        }
    }

    // ==================== Day 8: Giant Slimes ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onSlimeSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(8)) return;

        // Giant Slimes
        if (event.getEntity() instanceof Slime slime && !(event.getEntity() instanceof MagmaCube)) {
            if (plugin.getConfigManager().getDaySetting(8, "giant-slimes-enabled", true)) {
                if (random.nextDouble() < 0.15) { // 15% chance to be a giant slime
                    slime.setSize(10);
                    slime.setCustomName("§a§lGiant Slime");
                    slime.setCustomNameVisible(true);
                    AttributeInstance health = slime.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (health != null) {
                        health.setBaseValue(100.0);
                        slime.setHealth(100.0);
                    }
                } else {
                    // Upgrade normal slimes
                    slime.setSize(Math.min(slime.getSize() + 2, 8));
                }
            }
        }
    }

    // ==================== Day 9: Salmon replaced by Drowned ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onFishSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(9)) return;

        if (event.getEntity() instanceof Salmon) {
            if (plugin.getConfigManager().getDaySetting(9, "salmon-replaced-by-drowned", true)) {
                event.setCancelled(true);
                Location loc = event.getLocation();
                Drowned drowned = (Drowned) loc.getWorld().spawnEntity(loc, EntityType.DROWNED);
                drowned.setCustomName("§3Abyssal Drowned");
                drowned.setCustomNameVisible(true);

                AttributeInstance health = drowned.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(50.0);
                    drowned.setHealth(50.0);
                }
                AttributeInstance damage = drowned.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (damage != null) {
                    damage.setBaseValue(12.0);
                }
                drowned.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));

                // Give trident
                ItemStack trident = new ItemStack(Material.TRIDENT);
                ItemMeta meta = trident.getItemMeta();
                meta.addEnchant(Enchantment.LOYALTY, 3, true);
                trident.setItemMeta(meta);
                drowned.getEquipment().setItemInMainHand(trident);
                drowned.getEquipment().setItemInMainHandDropChance(0.0f);
            }
        }
    }

    // ==================== Day 9: Upgraded raid mobs ====================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRaidMobSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(9)) return;
        LivingEntity entity = event.getEntity();

        if (entity instanceof Evoker evoker) {
            AttributeInstance health = evoker.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(48.0);
                evoker.setHealth(48.0);
            }
        }

        if (entity instanceof Ravager ravager) {
            AttributeInstance health = ravager.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(200.0);
                ravager.setHealth(200.0);
            }
            AttributeInstance damage = ravager.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(24.0);
            }
        }

        if (entity instanceof Vindicator vindicator) {
            AttributeInstance health = vindicator.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(48.0);
                vindicator.setHealth(48.0);
            }
            // Give upgraded axe
            ItemStack axe = new ItemStack(Material.IRON_AXE);
            ItemMeta meta = axe.getItemMeta();
            meta.addEnchant(Enchantment.DAMAGE_ALL, 3, true);
            axe.setItemMeta(meta);
            vindicator.getEquipment().setItemInMainHand(axe);
            vindicator.getEquipment().setItemInMainHandDropChance(0.05f);
        }

        if (entity instanceof Vex vex) {
            AttributeInstance health = vex.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(28.0);
                vex.setHealth(28.0);
            }
            AttributeInstance damage = vex.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(12.0);
            }
        }

        // Illager crossbow users
        if (entity instanceof Pillager pillager) {
            AttributeInstance health = pillager.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40.0);
                pillager.setHealth(40.0);
            }
            ItemStack crossbow = new ItemStack(Material.CROSSBOW);
            ItemMeta meta = crossbow.getItemMeta();
            meta.addEnchant(Enchantment.QUICK_CHARGE, 3, true);
            meta.addEnchant(Enchantment.PIERCING, 3, true);
            crossbow.setItemMeta(meta);
            pillager.getEquipment().setItemInMainHand(crossbow);
            pillager.getEquipment().setItemInMainHandDropChance(0.05f);
        }
    }

    // ==================== Day 10: Passive/neutral mobs become hostile ====================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPassiveMobSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(10)) return;
        LivingEntity entity = event.getEntity();

        if (entity instanceof Animals && !(entity instanceof Tameable && ((Tameable) entity).isTamed())) {
            // Mark passive mobs as "hostile" via metadata
            entity.setMetadata("pdds_hostile", new FixedMetadataValue(plugin, true));

            // Boost passive mob stats so they are dangerous
            AttributeInstance damage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(6.0);
            }
            AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * 1.5);
            }
        }

        // Day 10: Blaze upgrades
        if (entity instanceof Blaze blaze) {
            AttributeInstance health = blaze.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40.0);
                blaze.setHealth(40.0);
            }
            blaze.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            blaze.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 1, false, false));
        }

        // Day 10: Piglin/Piglin Brute upgrades
        if (entity instanceof Piglin piglin) {
            AttributeInstance health = piglin.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(32.0);
                piglin.setHealth(32.0);
            }
            piglin.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        }

        if (entity instanceof PiglinBrute brute) {
            AttributeInstance health = brute.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(80.0);
                brute.setHealth(80.0);
            }
            AttributeInstance damage = brute.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(20.0);
            }
            brute.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        }
    }

    // ==================== Day 10: Hostile passive mob targeting ====================
    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!isDayActive(10)) return;
        // Make hostile passive mobs target nearby players
        if (event.getEntity() instanceof Animals animal && animal.hasMetadata("pdds_hostile")) {
            if (event.getTarget() == null) {
                // Find nearest player
                int range = plugin.getConfigManager().getDaySetting(10, "targeting-range", 100);
                Player nearest = null;
                double nearestDist = range * range;
                for (Player p : animal.getWorld().getPlayers()) {
                    double dist = p.getLocation().distanceSquared(animal.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = p;
                    }
                }
                if (nearest != null) {
                    animal.setTarget(nearest);
                }
            }
        }
    }

    // ==================== Day 11: Totem failure chance ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemUse(EntityResurrectEvent event) {
        if (!isDayActive(11)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        double failChance = plugin.getConfigManager().getDaySetting(11, "totem-failure-chance", 0.10);
        if (random.nextDouble() < failChance) {
            // Check if player has relic that prevents failure
            if (player.hasMetadata("pdds_totem_protect")) return;

            event.setCancelled(true);
            player.sendMessage(net.kyori.adventure.text.Component.text("§4§lYour Totem of Undying failed!"));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
        }
    }

    // ==================== Day 11: Lightning on bed carriers ====================
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!isDayActive(11)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if player is carrying a bed
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().name().endsWith("_BED")) {
                double chance = plugin.getConfigManager().getDaySetting(11, "bed-lightning-chance", 0.05);
                if (random.nextDouble() < chance) {
                    player.getWorld().strikeLightning(player.getLocation());
                    player.sendMessage(net.kyori.adventure.text.Component.text("§e§lLightning strikes — you carry a bed in these dark times!"));
                }
                break;
            }
        }
    }

    // ==================== Day 11: Wither Skeleton equipment upgrades ====================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onWitherSkeletonSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(11)) return;

        if (event.getEntity() instanceof WitherSkeleton ws) {
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.addEnchant(Enchantment.DAMAGE_ALL, 3, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            sword.setItemMeta(meta);
            ws.getEquipment().setItemInMainHand(sword);
            ws.getEquipment().setItemInMainHandDropChance(0.02f);

            // Armor
            ws.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            ws.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            ws.getEquipment().setHelmetDropChance(0.01f);
            ws.getEquipment().setChestplateDropChance(0.01f);

            AttributeInstance health = ws.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40.0);
                ws.setHealth(40.0);
            }
        }
    }

    // ==================== Day 12: Mobs +30% speed ====================
    @EventHandler(priority = EventPriority.LOW)
    public void onMobSpawnSpeed(CreatureSpawnEvent event) {
        if (!isDayActive(12)) return;
        LivingEntity entity = event.getEntity();

        if (entity instanceof Monster) {
            double bonus = plugin.getConfigManager().getDaySetting(12, "mob-speed-bonus-percent", 30) / 100.0;
            AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * (1.0 + bonus));
            }
        }

        // Day 12: Upgraded zombies
        if (entity instanceof Zombie zombie && !(entity instanceof ZombieVillager)) {
            AttributeInstance health = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40.0);
                zombie.setHealth(40.0);
            }
            AttributeInstance damage = zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(10.0);
            }
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        }

        // Day 12: Upgraded skeletons
        if (entity instanceof Skeleton skeleton) {
            AttributeInstance health = skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40.0);
                skeleton.setHealth(40.0);
            }
            skeleton.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        }
    }

    // ==================== Day 13: Squid → Elder Guardian, Cod → Guardian ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onAquaticSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(13)) return;

        if ((event.getEntity() instanceof Squid || event.getEntity() instanceof GlowSquid)
                && plugin.getConfigManager().getDaySetting(13, "squid-to-elder-guardian", true)) {
            event.setCancelled(true);
            Location loc = event.getLocation();
            ElderGuardian eg = (ElderGuardian) loc.getWorld().spawnEntity(loc, EntityType.ELDER_GUARDIAN);
            eg.setCustomName("§5§lAbyssal Elder Guardian");
            eg.setCustomNameVisible(true);
        }

        if (event.getEntity() instanceof Cod
                && plugin.getConfigManager().getDaySetting(13, "cod-to-guardian", true)) {
            event.setCancelled(true);
            Location loc = event.getLocation();
            loc.getWorld().spawnEntity(loc, EntityType.GUARDIAN);
        }
    }

    // ==================== Day 13: Giant Magma Cubes ====================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onMagmaCubeSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(13)) return;

        if (event.getEntity() instanceof MagmaCube mc) {
            if (plugin.getConfigManager().getDaySetting(13, "giant-magma-cubes", true)) {
                if (random.nextDouble() < 0.15) {
                    mc.setSize(10);
                    mc.setCustomName("§c§lGiant Magma Cube");
                    mc.setCustomNameVisible(true);
                    AttributeInstance health = mc.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (health != null) {
                        health.setBaseValue(120.0);
                        mc.setHealth(120.0);
                    }
                } else {
                    // Stronger regular magma cubes
                    mc.setSize(Math.min(mc.getSize() + 2, 8));
                    mc.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                }
            }
        }
    }

    // ==================== Day 14: Diamond armor triggers lightning ====================
    @EventHandler
    public void onPlayerDamageByEntityDay14(EntityDamageByEntityEvent event) {
        if (!isDayActive(14)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if wearing diamond armor
        boolean hasDiamondArmor = false;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType().name().startsWith("DIAMOND_")) {
                hasDiamondArmor = true;
                break;
            }
        }

        if (hasDiamondArmor) {
            double chance = plugin.getConfigManager().getDaySetting(14, "diamond-armor-lightning-chance", 0.05);
            if (random.nextDouble() < chance) {
                player.getWorld().strikeLightning(player.getLocation());
            }
        }
    }

    // ==================== Day 14: Upgraded spiders/cave spiders ====================
    @EventHandler(priority = EventPriority.LOW)
    public void onSpiderSpawnDay14(CreatureSpawnEvent event) {
        if (!isDayActive(14)) return;

        if (event.getEntity() instanceof Spider spider) {
            AttributeInstance health = spider.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(32.0);
                spider.setHealth(32.0);
            }
            spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            spider.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 1, false, false));
        }

        if (event.getEntity() instanceof CaveSpider cs) {
            AttributeInstance health = cs.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(24.0);
                cs.setHealth(24.0);
            }
            cs.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            cs.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 2, false, false));
        }
    }
}
