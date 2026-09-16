package me.pirot.permaDeathDifficultySystem.listeners;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Locks the Nether until Day 8 and the End until Day 21.
 *
 * <p>The brief describes Day 8 as "the Nether becomes accessible (if previously locked)" and
 * Day 21 as "the End dimension opens (if previously locked)", which implies both are gated
 * beforehand — Day 21 is also when the dragon encounter fires, so an earlier End trip would
 * skip it. Both gates are individually configurable under {@code day-settings.day-8.nether-gated}
 * and {@code day-settings.day-21.end-gated} if you'd rather run with vanilla portal access.
 */
public class DimensionGateListener implements Listener {

    private final PermaDeathDifficultySystem plugin;

    public DimensionGateListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the day that must be reached before this destination is allowed,
     * or 0 if the destination isn't gated.
     */
    private int requiredDay(Location destination) {
        if (destination == null || destination.getWorld() == null) return 0;

        World.Environment environment = destination.getWorld().getEnvironment();
        if (environment == World.Environment.NETHER
                && plugin.getConfigManager().getDaySetting(8, "nether-gated", true)) {
            return 8;
        }
        if (environment == World.Environment.THE_END
                && plugin.getConfigManager().getDaySetting(21, "end-gated", true)) {
            return 21;
        }
        return 0;
    }

    private boolean isBlocked(Location destination) {
        int required = requiredDay(destination);
        if (required == 0) return false;

        DayManager dayManager = plugin.getDayManager();
        return dayManager != null && !dayManager.isDayActive(required);
    }

    private void refuse(Player player, Location destination) {
        int required = requiredDay(destination);
        String dimension = required == 8 ? "Nether" : "End";

        player.sendMessage(Component.text("✖ The " + dimension + " is sealed until Day " + required + ".",
                NamedTextColor.DARK_RED, TextDecoration.BOLD));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.6f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!isBlocked(event.getTo())) return;
        event.setCancelled(true);
        refuse(event.getPlayer(), event.getTo());
    }

    /** Catches End-portal entry and any plugin/command teleport into a gated dimension. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null || event.getFrom().getWorld() == null || to.getWorld() == null) return;
        if (event.getFrom().getWorld().equals(to.getWorld())) return; // same dimension
        if (!isBlocked(to)) return;

        event.setCancelled(true);
        refuse(event.getPlayer(), to);
    }

    /** Keeps mobs and dropped items from slipping through a gated portal either. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (isBlocked(event.getTo())) {
            event.setCancelled(true);
        }
    }
}
