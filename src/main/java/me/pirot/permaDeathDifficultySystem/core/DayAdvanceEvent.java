package me.pirot.permaDeathDifficultySystem.core;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Custom event fired when the day advances in the difficulty progression.
 */
public class DayAdvanceEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int previousDay;
    private final int newDay;

    public DayAdvanceEvent(int previousDay, int newDay) {
        this.previousDay = previousDay;
        this.newDay = newDay;
    }

    public int getPreviousDay() {
        return previousDay;
    }

    public int getNewDay() {
        return newDay;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
