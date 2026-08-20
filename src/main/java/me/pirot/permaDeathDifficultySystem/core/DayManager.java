package me.pirot.permaDeathDifficultySystem.core;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

/**
 * Manages the 35-day difficulty progression timer and day advancement.
 */
public class DayManager {

    private final PermaDeathDifficultySystem plugin;
    private final ConfigManager configManager;
    private int currentDay;
    private BukkitTask advanceTask;

    private long dayStartTime;

    public DayManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.currentDay = configManager.getCurrentDay();
        this.dayStartTime = System.currentTimeMillis();
    }

    /**
     * Starts the automatic day advancement timer if auto-advance is enabled.
     */
    public void startTimer() {
        stopTimer();
        this.dayStartTime = System.currentTimeMillis();
        if (!configManager.isAutoAdvance()) return;

        long durationTicks = configManager.getDayDurationMinutes() * 60L * 20L; // minutes -> ticks
        advanceTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentDay < 35) {
                advanceDay();
            } else {
                stopTimer();
            }
        }, durationTicks, durationTicks);

        plugin.getLogger().info("Day advancement timer started: " + configManager.getDayDurationMinutes() + " minute intervals.");
    }

    /**
     * Stops the automatic day advancement timer.
     */
    public void stopTimer() {
        if (advanceTask != null) {
            advanceTask.cancel();
            advanceTask = null;
        }
    }

    /**
     * Gets the current day in the 35-day progression.
     */
    public int getCurrentDay() {
        return currentDay;
    }

    /**
     * Sets the current day, fires DayAdvanceEvent, and persists.
     */
    public void setDay(int day) {
        int previousDay = this.currentDay;
        this.currentDay = Math.max(1, Math.min(35, day));
        configManager.setCurrentDay(this.currentDay);

        // Fire event
        DayAdvanceEvent event = new DayAdvanceEvent(previousDay, this.currentDay);
        Bukkit.getPluginManager().callEvent(event);

        // Broadcast
        if (configManager.isBroadcastDayChange()) {
            broadcastDayChange(this.currentDay);
        }

        plugin.getLogger().info("Day changed from " + previousDay + " to " + this.currentDay);

        // Restart timer
        if (configManager.isAutoAdvance()) {
            startTimer();
        }
    }

    /**
     * Advances to the next day.
     */
    public void advanceDay() {
        if (currentDay < 35) {
            setDay(currentDay + 1);
        }
    }

    /**
     * Gets remaining seconds in the current day cycle.
     */
    public long getRemainingSeconds() {
        if (!configManager.isAutoAdvance()) return 0;
        long totalSeconds = configManager.getDayDurationMinutes() * 60L;
        long elapsedSeconds = (System.currentTimeMillis() - dayStartTime) / 1000L;
        return Math.max(0, totalSeconds - elapsedSeconds);
    }

    /**
     * Checks if a specific day's effects should be active (considering cumulative mode).
     */
    public boolean isDayActive(int day) {
        if (!configManager.isDayEnabled(day)) return false;
        if (configManager.isCumulativeEffects()) {
            return day <= currentDay;
        }
        return day == currentDay;
    }

    /**
     * Broadcasts the day change to all online players with a title and sound.
     */
    private void broadcastDayChange(int day) {
        Component title = Component.text("Day " + day, NamedTextColor.RED, TextDecoration.BOLD);
        Component subtitle = getDaySubtitle(day);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(3),
                Duration.ofMillis(1000)
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(title, subtitle, times));
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
        }

        Bukkit.broadcast(Component.text("☠ Day " + day + " of the PermaDeath challenge has begun! ☠",
                NamedTextColor.DARK_RED, TextDecoration.BOLD));
    }

    private Component getDaySubtitle(int day) {
        if (day <= 7) return Component.text("The nightmare begins...", NamedTextColor.GRAY, TextDecoration.ITALIC);
        if (day <= 14) return Component.text("Darkness grows stronger...", NamedTextColor.GRAY, TextDecoration.ITALIC);
        if (day <= 21) return Component.text("Hope fades away...", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC);
        if (day <= 28) return Component.text("The end is near...", NamedTextColor.DARK_PURPLE, TextDecoration.ITALIC);
        if (day <= 34) return Component.text("No escape...", NamedTextColor.DARK_RED, TextDecoration.ITALIC);
        return Component.text("FINAL JUDGMENT", NamedTextColor.DARK_RED, TextDecoration.BOLD);
    }
}
