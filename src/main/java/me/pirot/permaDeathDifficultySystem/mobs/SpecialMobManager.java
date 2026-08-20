package me.pirot.permaDeathDifficultySystem.mobs;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Manages special mob behaviors:
 * - Quantum Creeper teleportation toward players
 * - Spider web shooting (Day 1)
 * - Spider water speed (Day 26)
 */
public class SpecialMobManager implements Listener {

    private final PermaDeathDifficultySystem plugin;

    public SpecialMobManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts periodic tasks for special mob behaviors.
     */
    public void startTasks() {
        // Quantum Creeper teleportation — every 3 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickQuantumCreepers, 60L, 60L);

        // Spider web shooting — every 2 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickSpiderWebs, 40L, 40L);

        // Spider water speed — every 1 second
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickSpiderWaterSpeed, 20L, 20L);
    }

    /**
     * Quantum Creepers teleport toward the nearest player periodically.
     */
    private void tickQuantumCreepers() {
        DayManager dm = plugin.getDayManager();
        if (dm == null || !dm.isDayActive(11)) return;

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Creeper creeper && entity.hasMetadata("pdds_quantum_creeper")) {
                    // Find nearest player
                    Player nearest = null;
                    double nearestDist = 64 * 64; // 64 block range
                    for (Player p : world.getPlayers()) {
                        double dist = p.getLocation().distanceSquared(creeper.getLocation());
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = p;
                        }
                    }

                    if (nearest != null && nearestDist < 64 * 64 && nearestDist > 4 * 4) {
                        // Teleport partway toward the player
                        Location creeperLoc = creeper.getLocation();
                        Location playerLoc = nearest.getLocation();
                        double dx = playerLoc.getX() - creeperLoc.getX();
                        double dz = playerLoc.getZ() - creeperLoc.getZ();
                        double distance = Math.sqrt(dx * dx + dz * dz);

                        // Teleport 5-10 blocks toward the player
                        double teleportDist = Math.min(5 + Math.random() * 5, distance - 3);
                        double newX = creeperLoc.getX() + (dx / distance) * teleportDist;
                        double newZ = creeperLoc.getZ() + (dz / distance) * teleportDist;
                        int newY = world.getHighestBlockYAt((int) newX, (int) newZ);

                        Location teleportLoc = new Location(world, newX, newY + 1, newZ);
                        creeper.teleport(teleportLoc);

                        // Particle/sound effect
                        world.spawnParticle(org.bukkit.Particle.PORTAL, creeper.getLocation(), 30, 0.5, 1, 0.5);
                        world.playSound(creeper.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                    }
                }

                // Ender Quantum Creepers (Day 23) - same but more aggressive
                if (entity instanceof Creeper creeper && entity.hasMetadata("pdds_ender_quantum_creeper")) {
                    Player nearest = null;
                    double nearestDist = 128 * 128;
                    for (Player p : world.getPlayers()) {
                        double dist = p.getLocation().distanceSquared(creeper.getLocation());
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = p;
                        }
                    }

                    if (nearest != null && nearestDist > 3 * 3) {
                        Location playerLoc = nearest.getLocation();
                        // Teleport very close to the player
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 2 + Math.random() * 3;
                        Location teleportLoc = playerLoc.clone().add(
                                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        teleportLoc.setY(world.getHighestBlockYAt(teleportLoc) + 1);

                        creeper.teleport(teleportLoc);
                        world.spawnParticle(org.bukkit.Particle.DRAGON_BREATH, creeper.getLocation(), 50, 0.5, 1, 0.5);
                        world.playSound(creeper.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    }
                }
            }
        }
    }

    /**
     * Day 1: Spiders shoot webs (place cobwebs near their target).
     */
    private void tickSpiderWebs() {
        DayManager dm = plugin.getDayManager();
        if (dm == null || !dm.isDayActive(1)) return;

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Spider spider && spider.getTarget() instanceof Player) {
                    Player target = (Player) spider.getTarget();
                    double dist = spider.getLocation().distanceSquared(target.getLocation());

                    // Shoot web if within 16 blocks and random chance
                    if (dist < 256 && Math.random() < 0.1) {
                        Location webLoc = target.getLocation().clone();
                        // Place cobweb at player's feet if the block is air
                        if (webLoc.getBlock().getType() == org.bukkit.Material.AIR) {
                            webLoc.getBlock().setType(org.bukkit.Material.COBWEB);
                            // Remove after 5 seconds
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (webLoc.getBlock().getType() == org.bukkit.Material.COBWEB) {
                                    webLoc.getBlock().setType(org.bukkit.Material.AIR);
                                }
                            }, 100L);
                        }
                    }
                }
            }
        }
    }

    /**
     * Day 26: Spiders move extremely fast through water.
     */
    private void tickSpiderWaterSpeed() {
        DayManager dm = plugin.getDayManager();
        if (dm == null || !dm.isDayActive(26)) return;

        double speedMultiplier = plugin.getConfigManager().getDaySetting(26, "spider-water-speed-multiplier", 3.0);

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Spider spider) {
                    // Check if spider is in water
                    if (spider.isInWater()) {
                        // Boost velocity
                        org.bukkit.util.Vector velocity = spider.getVelocity();
                        velocity.multiply(speedMultiplier);
                        spider.setVelocity(velocity);
                    }
                }
            }
        }
    }
}
