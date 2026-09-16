package me.pirot.permaDeathDifficultySystem.missions;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Translates game events into mission progress.
 *
 * <p>Item objectives are evaluated by counting what the player actually holds rather than by
 * accumulating pickup deltas, so crafting, smelting, shift-clicking out of a chest and picking
 * items up off the floor all count, and nothing double-counts.
 */
public class MissionListener implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final MissionManager missionManager;

    public MissionListener(PermaDeathDifficultySystem plugin, MissionManager missionManager) {
        this.plugin = plugin;
        this.missionManager = missionManager;
    }

    private Mission currentMission() {
        return MissionDefinitions.getMission(plugin.getDayManager().getCurrentDay());
    }

    /** Kill objectives. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        Mission mission = currentMission();
        if (mission == null || mission.getObjectiveType() != Mission.ObjectiveType.KILL_ENTITY) return;
        if (!mission.matchesEntity(event.getEntity().getType())) return;

        missionManager.addProgress(killer.getUniqueId(), 1);
    }

    /** Item objectives: recount on pickup. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            scheduleRecount(player);
        }
    }

    /** Item objectives: recount after crafting. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRecount(player);
        }
    }

    /** Item objectives: recount after any inventory interaction that could move items in. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRecount(player);
        }
    }

    /** Item objectives: recount when a furnace or similar result is collected. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(org.bukkit.event.inventory.FurnaceExtractEvent event) {
        scheduleRecount(event.getPlayer());
    }

    /**
     * Recounts on the next tick, once the inventory change has actually settled — during the
     * event the moved stack may still be on the cursor rather than in a slot.
     */
    private void scheduleRecount(Player player) {
        Mission mission = currentMission();
        if (mission == null) return;
        if (mission.getObjectiveType() != Mission.ObjectiveType.OBTAIN_ITEM
                && mission.getObjectiveType() != Mission.ObjectiveType.OBTAIN_SPECIFIC_ITEM) return;
        if (missionManager.hasCompletedToday(player.getUniqueId())) return;

        Bukkit.getScheduler().runTask(plugin, () -> missionManager.recountItems(player, mission));
    }

    /** Raid objectives. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidFinish(RaidFinishEvent event) {
        Mission mission = currentMission();
        if (mission == null || mission.getObjectiveType() != Mission.ObjectiveType.COMPLETE_RAID) return;

        for (Player player : event.getWinners()) {
            missionManager.addProgress(player.getUniqueId(), 1);
        }
    }
}
