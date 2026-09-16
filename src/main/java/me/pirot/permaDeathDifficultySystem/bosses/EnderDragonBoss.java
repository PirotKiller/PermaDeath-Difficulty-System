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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Random;

/**
 * Day 21 Ender Dragon boss encounter:
 * - Dragon's Breath flood mechanic with warnings
 * - End Crystal enemy spawns
 * - High health/damage Dragon
 * - End portal opening
 */
public class EnderDragonBoss implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final BossBarManager bossBarManager;
    private final Random random = new Random();

    private boolean encounterActive = false;
    private BukkitTask breathFloodTask;
    private BukkitTask enemySpawnTask;

    private static final String BOSS_BAR_ID = "ender_dragon";

    public EnderDragonBoss(PermaDeathDifficultySystem plugin, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
    }

    /**
     * Starts the Ender Dragon encounter for Day 21.
     */
    public void startEncounter() {
        if (encounterActive) return;
        encounterActive = true;

        // Broadcast warning
        for (Player player : Bukkit.getOnlinePlayers()) {
            Title.Times times = Title.Times.times(
                    Duration.ofMillis(1000), Duration.ofSeconds(5), Duration.ofMillis(2000));
            player.showTitle(Title.title(
                    Component.text("THE DRAGON AWAKENS", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD),
                    Component.text("Day 21 — The End Opens", NamedTextColor.GRAY, TextDecoration.ITALIC),
                    times));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        }

        // Create boss bar
        bossBarManager.createBossBar(BOSS_BAR_ID,
                "§5§lEnder Dragon — Day 21 Encounter",
                BarColor.PURPLE, BarStyle.SEGMENTED_10, BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY);
        bossBarManager.addAllPlayers(BOSS_BAR_ID);

        // Spawn the dragon after a delay
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnDragon, 100L);

        // Start Dragon's Breath flood mechanic
        breathFloodTask = Bukkit.getScheduler().runTaskTimer(plugin, this::dragonBreathFlood, 600L, 600L);

        // Start enemy spawning from End Crystals
        enemySpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnCrystalEnemies, 400L, 400L);

        plugin.getLogger().info("Ender Dragon encounter started!");
    }

    private void spawnDragon() {
        // Spawn in the overworld at a random online player's location
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        if (players.length == 0) return;

        Player target = players[random.nextInt(players.length)];
        Location loc = target.getLocation().add(0, 50, 0);

        EnderDragon dragon = (EnderDragon) loc.getWorld().spawnEntity(loc, EntityType.ENDER_DRAGON);
        dragon.setCustomName("§5§lThe Awakened Dragon");
        dragon.setCustomNameVisible(true);

        // Enhanced stats
        AttributeInstance health = dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(500.0);
            dragon.setHealth(500.0);
        }

        Bukkit.broadcast(Component.text("§5§l☠ The Ender Dragon has manifested in the Overworld! ☠",
                NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
    }

    /**
     * Dragon's Breath flood: Warns players, then creates a large area of Dragon's Breath.
     */
    private void dragonBreathFlood() {
        if (!encounterActive) return;

        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        if (players.length == 0) return;

        Player target = players[random.nextInt(players.length)];

        // Warning phase
        target.sendMessage(Component.text("§5§l⚠ Dragon's Breath is flooding your area! MOVE! ⚠"));
        target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.0f);

        Location center = target.getLocation();

        // Spawn warning particles
        for (int i = 0; i < 50; i++) {
            Location particleLoc = center.clone().add(
                    random.nextDouble() * 16 - 8, 0, random.nextDouble() * 16 - 8);
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, particleLoc, 10, 0.5, 0.5, 0.5);
        }

        // After 3 seconds, spawn the actual breath
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < 8; i++) {
                Location cloudLoc = center.clone().add(
                        random.nextDouble() * 12 - 6, 0, random.nextDouble() * 12 - 6);
                AreaEffectCloud cloud = (AreaEffectCloud) center.getWorld().spawnEntity(cloudLoc, EntityType.AREA_EFFECT_CLOUD);
                cloud.setRadius(3.0f);
                cloud.setDuration(200);
                cloud.setRadiusPerTick(-0.005f);
                cloud.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 40, 1), true);
                cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 1), true);
                cloud.setParticle(Particle.DRAGON_BREATH);
            }
        }, 60L);
    }

    /**
     * Spawns enemies from "End Crystal" locations.
     */
    private void spawnCrystalEnemies() {
        if (!encounterActive) return;

        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        if (players.length == 0) return;

        Player target = players[random.nextInt(players.length)];
        Location loc = target.getLocation().add(
                random.nextInt(30) - 15, 5, random.nextInt(30) - 15);

        // Spawn End Crystal
        EnderCrystal crystal = (EnderCrystal) loc.getWorld().spawnEntity(loc, EntityType.ENDER_CRYSTAL);

        // Spawn enemies around it
        for (int i = 0; i < 3; i++) {
            Location spawnLoc = loc.clone().add(random.nextDouble() * 4 - 2, 0, random.nextDouble() * 4 - 2);
            spawnLoc.setY(loc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

            Enderman enderman = (Enderman) loc.getWorld().spawnEntity(spawnLoc, EntityType.ENDERMAN);
            enderman.setCustomName("§5Dragon's Guard");
            enderman.setCustomNameVisible(true);
            AttributeInstance health = enderman.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(60.0);
                enderman.setHealth(60.0);
            }
        }
    }

    /**
     * Handles Dragon death to end the encounter.
     */
    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (!encounterActive) return;
        if (!(event.getEntity() instanceof EnderDragon)) return;

        encounterActive = false;

        // Stop tasks
        if (breathFloodTask != null) breathFloodTask.cancel();
        if (enemySpawnTask != null) enemySpawnTask.cancel();

        // Remove boss bar
        bossBarManager.removeBossBar(BOSS_BAR_ID);

        // Announce victory
        Bukkit.broadcast(Component.text("§a§l✦ The Ender Dragon has been slain! The End is now open! ✦",
                NamedTextColor.GREEN, TextDecoration.BOLD));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(
                    Component.text("VICTORY", NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.text("The Dragon falls...", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(1000))));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    /**
     * Tears the encounter down on plugin disable so no tasks survive a reload.
     */
    public void shutdown() {
        if (breathFloodTask != null) breathFloodTask.cancel();
        if (enemySpawnTask != null) enemySpawnTask.cancel();
        bossBarManager.removeBossBar(BOSS_BAR_ID);
        encounterActive = false;
    }

    public boolean isEncounterActive() {
        return encounterActive;
    }
}
