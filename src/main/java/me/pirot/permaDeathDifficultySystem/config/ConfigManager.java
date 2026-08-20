package me.pirot.permaDeathDifficultySystem.config;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages all plugin configuration loading, saving, and typed access.
 */
public class ConfigManager {

    private final PermaDeathDifficultySystem plugin;
    private FileConfiguration config;

    public ConfigManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public void save() {
        plugin.saveConfig();
    }

    // ==================== General ====================

    public int getCurrentDay() {
        return config.getInt("general.current-day", 1);
    }

    public void setCurrentDay(int day) {
        config.set("general.current-day", Math.max(1, Math.min(35, day)));
        save();
    }

    public int getDayDurationMinutes() {
        return config.getInt("general.day-duration-minutes", 60);
    }

    public boolean isAutoAdvance() {
        return config.getBoolean("general.auto-advance", true);
    }

    public boolean isCumulativeEffects() {
        return config.getBoolean("general.cumulative-effects", true);
    }

    public boolean isBroadcastDayChange() {
        return config.getBoolean("general.broadcast-day-change", true);
    }

    // ==================== Day Settings ====================

    public boolean isDayEnabled(int day) {
        return config.getBoolean("day-settings.day-" + day + ".enabled", true);
    }

    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    // ==================== Day-Specific Helpers ====================

    public int getDaySetting(int day, String key, int defaultValue) {
        return config.getInt("day-settings.day-" + day + "." + key, defaultValue);
    }

    public double getDaySetting(int day, String key, double defaultValue) {
        return config.getDouble("day-settings.day-" + day + "." + key, defaultValue);
    }

    public boolean getDaySetting(int day, String key, boolean defaultValue) {
        return config.getBoolean("day-settings.day-" + day + "." + key, defaultValue);
    }

    // ==================== AI ====================

    public boolean isWallTargeting() {
        return config.getBoolean("ai.wall-targeting", true);
    }

    public boolean isInvisibilityCounter() {
        return config.getBoolean("ai.invisibility-counter", true);
    }

    public int getDefaultDetectionRange() {
        return config.getInt("ai.default-detection-range", 48);
    }

    // ==================== Relics ====================

    public boolean isRelicEnabled(String tier, String relicKey) {
        return config.getBoolean("relics." + tier + "." + relicKey, true);
    }

    // ==================== Missions ====================

    public boolean isRewardDropIfFull() {
        return config.getBoolean("missions.reward-drop-if-full", true);
    }

    public boolean isItemsRemainAfterCompletion() {
        return config.getBoolean("missions.items-remain-after-completion", true);
    }

    public FileConfiguration getRawConfig() {
        return config;
    }
}
