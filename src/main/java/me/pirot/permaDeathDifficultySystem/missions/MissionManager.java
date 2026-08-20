package me.pirot.permaDeathDifficultySystem.missions;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.core.DayAdvanceEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages active missions, tracks progress per player, grants rewards, and applies penalties.
 */
public class MissionManager implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final PenaltyManager penaltyManager;

    // Player UUID -> {entityType/itemType -> count}
    private final Map<UUID, Map<String, Integer>> playerProgress = new HashMap<>();
    private final Set<UUID> completedToday = new HashSet<>();

    private File dataFile;
    private FileConfiguration data;

    public MissionManager(PermaDeathDifficultySystem plugin, PenaltyManager penaltyManager) {
        this.plugin = plugin;
        this.penaltyManager = penaltyManager;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "mission_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create mission_data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load progress from file
        if (data.contains("progress")) {
            for (String uuidStr : data.getConfigurationSection("progress").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Integer> progress = new HashMap<>();
                for (String key : data.getConfigurationSection("progress." + uuidStr).getKeys(false)) {
                    progress.put(key, data.getInt("progress." + uuidStr + "." + key));
                }
                playerProgress.put(uuid, progress);
            }
        }

        if (data.contains("completed")) {
            for (String uuidStr : data.getStringList("completed")) {
                completedToday.add(UUID.fromString(uuidStr));
            }
        }
    }

    public void saveData() {
        // Save progress
        for (Map.Entry<UUID, Map<String, Integer>> entry : playerProgress.entrySet()) {
            for (Map.Entry<String, Integer> progress : entry.getValue().entrySet()) {
                data.set("progress." + entry.getKey().toString() + "." + progress.getKey(), progress.getValue());
            }
        }

        // Save completed
        List<String> completedList = new ArrayList<>();
        for (UUID uuid : completedToday) {
            completedList.add(uuid.toString());
        }
        data.set("completed", completedList);

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mission_data.yml: " + e.getMessage());
        }
    }

    /**
     * Adds progress for a player toward the current day's mission.
     */
    public void addProgress(UUID playerUUID, String key, int amount) {
        if (completedToday.contains(playerUUID)) return;

        int currentDay = plugin.getDayManager().getCurrentDay();
        Mission mission = MissionDefinitions.getMission(currentDay);
        if (mission == null) return;

        Map<String, Integer> progress = playerProgress.computeIfAbsent(playerUUID, k -> new HashMap<>());
        int current = progress.getOrDefault(key, 0) + amount;
        progress.put(key, current);

        // Check completion
        if (current >= mission.getTargetCount()) {
            completeMission(playerUUID, mission);
        } else {
            // Show progress
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                player.sendActionBar(Component.text(
                        "§eMission: " + mission.getName() + " §7[" + current + "/" + mission.getTargetCount() + "]"));
            }
        }
    }

    /**
     * Completes a mission for a player — grants reward.
     */
    public void completeMission(UUID playerUUID, Mission mission) {
        if (completedToday.contains(playerUUID)) return;
        completedToday.add(playerUUID);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        // Grant reward
        ItemStack reward = mission.getReward().clone();
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(reward);

        // Drop overflow items
        if (!overflow.isEmpty() && plugin.getConfigManager().isRewardDropIfFull()) {
            for (ItemStack item : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        // Announce
        player.sendMessage(Component.text("✓ Mission Complete: " + mission.getName(), NamedTextColor.GREEN, TextDecoration.BOLD));
        player.sendMessage(Component.text("  Reward: " + mission.getRewardDescription(), NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Broadcast
        Bukkit.broadcast(Component.text("§a§l" + player.getName() + " §acompleted today's mission: §e" + mission.getName()));

        saveData();
    }

    /**
     * Forces completion of a player's current mission (admin command).
     */
    public void forceComplete(UUID playerUUID) {
        int currentDay = plugin.getDayManager().getCurrentDay();
        Mission mission = MissionDefinitions.getMission(currentDay);
        if (mission != null) {
            completeMission(playerUUID, mission);
        }
    }

    /**
     * Resets a player's current mission progress.
     */
    public void resetProgress(UUID playerUUID) {
        playerProgress.remove(playerUUID);
        completedToday.remove(playerUUID);
        saveData();
    }

    /**
     * Gets the current progress for a player.
     */
    public int getProgress(UUID playerUUID, String key) {
        Map<String, Integer> progress = playerProgress.get(playerUUID);
        return progress != null ? progress.getOrDefault(key, 0) : 0;
    }

    /**
     * Checks if a player has completed today's mission.
     */
    public boolean hasCompletedToday(UUID playerUUID) {
        return completedToday.contains(playerUUID);
    }

    /**
     * Called when the day advances — applies penalties for incomplete missions and resets.
     */
    @EventHandler
    public void onDayAdvance(DayAdvanceEvent event) {
        int previousDay = event.getPreviousDay();
        Mission previousMission = MissionDefinitions.getMission(previousDay);
        if (previousMission == null) return;

        // Apply penalties to all players who didn't complete the mission
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!completedToday.contains(player.getUniqueId())) {
                // Apply penalty
                int penaltySlot = previousMission.getPenaltySlot();
                if (penaltySlot >= 0) {
                    penaltyManager.addPenalty(player.getUniqueId(), penaltySlot);
                    player.sendMessage(Component.text("§c§l✗ Mission Failed: " + previousMission.getName()));
                    player.sendMessage(Component.text("§c  Penalty: " + previousMission.getPenaltyDescription()));
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);
                }
            }
        }

        // Reset progress for the new day
        playerProgress.clear();
        completedToday.clear();
        saveData();

        // Announce new mission
        Mission newMission = MissionDefinitions.getMission(event.getNewDay());
        if (newMission != null) {
            Bukkit.broadcast(Component.text("§e§l⚔ New Mission: " + newMission.getName()
                    + " §7(Reward: " + newMission.getRewardDescription() + ")"));
        }
    }

    /**
     * Shows mission info to a joining player.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        int currentDay = plugin.getDayManager().getCurrentDay();
        Mission mission = MissionDefinitions.getMission(currentDay);
        if (mission == null) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(Component.text("§e§l⚔ Today's Mission: " + mission.getName()));
            player.sendMessage(Component.text("§7  Reward: " + mission.getRewardDescription()));
            player.sendMessage(Component.text("§7  Penalty: " + mission.getPenaltyDescription()));

            if (hasCompletedToday(player.getUniqueId())) {
                player.sendMessage(Component.text("§a  ✓ Already completed!"));
            } else {
                String key = getMissionKey(mission);
                int progress = getProgress(player.getUniqueId(), key);
                player.sendMessage(Component.text("§7  Progress: " + progress + "/" + mission.getTargetCount()));
            }
        }, 40L);
    }

    /**
     * Gets the tracking key for a mission (entity type name or item type name).
     */
    public static String getMissionKey(Mission mission) {
        if (mission.getObjectiveType() == Mission.ObjectiveType.KILL_ENTITY) {
            return mission.getTargetEntity() != null ? mission.getTargetEntity().name() : "UNKNOWN";
        } else if (mission.getObjectiveType() == Mission.ObjectiveType.OBTAIN_ITEM
                || mission.getObjectiveType() == Mission.ObjectiveType.OBTAIN_SPECIFIC_ITEM) {
            return mission.getTargetItem() != null ? mission.getTargetItem().name() : "UNKNOWN";
        } else if (mission.getObjectiveType() == Mission.ObjectiveType.COMPLETE_RAID) {
            return "RAID_COMPLETE";
        }
        return "UNKNOWN";
    }

    public PenaltyManager getPenaltyManager() {
        return penaltyManager;
    }
}
