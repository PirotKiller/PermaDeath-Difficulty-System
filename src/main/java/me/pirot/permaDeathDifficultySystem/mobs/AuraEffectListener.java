package me.pirot.permaDeathDifficultySystem.mobs;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Proximity auras radiated by mobs onto nearby players:
 * <ul>
 *   <li>Day 4 — Wolves apply Hunger</li>
 *   <li>Day 5 — Cats apply Darkness</li>
 *   <li>Day 13 — Phantoms apply extreme Levitation</li>
 *   <li>Day 15 — Axolotls apply Poison II</li>
 *   <li>Day 17 — Wardens apply extreme Slowness</li>
 *   <li>Day 19 — Bats apply Blindness</li>
 * </ul>
 *
 * <p>All six resolve from a single proximity query per player, sized to the widest active
 * radius, with per-aura distance checked afterwards.
 */
public class AuraEffectListener {

    private final PermaDeathDifficultySystem plugin;

    public AuraEffectListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAuras, 40L, 40L);
    }

    private void checkAuras() {
        DayManager dayManager = plugin.getDayManager();
        if (dayManager == null) return;

        boolean day4 = dayManager.isDayActive(4);
        boolean day5 = dayManager.isDayActive(5);
        boolean day13 = dayManager.isDayActive(13);
        boolean day15 = dayManager.isDayActive(15);
        boolean day17 = dayManager.isDayActive(17);
        boolean day19 = dayManager.isDayActive(19);
        if (!day4 && !day5 && !day13 && !day15 && !day17 && !day19) return;

        int wolfRadius = day4 ? plugin.getConfigManager().getDaySetting(4, "wolf-hunger-radius", 16) : 0;
        int catRadius = day5 ? plugin.getConfigManager().getDaySetting(5, "cat-darkness-radius", 16) : 0;
        int phantomRadius = day13 ? plugin.getConfigManager().getDaySetting(13, "phantom-levitation-radius", 16) : 0;
        int axolotlRadius = day15 ? plugin.getConfigManager().getDaySetting(15, "axolotl-poison-radius", 16) : 0;
        int wardenRadius = day17 ? plugin.getConfigManager().getDaySetting(17, "warden-slowness-radius", 20) : 0;
        int batRadius = day19 ? plugin.getConfigManager().getDaySetting(19, "bat-blindness-radius", 16) : 0;

        int scanRadius = Math.max(Math.max(Math.max(wolfRadius, catRadius), Math.max(phantomRadius, axolotlRadius)),
                Math.max(wardenRadius, batRadius));
        if (scanRadius <= 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) continue;

            boolean hunger = false, darkness = false, levitation = false;
            boolean poison = false, slowness = false, blindness = false;

            for (Entity entity : player.getNearbyEntities(scanRadius, scanRadius, scanRadius)) {
                double distanceSq = entity.getLocation().distanceSquared(player.getLocation());

                if (!hunger && day4 && entity instanceof Wolf && within(distanceSq, wolfRadius)) hunger = true;
                else if (!darkness && day5 && entity instanceof Cat && within(distanceSq, catRadius)) darkness = true;
                else if (!levitation && day13 && entity instanceof Phantom && within(distanceSq, phantomRadius)) levitation = true;
                else if (!poison && day15 && entity instanceof Axolotl && within(distanceSq, axolotlRadius)) poison = true;
                else if (!slowness && day17 && entity instanceof Warden && within(distanceSq, wardenRadius)) slowness = true;
                else if (!blindness && day19 && entity instanceof Bat && within(distanceSq, batRadius)) blindness = true;
            }

            if (hunger) apply(player, PotionEffectType.HUNGER, 100, 0);
            if (darkness) apply(player, PotionEffectType.DARKNESS, 100, 0);
            if (levitation) apply(player, PotionEffectType.LEVITATION, 60, 4);
            if (poison) apply(player, PotionEffectType.POISON, 100, 1);
            if (slowness) apply(player, PotionEffectType.SLOW, 100, 4);
            if (blindness) apply(player, PotionEffectType.BLINDNESS, 100, 0);
        }
    }

    private static boolean within(double distanceSq, int radius) {
        return radius > 0 && distanceSq <= (double) radius * radius;
    }

    private static void apply(Player player, PotionEffectType type, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, false, false, true));
    }
}
