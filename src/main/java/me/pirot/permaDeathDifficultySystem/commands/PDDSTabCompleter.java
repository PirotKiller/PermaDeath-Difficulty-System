package me.pirot.permaDeathDifficultySystem.commands;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completion for /pdds commands.
 */
public class PDDSTabCompleter implements TabCompleter {

    private final PermaDeathDifficultySystem plugin;

    public PDDSTabCompleter(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("day", "penalty", "relic", "mission", "scoreboard", "sb", "reload", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "day" -> completions.addAll(Arrays.asList("set", "get", "advance"));
                case "penalty" -> completions.addAll(Arrays.asList("add", "remove", "list"));
                case "relic" -> completions.addAll(Arrays.asList("give", "list"));
                case "mission" -> completions.addAll(Arrays.asList("status", "complete", "reset"));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "day" -> {
                    if (args[1].equalsIgnoreCase("set")) {
                        for (int i = 1; i <= 35; i++) {
                            completions.add(String.valueOf(i));
                        }
                    }
                }
                case "penalty", "mission" -> {
                    // Player names
                    completions.addAll(getOnlinePlayerNames());
                }
                case "relic" -> {
                    if (args[1].equalsIgnoreCase("give")) {
                        completions.addAll(getOnlinePlayerNames());
                    }
                }
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "penalty" -> {
                    if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")) {
                        // Slot numbers
                        for (int i = 0; i <= 40; i++) {
                            completions.add(String.valueOf(i));
                        }
                    }
                }
                case "relic" -> {
                    if (args[1].equalsIgnoreCase("give")) {
                        // Relic IDs
                        completions.addAll(plugin.getRelicManager().getRelicIds());
                    }
                }
            }
        }

        // Filter by current input
        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}
