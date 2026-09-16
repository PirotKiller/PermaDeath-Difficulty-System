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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * Handles mechanics for Days 22–35:
 * Day 22: Shulker projectiles explode; Shulkers explode on death + Dragon's Breath; super Endermen
 * Day 23: Ender Quantum Creepers; Ender Ghasts with Dragon's Breath
 * Day 24: Cows → Ravagers; Ravagers charge/destroy + Bad Omen V
 * Day 25: Wither Boss periodically spawns
 * Day 26: Spiders move fast through water
 * Day 27: Skeletons Power X bows; arrows trigger lightning
 * Day 28: Endermites of Death (~1000 HP, 500 damage)
 * Day 29: Spawners increase speed when mined
 * Day 30: Lightning on Elytra users
 * Day 31: Bedrock/Obsidian apply Wither III on touch
 * Day 32: Ender Quantum Creepers destroy Obsidian
 * Day 33: Phantoms explode when killed
 * Day 34: Acid Rain (Poison II)
 * Day 35: The Core final boss
 */
public class EndgameDaysHandler extends DayHandler {

    private final Random random = new Random();
    private int witherSpawnTaskId = -1;

    public EndgameDaysHandler(PermaDeathDifficultySystem plugin) {
        super(plugin);
    }

    @Override
    public boolean handlesDay(int day) {
        return day >= 22 && day <= 35;
    }

    @Override
    public void onDayStart(int day) {
        // Day 25: Start periodic Wither Boss spawning
        if (day >= 25 && isDayActive(25)) {
            startWitherSpawning();
        }

        // Day 35: Trigger The Core boss
        if (day == 35 && isDayActive(35)) {
            plugin.getLogger().info("Day 35 reached — The Core boss should be activated!");
            // Boss will be spawned via the TheCoreBoss manager
            if (plugin.getTheCoreBoss() != null) {
                plugin.getTheCoreBoss().startEncounter();
            }
        }
    }

    private void startWitherSpawning() {
        if (witherSpawnTaskId != -1) return;

        int intervalSeconds = plugin.getConfigManager().getDaySetting(25, "wither-boss-spawn-interval-seconds", 600);
        long intervalTicks = intervalSeconds * 20L;

        witherSpawnTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isDayActive(25)) return;

            // Pick a random online player
            Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
            if (players.length == 0) return;

            Player target = players[random.nextInt(players.length)];
            Location loc = target.getLocation().add(
                    random.nextInt(20) - 10,
                    10,
                    random.nextInt(20) - 10
            );

            // Spawn Wither
            Wither wither = (Wither) loc.getWorld().spawnEntity(loc, EntityType.WITHER);
            wither.setCustomName("§5§lHarbinger of Doom");
            wither.setCustomNameVisible(true);

            // Broadcast warning
            Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                    "§4§l☠ A Wither has spawned near " + target.getName() + "! ☠"));

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            }
        }, intervalTicks, intervalTicks).getTaskId();
    }

    // ==================== Day 22: Shulker projectiles explode ====================
    @EventHandler
    public void onShulkerBulletHit(ProjectileHitEvent event) {
        if (!isDayActive(22)) return;
        if (!(event.getEntity() instanceof ShulkerBullet bullet)) return;

        if (plugin.getConfigManager().getDaySetting(22, "shulker-projectile-explode", true)) {
            Location loc = bullet.getLocation();
            // Explode without block damage
            loc.getWorld().createExplosion(loc, 2.5f, false, false);

            // Apply extreme Levitation to nearby players
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
                if (entity instanceof Player player) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 200, 9, false, false, true));
                }
            }
        }
    }

    // ==================== Day 22: Shulkers explode on death ====================
    @EventHandler
    public void onShulkerDeath(EntityDeathEvent event) {
        if (!isDayActive(22)) return;

        if (event.getEntity() instanceof Shulker) {
            if (plugin.getConfigManager().getDaySetting(22, "shulker-death-explode", true)) {
                Location loc = event.getEntity().getLocation();
                // Explode without block destruction
                loc.getWorld().createExplosion(loc, 3.0f, false, false);

                // Leave Dragon's Breath (area effect cloud)
                AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
                cloud.setRadius(4.0f);
                cloud.setDuration(200); // 10 seconds
                cloud.setRadiusPerTick(-0.005f);
                cloud.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 40, 1), true);
                cloud.setParticle(Particle.DRAGON_BREATH);
            }
        }
    }

    // ==================== Day 22: Extremely strong Endermen ====================
    @EventHandler(priority = EventPriority.LOW)
    public void onEndermanSpawnDay22(CreatureSpawnEvent event) {
        if (!isDayActive(22)) return;

        if (event.getEntity() instanceof Enderman enderman) {
            AttributeInstance health = enderman.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(80.0);
                enderman.setHealth(80.0);
            }
            AttributeInstance damage = enderman.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(20.0);
            }
            enderman.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            enderman.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        }
    }

    // ==================== Day 23: Ender Ghasts ====================
    @EventHandler(priority = EventPriority.LOW)
    public void onGhastSpawnDay23(CreatureSpawnEvent event) {
        if (!isDayActive(23)) return;

        if (event.getEntity() instanceof Ghast ghast) {
            if (plugin.getConfigManager().getDaySetting(23, "ender-ghasts", true)) {
                ghast.setCustomName("§5§lEnder Ghast");
                ghast.setCustomNameVisible(true);
                ghast.setMetadata("pdds_ender_ghast", new FixedMetadataValue(plugin, true));

                AttributeInstance health = ghast.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(40.0);
                    ghast.setHealth(40.0);
                }
            }
        }
    }

    // ==================== Day 23: Ender Ghast projectiles leave Dragon's Breath ====================
    @EventHandler
    public void onGhastFireballHit(ProjectileHitEvent event) {
        if (!isDayActive(23)) return;
        if (!(event.getEntity() instanceof Fireball fireball)) return;
        if (!(fireball.getShooter() instanceof Ghast ghast)) return;
        if (!ghast.hasMetadata("pdds_ender_ghast")) return;

        Location loc = fireball.getLocation();
        // Leave Dragon's Breath
        AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setRadius(5.0f);
        cloud.setDuration(300);
        cloud.setRadiusPerTick(-0.005f);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 40, 2), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1), true);
        cloud.setParticle(Particle.DRAGON_BREATH);
    }

    // ==================== Day 24: Cows replaced by Ravagers ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCowSpawnDay24(CreatureSpawnEvent event) {
        if (!isDayActive(24)) return;

        if (event.getEntity() instanceof Cow && !(event.getEntity() instanceof MushroomCow)) {
            if (plugin.getConfigManager().getDaySetting(24, "cow-to-ravager", true)) {
                event.setCancelled(true);
                Location loc = event.getLocation();
                Ravager ravager = (Ravager) loc.getWorld().spawnEntity(loc, EntityType.RAVAGER);
                ravager.setCustomName("§4§lDoomhoof Ravager");
                ravager.setCustomNameVisible(true);
                ravager.setMetadata("pdds_charge_ravager", new FixedMetadataValue(plugin, true));

                AttributeInstance health = ravager.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(300.0);
                    ravager.setHealth(300.0);
                }
                AttributeInstance damage = ravager.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (damage != null) {
                    damage.setBaseValue(30.0);
                }
                AttributeInstance speed = ravager.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(0.5);
                }
            }
        }
    }

    // ==================== Day 24: Ravager charge attack - destroy 5x5 area + Bad Omen V ====================
    @EventHandler
    public void onRavagerAttack(EntityDamageByEntityEvent event) {
        if (!isDayActive(24)) return;
        if (!(event.getDamager() instanceof Ravager ravager)) return;
        if (!ravager.hasMetadata("pdds_charge_ravager")) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Apply Bad Omen V
        player.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, 6000, 4, false, true, true));

        // Destroy 5×5 area around impact (no drops)
        int radius = plugin.getConfigManager().getDaySetting(24, "ravager-charge-radius", 5) / 2;
        Location center = event.getEntity().getLocation();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.block.Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (block.getType() != Material.BEDROCK && block.getType() != Material.END_PORTAL_FRAME
                            && block.getType() != Material.END_PORTAL) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
    }

    // ==================== Day 26: Spiders fast in water ====================
    @EventHandler
    public void onSpiderMoveInWater(EntityDamageEvent event) {
        // We use a periodic task instead — see below
    }

    // Day 26 is handled by a repeating check in AuraEffectListener for spider water speed

    // ==================== Day 27: Skeleton Power X bows + arrow lightning ====================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSkeletonSpawnDay27(CreatureSpawnEvent event) {
        if (!isDayActive(27)) return;

        if (event.getEntity() instanceof Skeleton skeleton) {
            int power = plugin.getConfigManager().getDaySetting(27, "skeleton-bow-power", 10);
            ItemStack bow = new ItemStack(Material.BOW);
            ItemMeta meta = bow.getItemMeta();
            meta.addEnchant(Enchantment.ARROW_DAMAGE, power, true);
            meta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
            bow.setItemMeta(meta);
            skeleton.getEquipment().setItemInMainHand(bow);
            skeleton.getEquipment().setItemInMainHandDropChance(0.02f);

            skeleton.setMetadata("pdds_lightning_arrows", new FixedMetadataValue(plugin, true));
        }
    }

    // ==================== Day 27: Arrow lightning ====================
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!isDayActive(27)) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        if (plugin.getConfigManager().getDaySetting(27, "arrow-lightning", true)) {
            if (arrow.getShooter() instanceof Skeleton sk && sk.hasMetadata("pdds_lightning_arrows")) {
                Location loc = arrow.getLocation();
                loc.getWorld().strikeLightning(loc);
            }
        }
    }

    // ==================== Day 28: Endermites of Death ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onEndermiteSpawn(CreatureSpawnEvent event) {
        if (!isDayActive(28)) return;

        if (event.getEntity() instanceof Endermite em) {
            double hp = plugin.getConfigManager().getDaySetting(28, "endermite-of-death-hp", 1000.0);
            double dmg = plugin.getConfigManager().getDaySetting(28, "endermite-of-death-damage", 500.0);

            em.setCustomName("§4§l☠ Endermite of Death ☠");
            em.setCustomNameVisible(true);

            AttributeInstance health = em.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(hp);
                em.setHealth(hp);
            }
            AttributeInstance damage = em.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(dmg);
            }

            em.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 2, false, false));
            em.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));
        }
    }

    // ==================== Day 32: Ender Quantum Creepers eat obsidian and water ====================

    /**
     * Vanilla blast resistance makes obsidian immune to creeper explosions and water absorbs
     * the blast entirely, so the Day 32 mechanic has to clear those blocks manually after the
     * detonation resolves.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuantumCreeperExplode(EntityExplodeEvent event) {
        if (!isDayActive(32)) return;
        if (!plugin.getConfigManager().getDaySetting(32, "ender-quantum-destroy-obsidian", true)) return;
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!creeper.hasMetadata("pdds_ender_quantum_creeper")) return;

        int radius = plugin.getConfigManager().getDaySetting(32, "obsidian-destroy-radius", 4);
        Location center = event.getLocation();
        World world = center.getWorld();
        if (world == null) return;

        long radiusSq = (long) radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((long) dx * dx + (long) dy * dy + (long) dz * dz > radiusSq) continue;

                    org.bukkit.block.Block block = world.getBlockAt(
                            center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                    if (!isQuantumDestructible(block)) continue;

                    block.setType(Material.AIR);
                    plugin.getBlockIndex().onBlockChanged(block, Material.AIR);
                }
            }
        }
        world.spawnParticle(Particle.DRAGON_BREATH, center, 80, radius / 2.0, radius / 2.0, radius / 2.0, 0.05);
        world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.4f);
    }

    private boolean isQuantumDestructible(org.bukkit.block.Block block) {
        Material type = block.getType();
        if (type == Material.OBSIDIAN || type == Material.CRYING_OBSIDIAN) return true;
        if (type == Material.WATER) return true;
        // Waterlogged blocks count as "water-containing" per the spec.
        return block.getBlockData() instanceof org.bukkit.block.data.Waterlogged waterlogged
                && waterlogged.isWaterlogged();
    }

    // ==================== Day 30: Lightning on Elytra users ====================
    @EventHandler
    public void onElytraFlight(PlayerMoveEvent event) {
        if (!isDayActive(30)) return;

        Player player = event.getPlayer();
        if (!player.isGliding()) return;

        double chance = plugin.getConfigManager().getDaySetting(30, "elytra-lightning-chance", 0.05);
        // Per-tick is too frequent, throttle to every 2 seconds
        if (!player.hasMetadata("pdds_elytra_check")) {
            player.setMetadata("pdds_elytra_check", new FixedMetadataValue(plugin, true));
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    player.removeMetadata("pdds_elytra_check", plugin), 40L);

            if (random.nextDouble() < chance) {
                player.getWorld().strikeLightning(player.getLocation());
                player.sendMessage(net.kyori.adventure.text.Component.text("§e§lLightning targets your Elytra!"));
            }
        }
    }

    // ==================== Day 31: Bedrock/Obsidian apply Wither III ====================
    @EventHandler
    public void onPlayerMoveDay31(PlayerMoveEvent event) {
        if (!isDayActive(31)) return;

        Player player = event.getPlayer();
        Location loc = player.getLocation();

        // Check blocks below and around the player
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Material mat = loc.getWorld().getBlockAt(
                            loc.getBlockX() + dx, loc.getBlockY() + dy, loc.getBlockZ() + dz).getType();
                    if (mat == Material.BEDROCK || mat == Material.OBSIDIAN) {
                        int level = plugin.getConfigManager().getDaySetting(31, "bedrock-obsidian-wither-level", 3);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, level - 1, false, false, true));
                        return; // Only apply once per move
                    }
                }
            }
        }
    }

    // ==================== Day 33: Phantoms explode when killed ====================
    @EventHandler
    public void onPhantomDeath(EntityDeathEvent event) {
        if (!isDayActive(33)) return;

        if (event.getEntity() instanceof Phantom) {
            if (plugin.getConfigManager().getDaySetting(33, "phantom-death-explode", true)) {
                Location loc = event.getEntity().getLocation();
                loc.getWorld().createExplosion(loc, 4.0f, true, false);
            }
        }
    }

    // ==================== Day 34: Acid Rain ====================
    @EventHandler
    public void onPlayerMoveDay34(PlayerMoveEvent event) {
        if (!isDayActive(34)) return;
        if (!plugin.getConfigManager().getDaySetting(34, "acid-rain", true)) return;

        Player player = event.getPlayer();
        World world = player.getWorld();

        if (world.hasStorm() && player.getLocation().getBlockY() >= world.getHighestBlockYAt(player.getLocation()) - 1) {
            // Throttle to every 2 seconds
            if (!player.hasMetadata("pdds_acid_rain_cd")) {
                player.setMetadata("pdds_acid_rain_cd", new FixedMetadataValue(plugin, true));
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        player.removeMetadata("pdds_acid_rain_cd", plugin), 40L);

                int poisonLevel = plugin.getConfigManager().getDaySetting(34, "acid-rain-poison-level", 2);
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, poisonLevel - 1, false, false, true));
                player.damage(1.0); // Small direct damage too
            }
        }
    }
}
