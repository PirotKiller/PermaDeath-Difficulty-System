package me.pirot.permaDeathDifficultySystem.days;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayAdvanceEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry that manages all DayHandler instances and routes day-change events to them.
 */
public class DayHandlerRegistry implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final List<DayHandler> handlers = new ArrayList<>();

    public DayHandlerRegistry(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all day handlers and registers them as event listeners.
     */
    public void registerAll() {
        registerHandler(new EarlyDaysHandler(plugin));
        registerHandler(new MidDaysHandler(plugin));
        registerHandler(new LateDaysHandler(plugin));
        registerHandler(new EndgameDaysHandler(plugin));

        // Register this registry as a listener to receive DayAdvanceEvents
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("Registered " + handlers.size() + " day handlers.");

        // Initialize current day
        int currentDay = plugin.getDayManager().getCurrentDay();
        for (DayHandler handler : handlers) {
            if (handler.handlesDay(currentDay)) {
                handler.onDayStart(currentDay);
            }
        }
    }

    private void registerHandler(DayHandler handler) {
        handlers.add(handler);
        Bukkit.getPluginManager().registerEvents(handler, plugin);
        handler.startTasks();
    }

    @EventHandler
    public void onDayAdvance(DayAdvanceEvent event) {
        int newDay = event.getNewDay();
        for (DayHandler handler : handlers) {
            if (handler.handlesDay(newDay)) {
                handler.onDayStart(newDay);
            }
        }
    }

    public List<DayHandler> getHandlers() {
        return handlers;
    }
}
