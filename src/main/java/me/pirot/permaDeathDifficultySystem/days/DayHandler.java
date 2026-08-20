package me.pirot.permaDeathDifficultySystem.days;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.event.Listener;

/**
 * Abstract base class for day-specific handlers.
 * Each handler manages the mechanics for a range of days.
 */
public abstract class DayHandler implements Listener {

    protected final PermaDeathDifficultySystem plugin;

    public DayHandler(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a day within this handler's range starts.
     * Used to initialize day-specific mechanics (e.g., start storms, modify gamerules).
     */
    public abstract void onDayStart(int day);

    /**
     * Checks if a given day falls within this handler's range.
     */
    public abstract boolean handlesDay(int day);

    /**
     * Helper to check if a specific day's effects are currently active.
     */
    protected boolean isDayActive(int day) {
        return plugin.getDayManager().isDayActive(day);
    }

    /**
     * Gets the current day from the DayManager.
     */
    protected int getCurrentDay() {
        return plugin.getDayManager().getCurrentDay();
    }
}
