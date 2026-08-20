package me.pirot.permaDeathDifficultySystem.bosses;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Random;

/**
 * Day 35 — The Core: Final boss encounter.
 * A command-block-based boss/object inspired by a Wither Storm.
 * Implemented as an armor-stand-based entity that players mine (hit) to damage.
 * Defends itself with projectiles, mob summoning, and environmental destruction.
 *
 * Phases:
 * Phase 1 (100-75% HP): Projectile attacks + occasional mob spawns
 * Phase 2 (75-50% HP): Environmental destruction + faster attacks
 * Phase 3 (50-25% HP): Mass mob summoning + area denial
 * Phase 4 (25-0% HP): All mechanics intensified + desperation attacks
 */
public class TheCoreBoss implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final BossBarManager bossBarManager;
    private final Random random = new Random();

    private boolean encounterActive = false;
    private ArmorStand coreEntity;
    private double coreHealth;
    private double coreMaxHealth;
    private int currentPhase = 1;
    private Location coreLocation;

    private BukkitTask attackTask;
    private BukkitTask mobSpawnTask;
    private BukkitTask environmentTask;
    private BukkitTask particleTask;

    private static final String BOSS_BAR_ID = "the_core";
    private static final double MAX_HEALTH = 5000.0;

    public TheCoreBoss(PermaDeathDifficultySystem plugin, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
        this.coreMaxHealth = MAX_HEALTH;
        this.coreHealth = MAX_HEALTH;
    }

    /**
     * Starts The Core encounter.
     */
    public void startEncounter() {
        if (encounterActive) return;
        encounterActive = true;
        coreHealth = coreMaxHealth;
        currentPhase = 1;

        // Warning sequence
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(
                    Component.text("THE CORE AWAKENS", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                    Component.text("Day 35 — Final Judgment", NamedTextColor.RED, TextDecoration.ITALIC),
                    Title.Times.times(Duration.ofSeconds(2), Duration.ofSeconds(5), Duration.ofSeconds(2))));
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        }

        // Spawn The Core after a dramatic delay
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnCore, 140L);

        plugin.getLogger().info("The Core encounter started!");
    }

    private void spawnCore() {
        // Find the center player
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        if (players.length == 0) {
            encounterActive = false;
            return;
        }

        Player target = players[random.nextInt(players.length)];
        coreLocation = target.getLocation().add(0, 15, 0);

        // Create the Core entity (armor stand with command block head)
        coreEntity = (ArmorStand) coreLocation.getWorld().spawnEntity(coreLocation, EntityType.ARMOR_STAND);
        coreEntity.setCustomName("§4§l☠ THE CORE ☠");
        coreEntity.setCustomNameVisible(true);
        coreEntity.setGravity(false);
        coreEntity.setInvulnerable(false);
        coreEntity.setVisible(true);
        coreEntity.setGlowing(true);
        coreEntity.setSmall(false);
        coreEntity.setMetadata("pdds_the_core", new FixedMetadataValue(plugin, true));

        // Give it a command block "head" for visual
        ItemStack head = new ItemStack(Material.COMMAND_BLOCK);
        coreEntity.getEquipment().setHelmet(head);
        coreEntity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));

        // Create surrounding structure (floating command blocks)
        createCoreStructure();

        // Boss bar
        bossBarManager.createBossBar(BOSS_BAR_ID,
                "§4§l☠ THE CORE — Phase 1 ☠",
                BarColor.RED, BarStyle.SEGMENTED_20, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY);
        bossBarManager.addAllPlayers(BOSS_BAR_ID);
        bossBarManager.updateProgress(BOSS_BAR_ID, 1.0);

        // Start attack patterns
        attackTask = Bukkit.getScheduler().runTaskTimer(plugin, this::performAttack, 100L, 60L);
        mobSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnDefenders, 200L, 200L);
        environmentTask = Bukkit.getScheduler().runTaskTimer(plugin, this::environmentalAttack, 300L, 300L);
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnParticles, 5L, 5L);

        Bukkit.broadcast(Component.text("§4§l☠ THE CORE has materialized! Hit it to destroy it! ☠"));
    }

    private void createCoreStructure() {
        if (coreLocation == null) return;
        World world = coreLocation.getWorld();

        // Create a ring of command blocks around the core
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 / 8) * i;
            int x = (int) (Math.cos(angle) * 3);
            int z = (int) (Math.sin(angle) * 3);
            Location blockLoc = coreLocation.clone().add(x, 0, z);
            if (blockLoc.getBlock().getType() == Material.AIR) {
                blockLoc.getBlock().setType(Material.COMMAND_BLOCK);
            }
        }
        // Core block
        coreLocation.getBlock().setType(Material.COMMAND_BLOCK);
    }

    /**
     * Handles damage to The Core (players hitting the armor stand).
     */
    @EventHandler
    public void onCoreDamaged(EntityDamageByEntityEvent event) {
        if (!encounterActive) return;
        if (!(event.getEntity() instanceof ArmorStand as)) return;
        if (!as.hasMetadata("pdds_the_core")) return;

        event.setCancelled(true); // Prevent actual armor stand damage

        double damage = event.getFinalDamage();
        if (event.getDamager() instanceof Player player) {
            // Apply damage to core health
            coreHealth -= damage;
            if (coreHealth <= 0) coreHealth = 0;

            // Update boss bar
            double progress = coreHealth / coreMaxHealth;
            bossBarManager.updateProgress(BOSS_BAR_ID, progress);

            // Update phase
            updatePhase();

            // Feedback
            player.sendActionBar(Component.text(
                    "§4Core HP: " + String.format("%.0f", coreHealth) + " / " + String.format("%.0f", coreMaxHealth)));

            // Visual feedback
            Location loc = as.getLocation();
            loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 20, 0.5, 0.5, 0.5,
                    Material.COMMAND_BLOCK.createBlockData());
            loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_HIT, 1.0f, 1.0f);

            // Core retaliates
            if (random.nextDouble() < 0.3 * currentPhase) {
                retaliateAgainst(player);
            }

            // Check death
            if (coreHealth <= 0) {
                defeatCore();
            }
        }
    }

    private void updatePhase() {
        double healthPercent = coreHealth / coreMaxHealth;
        int newPhase;
        if (healthPercent > 0.75) newPhase = 1;
        else if (healthPercent > 0.50) newPhase = 2;
        else if (healthPercent > 0.25) newPhase = 3;
        else newPhase = 4;

        if (newPhase != currentPhase) {
            currentPhase = newPhase;
            bossBarManager.updateTitle(BOSS_BAR_ID,
                    "§4§l☠ THE CORE — Phase " + currentPhase + " ☠");

            Bukkit.broadcast(Component.text("§4§lThe Core enters Phase " + currentPhase + "!",
                    NamedTextColor.DARK_RED, TextDecoration.BOLD));

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f + (currentPhase * 0.2f));
            }
        }
    }

    /**
     * The Core retaliates against a player who hit it.
     */
    private void retaliateAgainst(Player player) {
        if (coreEntity == null || coreEntity.isDead()) return;

        Location coreLoc = coreEntity.getLocation();
        Location playerLoc = player.getLocation();

        switch (random.nextInt(4)) {
            case 0 -> {
                // Fireball
                Vector direction = playerLoc.toVector().subtract(coreLoc.toVector()).normalize();
                Fireball fireball = coreLoc.getWorld().spawn(
                        coreLoc.clone().add(0, 1, 0), Fireball.class);
                fireball.setDirection(direction.multiply(1.5));
                fireball.setYield(2.0f);
                fireball.setIsIncendiary(false);
            }
            case 1 -> {
                // Lightning
                player.getWorld().strikeLightning(playerLoc);
            }
            case 2 -> {
                // Knockback
                Vector knockback = playerLoc.toVector().subtract(coreLoc.toVector()).normalize().multiply(3).setY(1);
                player.setVelocity(knockback);
                player.damage(5.0);
            }
            case 3 -> {
                // Debuffs
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
            }
        }
    }

    /**
     * Periodic attack pattern based on current phase.
     */
    private void performAttack() {
        if (!encounterActive || coreEntity == null || coreEntity.isDead()) return;

        Location coreLoc = coreEntity.getLocation();

        // Number of attacks scales with phase
        int attackCount = currentPhase;
        for (int i = 0; i < attackCount; i++) {
            Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
            if (players.length == 0) return;
            Player target = players[random.nextInt(players.length)];

            // Wither skulls
            WitherSkull skull = coreLoc.getWorld().spawn(
                    coreLoc.clone().add(0, 2, 0), WitherSkull.class);
            Vector direction = target.getLocation().toVector()
                    .subtract(coreLoc.toVector()).normalize().multiply(1.0);
            skull.setDirection(direction);
        }
    }

    /**
     * Spawns defender mobs around The Core.
     */
    private void spawnDefenders() {
        if (!encounterActive || coreEntity == null || coreEntity.isDead()) return;

        Location coreLoc = coreEntity.getLocation();
        int mobCount = 2 + currentPhase;

        for (int i = 0; i < mobCount; i++) {
            Location spawnLoc = coreLoc.clone().add(
                    random.nextInt(10) - 5, 0, random.nextInt(10) - 5);
            spawnLoc.setY(coreLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

            EntityType type;
            switch (random.nextInt(4)) {
                case 0 -> type = EntityType.WITHER_SKELETON;
                case 1 -> type = EntityType.BLAZE;
                case 2 -> type = EntityType.VEX;
                default -> type = EntityType.ZOMBIE;
            }

            LivingEntity mob = (LivingEntity) coreLoc.getWorld().spawnEntity(spawnLoc, type);
            mob.setCustomName("§4Core Defender");
            mob.setCustomNameVisible(true);
            mob.setMetadata("pdds_core_defender", new FixedMetadataValue(plugin, true));

            AttributeInstance health = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                double hp = 40.0 * currentPhase;
                health.setBaseValue(hp);
                mob.setHealth(hp);
            }
        }
    }

    /**
     * Environmental destruction around The Core.
     */
    private void environmentalAttack() {
        if (!encounterActive || coreEntity == null || coreEntity.isDead()) return;
        if (currentPhase < 2) return;

        Location coreLoc = coreEntity.getLocation();
        int radius = 5 * currentPhase;

        // Destroy blocks in a random area
        for (int i = 0; i < 10 * currentPhase; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int y = random.nextInt(6) - 3;
            int z = random.nextInt(radius * 2) - radius;

            Location blockLoc = coreLoc.clone().add(x, y, z);
            Material mat = blockLoc.getBlock().getType();
            if (mat != Material.AIR && mat != Material.BEDROCK
                    && mat != Material.COMMAND_BLOCK && mat != Material.END_PORTAL_FRAME) {
                blockLoc.getBlock().setType(Material.AIR);
                blockLoc.getWorld().spawnParticle(Particle.BLOCK_CRACK, blockLoc, 5, mat.createBlockData());
            }
        }

        coreLoc.getWorld().playSound(coreLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);

        // Area damage
        for (Entity entity : coreLoc.getWorld().getNearbyEntities(coreLoc, radius, radius, radius)) {
            if (entity instanceof Player player) {
                player.damage(2.0 * currentPhase);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, currentPhase - 1, false, false));
            }
        }
    }

    /**
     * Spawns ominous particles around The Core.
     */
    private void spawnParticles() {
        if (!encounterActive || coreEntity == null || coreEntity.isDead()) return;

        Location loc = coreEntity.getLocation();
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 5, 1.5, 1.5, 1.5, 0.05);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 3, 2, 2, 2, 0.02);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 2, 3, 3, 3, 0.01);
    }

    /**
     * The Core is defeated.
     */
    private void defeatCore() {
        encounterActive = false;

        // Stop all tasks
        if (attackTask != null) attackTask.cancel();
        if (mobSpawnTask != null) mobSpawnTask.cancel();
        if (environmentTask != null) environmentTask.cancel();
        if (particleTask != null) particleTask.cancel();

        // Remove boss bar
        bossBarManager.removeBossBar(BOSS_BAR_ID);

        // Dramatic explosion sequence
        if (coreEntity != null && !coreEntity.isDead()) {
            Location coreLoc = coreEntity.getLocation();

            // Multiple explosions
            for (int i = 0; i < 10; i++) {
                int delay = i * 10;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Location explosionLoc = coreLoc.clone().add(
                            random.nextInt(10) - 5, random.nextInt(6) - 3, random.nextInt(10) - 5);
                    coreLoc.getWorld().createExplosion(explosionLoc, 3.0f, false, false);
                }, delay);
            }

            // Remove the core entity
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (coreEntity != null && !coreEntity.isDead()) {
                    coreEntity.remove();
                }
                // Clean up command blocks
                for (int x = -5; x <= 5; x++) {
                    for (int y = -3; y <= 3; y++) {
                        for (int z = -5; z <= 5; z++) {
                            Location blockLoc = coreLoc.clone().add(x, y, z);
                            if (blockLoc.getBlock().getType() == Material.COMMAND_BLOCK) {
                                blockLoc.getBlock().setType(Material.AIR);
                            }
                        }
                    }
                }

                // Drop rewards
                coreLoc.getWorld().dropItemNaturally(coreLoc, new ItemStack(Material.NETHER_STAR, 16));
                coreLoc.getWorld().dropItemNaturally(coreLoc, new ItemStack(Material.NETHERITE_BLOCK, 8));
                coreLoc.getWorld().dropItemNaturally(coreLoc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 64));
            }, 120L);

            // Kill all Core defenders
            for (Entity entity : coreLoc.getWorld().getEntities()) {
                if (entity.hasMetadata("pdds_core_defender")) {
                    entity.remove();
                }
            }
        }

        // Victory announcement
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(
                    Component.text("THE CORE IS DESTROYED", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("You have survived 35 days!", NamedTextColor.GREEN, TextDecoration.ITALIC),
                    Title.Times.times(Duration.ofSeconds(2), Duration.ofSeconds(8), Duration.ofSeconds(3))));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
        }

        Bukkit.broadcast(Component.text(
                "§6§l✦ THE CORE HAS BEEN DESTROYED! THE PERMADEATH CHALLENGE IS COMPLETE! ✦",
                NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    public boolean isEncounterActive() {
        return encounterActive;
    }
}
