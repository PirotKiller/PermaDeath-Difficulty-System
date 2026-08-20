package me.pirot.permaDeathDifficultySystem.bosses;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages boss bars for the Ender Dragon and The Core boss encounters.
 */
public class BossBarManager {

    private final PermaDeathDifficultySystem plugin;
    private final Map<String, BossBar> bossBars = new HashMap<>();

    public BossBarManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a boss bar with the given parameters.
     */
    public BossBar createBossBar(String id, String title, BarColor color, BarStyle style, BarFlag... flags) {
        BossBar bar = Bukkit.createBossBar(title, color, style, flags);
        bar.setVisible(true);
        bossBars.put(id, bar);
        return bar;
    }

    /**
     * Adds all online players to a boss bar.
     */
    public void addAllPlayers(String id) {
        BossBar bar = bossBars.get(id);
        if (bar == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(player);
        }
    }

    /**
     * Updates the progress of a boss bar.
     */
    public void updateProgress(String id, double progress) {
        BossBar bar = bossBars.get(id);
        if (bar == null) return;
        bar.setProgress(Math.max(0, Math.min(1, progress)));
    }

    /**
     * Updates the title of a boss bar.
     */
    public void updateTitle(String id, String title) {
        BossBar bar = bossBars.get(id);
        if (bar == null) return;
        bar.setTitle(title);
    }

    /**
     * Removes a boss bar.
     */
    public void removeBossBar(String id) {
        BossBar bar = bossBars.get(id);
        if (bar == null) return;
        bar.removeAll();
        bar.setVisible(false);
        bossBars.remove(id);
    }

    /**
     * Removes all boss bars.
     */
    public void removeAll() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        bossBars.clear();
    }

    /**
     * Gets a boss bar by ID.
     */
    public BossBar getBossBar(String id) {
        return bossBars.get(id);
    }
}
