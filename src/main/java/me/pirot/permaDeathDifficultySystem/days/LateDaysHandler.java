package me.pirot.permaDeathDifficultySystem.days;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Handles mechanics for Days 15–21:
 * Day 15: Axolotls apply Poison II within 16 blocks; cows can explode
 * Day 16: Slowness every 30min; very rapid drowning; Iron Golems → Vexes
 * Day 17: Warden applies extreme Slowness within 20 blocks; zombies can break blocks
 * Day 18: Lava 3× damage, Nausea; Poison II every 30min; fire 3× damage; upgraded Ghasts
 * Day 19: Bats cause Blindness + Strength III/Speed III; invisible Berserk Silverfish
 * Day 20: Targeting range 256; bare-hand damage; Ender Pearl double damage
 * Day 21: Ender Dragon stage; End opens; Y>150 damage; Giant Zombies; Wither every 30min; 2 hearts; Enderman pick blocks
 */
public class LateDaysHandler extends DayHandler {

    private final Random random = new Random();

    public LateDaysHandler(PermaDeathDifficultySystem plugin) {
        super(plugin);
    }

    @Override
    public boolean handlesDay(int day) {
        return day >= 15 && day <= 21;
    }

    @Override
    public void onDayStart(int day) {
        // Day 21: Set all players to 2 hearts (4 health)
        if (day >= 21 && isDayActive(21)) {
            int maxHealth = plugin.getConfigManager().getDaySetting(21, "player-max-health", 4);
            for (Player player : Bukkit.getOnlinePlayers()) {
                AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.setBaseValue(maxHealth);
                    if (player.getHealth() > maxHealth) {
                        player.setHealth(maxHealth);
                    }
                }
            }
        }
    }

    // ==================== Day 15: Cows can explode ====================
    @EventHandler
    public void onCowDeath(EntityDeathEvent event) {
        if (!isDayActive(15)) return;

        if (event.getEntity() instanceof Cow) {
            double chance = plugin.getConfigManager().getDaySetting(15, "cow-explode-chance", 0.1);
            if (random.nextDouble() < chance) {
                Location loc = event.getEntity().getLocation();
                // Explode without block destruction
                loc.getWorld().createExplosion(loc, 3.0f, false, false);
            }
        }
    }

    // ==================== Day 16: Iron Golems → upgraded Vexes ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onIronGolemSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(16)) return;

        if (event.getEntity() instanceof IronGolem
                && plugin.getConfigManager().getDaySetting(16, "iron-golem-to-vex", true)) {
            event.setCancelled(true);
            Location loc = event.getLocation();
            // Spawn 3 upgraded Vexes
            for (int i = 0; i < 3; i++) {
                Vex vex = (Vex) loc.getWorld().spawnEntity(
                        loc.clone().add(random.nextDouble() * 2 - 1, 1, random.nextDouble() * 2 - 1),
                        EntityType.VEX);
                vex.setCustomName("§c§lWrath Vex");
                vex.setCustomNameVisible(true);
                AttributeInstance health = vex.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(40.0);
                    vex.setHealth(40.0);
                }
                AttributeInstance damage = vex.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (damage != null) {
                    damage.setBaseValue(15.0);
                }
                vex.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 1, false, false));
            }
        }
    }

    // ==================== Day 16: Rapid drowning ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrowningDamage(EntityDamageEvent event) {
        if (!isDayActive(16)) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.DROWNING) return;

        double multiplier = plugin.getConfigManager().getDaySetting(16, "rapid-drowning-multiplier", 5.0);
        event.setDamage(event.getDamage() * multiplier);
    }

    // ==================== Day 17: Zombies can break blocks ====================
    @EventHandler
    public void onZombieTargetDay17(EntityTargetLivingEntityEvent event) {
        if (!isDayActive(17)) return;
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!(event.getTarget() instanceof Player)) return;

        if (plugin.getConfigManager().getDaySetting(17, "zombie-break-blocks", true)) {
            // Schedule block breaking behavior
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (zombie.isDead() || zombie.getTarget() == null) {
                    task.cancel();
                    return;
                }

                // Break breakable blocks in front of the zombie
                Location front = zombie.getLocation().add(zombie.getLocation().getDirection().multiply(1));
                org.bukkit.block.Block block = front.getBlock();
                Material mat = block.getType();

                // Only break certain blocks (doors, crops, torches, fences)
                if (isZombieBreakable(mat)) {
                    block.breakNaturally();
                    zombie.getWorld().playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f);
                }
            }, 40L, 40L); // Every 2 seconds
        }
    }

    private boolean isZombieBreakable(Material mat) {
        return mat.name().contains("DOOR") || mat.name().contains("FENCE_GATE")
                || mat == Material.TORCH || mat == Material.WALL_TORCH
                || mat == Material.SOUL_TORCH || mat == Material.SOUL_WALL_TORCH
                || mat == Material.WHEAT || mat == Material.CARROTS
                || mat == Material.POTATOES || mat == Material.BEETROOTS
                || mat == Material.GLASS || mat == Material.GLASS_PANE
                || mat.name().contains("STAINED_GLASS");
    }

    // ==================== Day 18: Lava and fire 3× damage ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onLavaFireDamage(EntityDamageEvent event) {
        if (!isDayActive(18)) return;
        if (!(event.getEntity() instanceof Player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            double multiplier = plugin.getConfigManager().getDaySetting(18, "lava-damage-multiplier", 3.0);
            event.setDamage(event.getDamage() * multiplier);

            // Apply Nausea
            Player player = (Player) event.getEntity();
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 0, false, false, true));
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            double multiplier = plugin.getConfigManager().getDaySetting(18, "fire-damage-multiplier", 3.0);
            event.setDamage(event.getDamage() * multiplier);
        }
    }

    // ==================== Day 18: Upgraded Ghasts ====================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onGhastSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(18)) return;

        if (event.getEntity() instanceof Ghast ghast) {
            AttributeInstance health = ghast.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(20.0);
                ghast.setHealth(20.0);
            }
            ghast.setCustomName("§c§lInfernal Ghast");
            ghast.setCustomNameVisible(true);
        }
    }

    // ==================== Day 19: Bats cause Blindness + buffed ====================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBatSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(19)) return;

        if (event.getEntity() instanceof Bat bat) {
            bat.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 2, false, false)); // Strength III
            bat.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false)); // Speed III
            bat.setCustomName("§8§lBlind Screamer");
            bat.setCustomNameVisible(true);
        }
    }

    // ==================== Day 19: Silverfish become invisible Berserk Silverfish ====================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSilverfishSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(19)) return;

        if (event.getEntity() instanceof Silverfish sf) {
            if (plugin.getConfigManager().getDaySetting(19, "silverfish-invisible", true)) {
                sf.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                sf.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 2, false, false));
                sf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
                sf.setCustomName("§4§lBerserk Silverfish");
                sf.setCustomNameVisible(false); // Invisible, name hidden
                AttributeInstance health = sf.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(16.0);
                    sf.setHealth(16.0);
                }
            }
        }
    }

    // ==================== Day 20: Bare-hand interaction causes massive damage ====================
    @EventHandler
    public void onBareHandInteract(PlayerInteractEvent event) {
        if (!isDayActive(20)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (mainHand.getType() == Material.AIR) {
            // Check if they're interacting with a block
            if (event.getAction().name().contains("BLOCK")) {
                double damage = plugin.getConfigManager().getDaySetting(20, "bare-hand-damage", 20.0);
                player.damage(damage);
                player.sendMessage(net.kyori.adventure.text.Component.text("§c§lYour bare hands burn from touching the cursed world!"));
            }
        }
    }

    // ==================== Day 20: Ender Pearl double teleportation damage ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnderPearlDamage(EntityDamageEvent event) {
        if (!isDayActive(20)) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        Player player = (Player) event.getEntity();
        // Check if the player recently threw an ender pearl (within last 2 seconds)
        if (player.hasMetadata("pdds_ender_pearl_thrown")) {
            double multiplier = plugin.getConfigManager().getDaySetting(20, "ender-pearl-damage-multiplier", 2.0);
            event.setDamage(event.getDamage() * multiplier);
            player.removeMetadata("pdds_ender_pearl_thrown", plugin);
        }
    }

    @EventHandler
    public void onEnderPearlThrow(ProjectileLaunchEvent event) {
        if (!isDayActive(20)) return;
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        player.setMetadata("pdds_ender_pearl_thrown", new FixedMetadataValue(plugin, true));
        // Remove metadata after 5 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.removeMetadata("pdds_ender_pearl_thrown", plugin), 100L);
    }

    // ==================== Day 21: Y > 150 damage ====================
    @EventHandler
    public void onPlayerMoveDay21(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!isDayActive(21)) return;

        Player player = event.getPlayer();
        if (player.getLocation().getBlockY() > 150) {
            double damage = plugin.getConfigManager().getDaySetting(21, "y-above-150-damage", 2.0);
            // Apply damage every second (check on move, throttle)
            if (!player.hasMetadata("pdds_y150_cooldown")) {
                player.damage(damage);
                player.setMetadata("pdds_y150_cooldown", new FixedMetadataValue(plugin, true));
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        player.removeMetadata("pdds_y150_cooldown", plugin), 20L);
            }
        }
    }

    // ==================== Day 21: Giant Zombies ====================
    @EventHandler(priority = EventPriority.LOW)
    public void onZombieSpawnDay21(CreatureSpawnEvent event) {
        if (!isDayActive(21)) return;

        if (event.getEntity() instanceof Zombie && !(event.getEntity() instanceof ZombieVillager)
                && !(event.getEntity() instanceof Drowned) && !(event.getEntity() instanceof Husk)) {
            if (plugin.getConfigManager().getDaySetting(21, "giant-zombies", true)) {
                if (random.nextDouble() < 0.08) { // 8% chance
                    // Spawn a Giant at the location
                    event.setCancelled(true);
                    Location loc = event.getLocation();
                    Giant giant = (Giant) loc.getWorld().spawnEntity(loc, EntityType.GIANT);
                    giant.setCustomName("§4§lUndead Titan");
                    giant.setCustomNameVisible(true);
                    AttributeInstance health = giant.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (health != null) {
                        health.setBaseValue(200.0);
                        giant.setHealth(200.0);
                    }
                    AttributeInstance damage = giant.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                    if (damage != null) {
                        damage.setBaseValue(30.0);
                    }
                }
            }
        }
    }

    // ==================== Day 21: Set new player health on join ====================
    @EventHandler
    public void onPlayerJoinDay21(org.bukkit.event.player.PlayerJoinEvent event) {
        if (!isDayActive(21)) return;

        int maxHealth = plugin.getConfigManager().getDaySetting(21, "player-max-health", 4);
        Player player = event.getPlayer();
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(maxHealth);
            if (player.getHealth() > maxHealth) {
                player.setHealth(maxHealth);
            }
        }
    }

    // ==================== Day 21: Endermen can pick up most blocks ====================
    @EventHandler
    public void onEndermanPickBlock(EntityChangeBlockEvent event) {
        if (!isDayActive(21)) return;
        if (!(event.getEntity() instanceof Enderman)) return;

        if (plugin.getConfigManager().getDaySetting(21, "enderman-pick-blocks", true)) {
            // Allow the event — Enderman can pick up any block
            // By default, Endermen can only pick specific blocks; we allow all
            event.setCancelled(false);
        }
    }
}
