package me.pirot.permaDeathDifficultySystem.missions;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for game events to track mission progress (kills, item pickups, raid completions).
 */
public class MissionListener implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final MissionManager missionManager;

    public MissionListener(PermaDeathDifficultySystem plugin, MissionManager missionManager) {
        this.plugin = plugin;
        this.missionManager = missionManager;
    }

    /**
     * Track entity kills for KILL_ENTITY missions.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        Entity killed = event.getEntity();
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        int currentDay = plugin.getDayManager().getCurrentDay();
        Mission mission = MissionDefinitions.getMission(currentDay);
        if (mission == null || mission.getObjectiveType() != Mission.ObjectiveType.KILL_ENTITY) return;

        // Check if the killed entity type matches the mission target
        if (mission.getTargetEntity() != null && killed.getType() == mission.getTargetEntity()) {
            missionManager.addProgress(killer.getUniqueId(), killed.getType().name(), 1);
        }

        // Special case for Day 5: Kill Cats AND Wolves
        if (currentDay == 5) {
            if (killed.getType() == org.bukkit.entity.EntityType.CAT
                    || killed.getType() == org.bukkit.entity.EntityType.WOLF) {
                missionManager.addProgress(killer.getUniqueId(), mission.getTargetEntity().name(), 1);
            }
        }

        // Special case for Day 7: Kill Zombie Villagers AND Witches
        if (currentDay == 7) {
            if (killed.getType() == org.bukkit.entity.EntityType.ZOMBIE_VILLAGER
                    || killed.getType() == org.bukkit.entity.EntityType.WITCH) {
                missionManager.addProgress(killer.getUniqueId(), mission.getTargetEntity().name(), 1);
            }
        }
    }

    /**
     * Track item pickups for OBTAIN_ITEM missions.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        int currentDay = plugin.getDayManager().getCurrentDay();
        Mission mission = MissionDefinitions.getMission(currentDay);
        if (mission == null) return;
        if (mission.getObjectiveType() != Mission.ObjectiveType.OBTAIN_ITEM
                && mission.getObjectiveType() != Mission.ObjectiveType.OBTAIN_SPECIFIC_ITEM) return;

        ItemStack item = event.getItem().getItemStack();
        if (mission.getTargetItem() != null && item.getType() == mission.getTargetItem()) {
            missionManager.addProgress(player.getUniqueId(), item.getType().name(), item.getAmount());
        }
    }

    /**
     * Track inventory interactions for OBTAIN_ITEM missions (crafting, smelting, etc.).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int currentDay = plugin.getDayManager().getCurrentDay();
        Mission mission = MissionDefinitions.getMission(currentDay);
        if (mission == null) return;
        if (mission.getObjectiveType() != Mission.ObjectiveType.OBTAIN_ITEM
                && mission.getObjectiveType() != Mission.ObjectiveType.OBTAIN_SPECIFIC_ITEM) return;

        ItemStack cursor = event.getCursor();
        if (cursor != null && mission.getTargetItem() != null && cursor.getType() == mission.getTargetItem()) {
            // Count items in player's inventory
            int totalCount = 0;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.getType() == mission.getTargetItem()) {
                    totalCount += invItem.getAmount();
                }
            }
            // Add the cursor amount
            totalCount += cursor.getAmount();

            // Update progress to the total count (overwrite, not add)
            String key = mission.getTargetItem().name();
            if (totalCount >= mission.getTargetCount()) {
                missionManager.addProgress(player.getUniqueId(), key,
                        mission.getTargetCount() - missionManager.getProgress(player.getUniqueId(), key));
            }
        }
    }

    /**
     * Track raid completions for COMPLETE_RAID missions.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidFinish(RaidFinishEvent event) {
        int currentDay = plugin.getDayManager().getCurrentDay();
        Mission mission = MissionDefinitions.getMission(currentDay);
        if (mission == null || mission.getObjectiveType() != Mission.ObjectiveType.COMPLETE_RAID) return;

        // Credit all players who participated in the raid
        for (Player player : event.getWinners()) {
            missionManager.addProgress(player.getUniqueId(), "RAID_COMPLETE", 1);
        }
    }
}
