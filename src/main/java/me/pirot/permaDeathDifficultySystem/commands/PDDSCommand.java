package me.pirot.permaDeathDifficultySystem.commands;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import me.pirot.permaDeathDifficultySystem.missions.Mission;
import me.pirot.permaDeathDifficultySystem.missions.MissionDefinitions;
import me.pirot.permaDeathDifficultySystem.missions.MissionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Main command handler for /pdds.
 *
 * Subcommands:
 * /pdds day set <1-35>
 * /pdds day get
 * /pdds day advance
 * /pdds penalty add <player> <slot>
 * /pdds penalty remove <player> <slot>
 * /pdds penalty list <player>
 * /pdds relic give <player> <relic>
 * /pdds relic list
 * /pdds mission status [player]
 * /pdds mission complete <player>
 * /pdds mission reset <player>
 * /pdds reload
 * /pdds help
 */
public class PDDSCommand implements CommandExecutor {

    private final PermaDeathDifficultySystem plugin;

    public PDDSCommand(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "day" -> handleDay(sender, args);
            case "penalty" -> handlePenalty(sender, args);
            case "relic" -> handleRelic(sender, args);
            case "mission" -> handleMission(sender, args);
            case "scoreboard", "sb" -> handleScoreboard(sender);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand. Use /pdds help", NamedTextColor.RED));
            }
        }
        return true;
    }

    // ==================== Day Commands ====================

    private void handleDay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pdds.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /pdds day <set|get|advance>", NamedTextColor.YELLOW));
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "get" -> {
                int day = plugin.getDayManager().getCurrentDay();
                sender.sendMessage(Component.text("Current day: " + day + "/35", NamedTextColor.GREEN));
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /pdds day set <1-35>", NamedTextColor.YELLOW));
                    return;
                }
                try {
                    int day = Integer.parseInt(args[2]);
                    if (day < 1 || day > 35) {
                        sender.sendMessage(Component.text("Day must be between 1 and 35!", NamedTextColor.RED));
                        return;
                    }
                    plugin.getDayManager().setDay(day);
                    sender.sendMessage(Component.text("Day set to " + day, NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
            }
            case "advance" -> {
                plugin.getDayManager().advanceDay();
                sender.sendMessage(Component.text("Advanced to day " + plugin.getDayManager().getCurrentDay(), NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /pdds day <set|get|advance>", NamedTextColor.YELLOW));
        }
    }

    // ==================== Penalty Commands ====================

    private void handlePenalty(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pdds.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /pdds penalty <add|remove|list> <player> [slot]", NamedTextColor.YELLOW));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /pdds penalty add <player> <slot>", NamedTextColor.YELLOW));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                    return;
                }
                try {
                    int slot = Integer.parseInt(args[3]);
                    plugin.getMissionManager().getPenaltyManager().addPenalty(target.getUniqueId(), slot);
                    sender.sendMessage(Component.text("Added penalty slot " + slot + " for " + target.getName(), NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid slot number!", NamedTextColor.RED));
                }
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /pdds penalty remove <player> <slot>", NamedTextColor.YELLOW));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                    return;
                }
                try {
                    int slot = Integer.parseInt(args[3]);
                    plugin.getMissionManager().getPenaltyManager().removePenalty(target.getUniqueId(), slot);
                    sender.sendMessage(Component.text("Removed penalty slot " + slot + " for " + target.getName(), NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid slot number!", NamedTextColor.RED));
                }
            }
            case "list" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /pdds penalty list <player>", NamedTextColor.YELLOW));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                    return;
                }
                Set<Integer> slots = plugin.getMissionManager().getPenaltyManager().getLockedSlots(target.getUniqueId());
                if (slots.isEmpty()) {
                    sender.sendMessage(Component.text(target.getName() + " has no penalties.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(target.getName() + "'s locked slots: " + slots, NamedTextColor.YELLOW));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /pdds penalty <add|remove|list>", NamedTextColor.YELLOW));
        }
    }

    // ==================== Relic Commands ====================

    private void handleRelic(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pdds.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /pdds relic <give|list>", NamedTextColor.YELLOW));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "give" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /pdds relic give <player> <relic-id>", NamedTextColor.YELLOW));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                    return;
                }
                String relicId = args[3].toLowerCase();
                if (plugin.getRelicManager().giveRelic(target, relicId)) {
                    sender.sendMessage(Component.text("Gave relic '" + relicId + "' to " + target.getName(), NamedTextColor.GREEN));
                    target.sendMessage(Component.text("§6§l✦ You received a relic: " + relicId + " ✦"));
                } else {
                    sender.sendMessage(Component.text("Relic '" + relicId + "' not found! Use /pdds relic list", NamedTextColor.RED));
                }
            }
            case "list" -> {
                sender.sendMessage(Component.text("=== Available Relics ===", NamedTextColor.GOLD, TextDecoration.BOLD));
                for (String id : plugin.getRelicManager().getRelicIds()) {
                    String displayName = plugin.getRelicManager().getRelicDisplayName(id);
                    sender.sendMessage(Component.text("  • " + id + " — " + displayName, NamedTextColor.YELLOW));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /pdds relic <give|list>", NamedTextColor.YELLOW));
        }
    }

    // ==================== Mission Commands ====================

    private void handleMission(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /pdds mission <status|complete|reset> [player]", NamedTextColor.YELLOW));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "status" -> {
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                } else if (sender instanceof Player) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage(Component.text("Specify a player!", NamedTextColor.RED));
                    return;
                }
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                    return;
                }

                int currentDay = plugin.getDayManager().getCurrentDay();
                Mission mission = MissionDefinitions.getMission(currentDay);
                if (mission == null) {
                    sender.sendMessage(Component.text("No mission for day " + currentDay, NamedTextColor.YELLOW));
                    return;
                }

                MissionManager mm = plugin.getMissionManager();
                boolean completed = mm.hasCompletedToday(target.getUniqueId());
                String key = MissionManager.getMissionKey(mission);
                int progress = mm.getProgress(target.getUniqueId(), key);

                sender.sendMessage(Component.text("=== Mission Status: " + target.getName() + " ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("  Day: " + currentDay, NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  Mission: " + mission.getName(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  Progress: " + progress + "/" + mission.getTargetCount(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  Completed: " + (completed ? "§aYes" : "§cNo"), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  Reward: " + mission.getRewardDescription(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  Penalty: " + mission.getPenaltyDescription(), NamedTextColor.YELLOW));
            }
            case "complete" -> {
                if (!sender.hasPermission("pdds.admin")) {
                    sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /pdds mission complete <player>", NamedTextColor.YELLOW));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                    return;
                }
                plugin.getMissionManager().forceComplete(target.getUniqueId());
                sender.sendMessage(Component.text("Force-completed mission for " + target.getName(), NamedTextColor.GREEN));
            }
            case "reset" -> {
                if (!sender.hasPermission("pdds.admin")) {
                    sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /pdds mission reset <player>", NamedTextColor.YELLOW));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                    return;
                }
                plugin.getMissionManager().resetProgress(target.getUniqueId());
                sender.sendMessage(Component.text("Reset mission progress for " + target.getName(), NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /pdds mission <status|complete|reset>", NamedTextColor.YELLOW));
        }
    }

    // ==================== Scoreboard Command ====================

    private void handleScoreboard(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can toggle the scoreboard!", NamedTextColor.RED));
            return;
        }
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().toggleScoreboard(player);
        }
    }

    // ==================== Reload ====================

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("pdds.admin")) {
            sender.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return;
        }
        plugin.getConfigManager().reload();
        sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
    }

    // ==================== Help ====================

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("=== PermaDeath Difficulty System ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  /pdds day get", NamedTextColor.YELLOW)
                .append(Component.text(" — Show current day", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds day set <1-35>", NamedTextColor.YELLOW)
                .append(Component.text(" — Set current day", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds day advance", NamedTextColor.YELLOW)
                .append(Component.text(" — Advance to next day", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds penalty add <player> <slot>", NamedTextColor.YELLOW)
                .append(Component.text(" — Lock a slot", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds penalty remove <player> <slot>", NamedTextColor.YELLOW)
                .append(Component.text(" — Unlock a slot", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds penalty list <player>", NamedTextColor.YELLOW)
                .append(Component.text(" — List locked slots", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds relic give <player> <id>", NamedTextColor.YELLOW)
                .append(Component.text(" — Give a relic", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds relic list", NamedTextColor.YELLOW)
                .append(Component.text(" — List all relics", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds mission status [player]", NamedTextColor.YELLOW)
                .append(Component.text(" — Mission progress", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds mission complete <player>", NamedTextColor.YELLOW)
                .append(Component.text(" — Force complete", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds mission reset <player>", NamedTextColor.YELLOW)
                .append(Component.text(" — Reset progress", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds scoreboard", NamedTextColor.YELLOW)
                .append(Component.text(" — Toggle your sidebar scoreboard", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /pdds reload", NamedTextColor.YELLOW)
                .append(Component.text(" — Reload config", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(""));
    }
}
