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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks per-player progress against the current day's mission, grants rewards the instant an
 * objective completes, and applies the day's penalty to anyone who missed it at rollover.
 *
 * <p>Exactly one mission is active at a time and progress resets daily, so progress is a single
 * counter per player rather than a map keyed by objective.
 */
public class MissionManager implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final PenaltyManager penaltyManager;

    private final Map<UUID, Integer> progress = new HashMap<>();
    private final Set<UUID> completedToday = new HashSet<>();
    /** Rewards owed to players who completed while offline, or whose completion was forced. */
    private final Set<UUID> pendingRewards = new HashSet<>();
    /** Everyone who has ever joined, so rollover penalties reach offline players too. */
    private final Set<UUID> knownPlayers = new HashSet<>();

    private File dataFile;
    private FileConfiguration data;

    public MissionManager(PermaDeathDifficultySystem plugin, PenaltyManager penaltyManager) {
        this.plugin = plugin;
        this.penaltyManager = penaltyManager;
        loadData();
    }

    // ==================== Persistence ====================

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "mission_data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create mission_data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        if (data.isConfigurationSection("progress")) {
            for (String key : data.getConfigurationSection("progress").getKeys(false)) {
                parseUuid(key).ifPresent(uuid -> progress.put(uuid, data.getInt("progress." + key)));
            }
        }
        readUuidList("completed", completedToday);
        readUuidList("pending-rewards", pendingRewards);
        readUuidList("known-players", knownPlayers);
    }

    private void readUuidList(String path, Set<UUID> into) {
        for (String raw : data.getStringList(path)) {
            parseUuid(raw).ifPresent(into::add);
        }
    }

    private java.util.Optional<UUID> parseUuid(String raw) {
        try {
            return java.util.Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Skipping malformed UUID in mission_data.yml: " + raw);
            return java.util.Optional.empty();
        }
    }

    /**
     * Writes the full state. Each section is cleared first — the previous implementation only
     * ever added keys, so cleared progress reappeared after a restart.
     */
    public void saveData() {
        data.set("progress", null);
        for (Map.Entry<UUID, Integer> entry : progress.entrySet()) {
            data.set("progress." + entry.getKey(), entry.getValue());
        }
        data.set("completed", toStringList(completedToday));
        data.set("pending-rewards", toStringList(pendingRewards));
        data.set("known-players", toStringList(knownPlayers));

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mission_data.yml: " + e.getMessage());
        }
    }

    private static List<String> toStringList(Set<UUID> uuids) {
        List<String> out = new ArrayList<>(uuids.size());
        for (UUID uuid : uuids) out.add(uuid.toString());
        return out;
    }

    // ==================== Progress ====================

    /** Adds progress toward the active mission and completes it if the target is reached. */
    public void addProgress(UUID playerUUID, int amount) {
        if (completedToday.contains(playerUUID)) return;

        Mission mission = MissionDefinitions.getMission(plugin.getDayManager().getCurrentDay());
        if (mission == null) return;

        int updated = progress.getOrDefault(playerUUID, 0) + amount;
        setProgress(playerUUID, updated, mission);
    }

    /**
     * Recomputes progress for an item objective from what the player is actually carrying.
     * Mission items are never consumed, so progress tracks the live inventory count.
     */
    public void recountItems(Player player, Mission mission) {
        if (completedToday.contains(player.getUniqueId())) return;
        if (mission.getTargetItem() == null) return;

        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mission.getTargetItem()) {
                total += item.getAmount();
            }
        }
        if (total > progress.getOrDefault(player.getUniqueId(), 0)) {
            setProgress(player.getUniqueId(), total, mission);
        }
    }

    private void setProgress(UUID playerUUID, int value, Mission mission) {
        progress.put(playerUUID, value);

        if (value >= mission.getTargetCount()) {
            completeMission(playerUUID, mission);
            return;
        }
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            player.sendActionBar(Component.text(
                    "§eMission: " + mission.getName() + " §7[" + value + "/" + mission.getTargetCount() + "]"));
        }
    }

    /** Completes the mission and grants the reward, deferring it if the player is offline. */
    public void completeMission(UUID playerUUID, Mission mission) {
        if (!completedToday.add(playerUUID)) return;

        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            // Don't silently swallow the reward — hand it over on next login.
            pendingRewards.add(playerUUID);
            saveData();
            return;
        }

        grantReward(player, mission);
        Bukkit.broadcast(Component.text("§a§l" + player.getName()
                + " §acompleted today's mission: §e" + mission.getName()));
        saveData();
    }

    private void grantReward(Player player, Mission mission) {
        ItemStack reward = mission.getReward().clone();
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward);

        if (!overflow.isEmpty() && plugin.getConfigManager().isRewardDropIfFull()) {
            for (ItemStack item : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        player.sendMessage(Component.text("✓ Mission Complete: " + mission.getName(),
                NamedTextColor.GREEN, TextDecoration.BOLD));
        player.sendMessage(Component.text("  Reward: " + mission.getRewardDescription(), NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /** Admin override. */
    public void forceComplete(UUID playerUUID) {
        Mission mission = MissionDefinitions.getMission(plugin.getDayManager().getCurrentDay());
        if (mission != null) {
            completeMission(playerUUID, mission);
        }
    }

    public void resetProgress(UUID playerUUID) {
        progress.remove(playerUUID);
        completedToday.remove(playerUUID);
        pendingRewards.remove(playerUUID);
        saveData();
    }

    public int getProgress(UUID playerUUID) {
        return progress.getOrDefault(playerUUID, 0);
    }

    public boolean hasCompletedToday(UUID playerUUID) {
        return completedToday.contains(playerUUID);
    }

    // ==================== Day rollover ====================

    /**
     * Applies the previous day's penalty to everyone who didn't finish, then resets for the new day.
     */
    @EventHandler
    public void onDayAdvance(DayAdvanceEvent event) {
        Mission previous = MissionDefinitions.getMission(event.getPreviousDay());

        if (previous != null && previous.getPenaltySlot() >= 0) {
            // Covers offline players too — they'd otherwise skip the penalty entirely just by
            // logging off before rollover.
            List<UUID> failed = new ArrayList<>();
            for (UUID uuid : knownPlayers) {
                if (!completedToday.contains(uuid)) failed.add(uuid);
            }
            penaltyManager.addPenalties(failed, previous.getPenaltySlot());

            for (UUID uuid : failed) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;
                player.sendMessage(Component.text("§c§l✗ Mission Failed: " + previous.getName()));
                player.sendMessage(Component.text("§c  Penalty: " + previous.getPenaltyDescription()));
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);
            }
        }

        progress.clear();
        completedToday.clear();
        pendingRewards.clear();
        saveData();

        Mission next = MissionDefinitions.getMission(event.getNewDay());
        if (next != null) {
            Bukkit.broadcast(Component.text("§e§l⚔ New Mission: " + next.getName()
                    + " §7(Reward: " + next.getRewardDescription() + ")"));
        }
    }

    // ==================== Join ====================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (knownPlayers.add(uuid)) {
            saveData();
        }

        Mission mission = MissionDefinitions.getMission(plugin.getDayManager().getCurrentDay());
        if (mission == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Hand over anything earned while they were away.
            if (pendingRewards.remove(uuid)) {
                grantReward(player, mission);
                saveData();
            }

            player.sendMessage(Component.text("§e§l⚔ Today's Mission: " + mission.getName()));
            player.sendMessage(Component.text("§7  Reward: " + mission.getRewardDescription()));
            player.sendMessage(Component.text("§7  Penalty: " + mission.getPenaltyDescription()));

            if (hasCompletedToday(uuid)) {
                player.sendMessage(Component.text("§a  ✓ Already completed!"));
            } else {
                // Item objectives may already be satisfied by what they logged in carrying.
                recountItems(player, mission);
                player.sendMessage(Component.text(
                        "§7  Progress: " + getProgress(uuid) + "/" + mission.getTargetCount()));
            }
        }, 40L);
    }

    public PenaltyManager getPenaltyManager() {
        return penaltyManager;
    }
}
