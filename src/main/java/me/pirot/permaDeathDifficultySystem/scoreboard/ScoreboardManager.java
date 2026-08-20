package me.pirot.permaDeathDifficultySystem.scoreboard;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.missions.Mission;
import me.pirot.permaDeathDifficultySystem.missions.MissionDefinitions;
import me.pirot.permaDeathDifficultySystem.missions.MissionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Manages custom sidebar scoreboard for tracking day progression,
 * countdown timers, daily mission status, progress, locked penalties, and online players.
 */
public class ScoreboardManager implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Set<UUID> disabledPlayers = new HashSet<>();
    private BukkitTask updateTask;

    public ScoreboardManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the periodic scoreboard update task (every 1 second / 20 ticks).
     */
    public void start() {
        stop();
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Setup board for currently online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            setupScoreboard(player);
        }

        // Update task every second
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
        plugin.getLogger().info("Scoreboard system initialized.");
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        playerScoreboards.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setupScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerScoreboards.remove(event.getPlayer().getUniqueId());
        disabledPlayers.remove(event.getPlayer().getUniqueId());
    }

    public void toggleScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledPlayers.contains(uuid)) {
            disabledPlayers.remove(uuid);
            setupScoreboard(player);
            player.sendMessage(Component.text("§aScoreboard enabled."));
        } else {
            disabledPlayers.add(uuid);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            playerScoreboards.remove(uuid);
            player.sendMessage(Component.text("§cScoreboard disabled."));
        }
    }

    private void setupScoreboard(Player player) {
        if (disabledPlayers.contains(player.getUniqueId())) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("pdds_board", Criteria.DUMMY,
                parseComponent("§4§l☠ PERMADEATH ☠"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Pre-create teams for 12 lines to allow smooth flicker-free updating
        for (int line = 1; line <= 12; line++) {
            String entryKey = getEntryKey(line);
            Team team = board.registerNewTeam("line_" + line);
            team.addEntry(entryKey);
            obj.getScore(entryKey).setScore(line);
        }

        player.setScoreboard(board);
        playerScoreboards.put(player.getUniqueId(), board);
        updateScoreboard(player, board);
    }

    private void updateAll() {
        if (!plugin.getConfigManager().getBoolean("scoreboard.enabled", true)) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (disabledPlayers.contains(player.getUniqueId())) continue;
            Scoreboard board = playerScoreboards.get(player.getUniqueId());
            if (board == null) {
                setupScoreboard(player);
            } else {
                updateScoreboard(player, board);
            }
        }
    }

    private void updateScoreboard(Player player, Scoreboard board) {
        Objective obj = board.getObjective("pdds_board");
        if (obj == null) return;

        // Custom Title
        String titleStr = plugin.getConfigManager().getString("scoreboard.title", "§4§l☠ PERMADEATH ☠");
        obj.displayName(parseComponent(titleStr));

        int day = plugin.getDayManager().getCurrentDay();
        long remainingSec = plugin.getDayManager().getRemainingSeconds();
        String timeFormatted = formatTime(remainingSec);

        // Mission details
        Mission mission = MissionDefinitions.getMission(day);
        String missionName = (mission != null) ? mission.getName() : "None";
        if (missionName.length() > 20) {
            missionName = missionName.substring(0, 18) + "..";
        }

        String progressStr;
        MissionManager mm = plugin.getMissionManager();
        if (mm != null && mm.hasCompletedToday(player.getUniqueId())) {
            progressStr = "§a✔ Completed";
        } else if (mission != null && mm != null) {
            String key = MissionManager.getMissionKey(mission);
            int current = mm.getProgress(player.getUniqueId(), key);
            progressStr = "§a" + current + " §7/ §e" + mission.getTargetCount();
        } else {
            progressStr = "§7N/A";
        }

        // Penalty details
        int lockedSlots = 0;
        if (mm != null && mm.getPenaltyManager() != null) {
            lockedSlots = mm.getPenaltyManager().getLockedSlots(player.getUniqueId()).size();
        }
        String penaltyStr = (lockedSlots > 0) ? "§c" + lockedSlots + " Locked" : "§aNone";

        String footer = plugin.getConfigManager().getString("scoreboard.footer", "§7permadeath.server");

        // Build 12 lines from top (score 12) down to bottom (score 1)
        updateLine(board, 12, "§7§m-------------------");
        updateLine(board, 11, "§fDay: §cDay " + day + " §7/ 35");
        updateLine(board, 10, "§fNext Day: §e" + timeFormatted);
        updateLine(board, 9, "§1 ");
        updateLine(board, 8, "§fMission: §e" + missionName);
        updateLine(board, 7, "§fProgress: " + progressStr);
        updateLine(board, 6, "§2 ");
        updateLine(board, 5, "§fPenalties: " + penaltyStr);
        updateLine(board, 4, "§3 ");
        updateLine(board, 3, "§fOnline: §b" + Bukkit.getOnlinePlayers().size());
        updateLine(board, 2, "§4 ");
        updateLine(board, 1, footer);
    }

    private void updateLine(Scoreboard board, int line, String text) {
        Team team = board.getTeam("line_" + line);
        if (team != null) {
            Component prefix = parseComponent(text);
            team.prefix(prefix);
        }
    }

    private String getEntryKey(int line) {
        return ChatColor.COLOR_CHAR + String.valueOf((char) ('a' + line));
    }

    private Component parseComponent(String legacyText) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacyText.replace('§', '&'));
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "Advancing...";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02dm %02ds", minutes, secs);
    }
}
