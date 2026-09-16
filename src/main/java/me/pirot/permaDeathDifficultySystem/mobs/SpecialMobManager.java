package me.pirot.permaDeathDifficultySystem.mobs;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

/**
 * Behaviours that vanilla AI can't express, driven from a single ticker:
 * <ul>
 *   <li>Day 1 — spiders shoot webs at their target</li>
 *   <li>Day 11 — Quantum Creepers blink toward the nearest player</li>
 *   <li>Day 23 — Ender Quantum Creepers blink directly onto a player</li>
 *   <li>Day 26 — spiders swim fast</li>
 * </ul>
 *
 * <p>Everything here is player-scoped. The previous version ran three separate timers that each
 * walked {@code world.getEntities()} for every world once or twice a second, so its cost scaled
 * with world size rather than with the number of players actually nearby.
 */
public class SpecialMobManager implements Listener {

    private static final int MOB_SCAN_RADIUS = 64;

    private final PermaDeathDifficultySystem plugin;
    private int tickCounter;

    public SpecialMobManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    public void startTasks() {
        // One 1s ticker; the slower behaviours divide down off the counter.
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        DayManager dayManager = plugin.getDayManager();
        if (dayManager == null) return;

        tickCounter++;
        boolean webTick = tickCounter % 2 == 0;      // every 2s
        boolean teleportTick = tickCounter % 3 == 0; // every 3s

        boolean day1 = dayManager.isDayActive(1);
        boolean day11 = dayManager.isDayActive(11);
        boolean day23 = dayManager.isDayActive(23);
        boolean day26 = dayManager.isDayActive(26);
        if (!day1 && !day11 && !day23 && !day26) return;

        double waterSpeed = plugin.getConfigManager().getDaySetting(26, "spider-water-speed-multiplier", 3.0);

        // Dedupe: a mob within range of two players must only act once.
        Set<Entity> handled = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Entity entity : player.getNearbyEntities(MOB_SCAN_RADIUS, MOB_SCAN_RADIUS, MOB_SCAN_RADIUS)) {
                if (!handled.add(entity)) continue;

                if (entity instanceof Spider spider) {
                    if (day26 && spider.isInWater()) {
                        Vector velocity = spider.getVelocity().multiply(waterSpeed);
                        spider.setVelocity(velocity);
                    }
                    if (day1 && webTick) {
                        shootWeb(spider);
                    }
                } else if (teleportTick && entity instanceof Creeper creeper) {
                    if (day23 && creeper.hasMetadata("pdds_ender_quantum_creeper")) {
                        blinkOntoPlayer(creeper);
                    } else if (day11 && creeper.hasMetadata("pdds_quantum_creeper")) {
                        blinkTowardPlayer(creeper);
                    }
                }
            }
        }
    }

    /** Day 1: a spider webs the ground under whoever it's chasing. */
    private void shootWeb(Spider spider) {
        if (!(spider.getTarget() instanceof Player target)) return;
        if (spider.getLocation().distanceSquared(target.getLocation()) >= 256) return;
        if (Math.random() >= 0.1) return;

        Location webLocation = target.getLocation().getBlock().getLocation();
        if (webLocation.getBlock().getType() != Material.AIR) return;

        webLocation.getBlock().setType(Material.COBWEB);
        spider.getWorld().playSound(webLocation, Sound.ENTITY_SPIDER_HURT, 0.7f, 1.6f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (webLocation.getBlock().getType() == Material.COBWEB) {
                webLocation.getBlock().setType(Material.AIR);
            }
        }, 100L);
    }

    /** Day 11: close part of the gap to the nearest player. */
    private void blinkTowardPlayer(Creeper creeper) {
        Player target = nearestPlayer(creeper, 64);
        if (target == null) return;

        Location from = creeper.getLocation();
        double dx = target.getLocation().getX() - from.getX();
        double dz = target.getLocation().getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 4) return;

        double step = Math.min(5 + Math.random() * 5, distance - 3);
        double x = from.getX() + (dx / distance) * step;
        double z = from.getZ() + (dz / distance) * step;
        teleport(creeper, new Location(creeper.getWorld(), x, 0, z), Particle.PORTAL, 1.5f);
    }

    /** Day 23: blink straight onto the player. */
    private void blinkOntoPlayer(Creeper creeper) {
        Player target = nearestPlayer(creeper, 128);
        if (target == null) return;
        if (creeper.getLocation().distanceSquared(target.getLocation()) < 9) return;

        double angle = Math.random() * Math.PI * 2;
        double distance = 2 + Math.random() * 3;
        Location destination = target.getLocation().clone()
                .add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        teleport(creeper, destination, Particle.DRAGON_BREATH, 0.5f);
    }

    private void teleport(Creeper creeper, Location destination, Particle particle, float pitch) {
        World world = creeper.getWorld();
        // Only land somewhere already loaded; teleporting into unloaded terrain forces a
        // synchronous chunk generation on the main thread.
        if (!world.isChunkLoaded(destination.getBlockX() >> 4, destination.getBlockZ() >> 4)) return;

        destination.setY(world.getHighestBlockYAt(destination) + 1);
        creeper.teleport(destination);
        world.spawnParticle(particle, creeper.getLocation(), 30, 0.5, 1, 0.5);
        world.playSound(creeper.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, pitch);
    }

    private Player nearestPlayer(Entity entity, int range) {
        Player nearest = null;
        double nearestDistance = (double) range * range;

        for (Player player : entity.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(entity.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }
}
