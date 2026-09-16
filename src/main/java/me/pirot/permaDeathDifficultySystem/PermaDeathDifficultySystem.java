package me.pirot.permaDeathDifficultySystem;

import me.pirot.permaDeathDifficultySystem.bosses.BossBarManager;
import me.pirot.permaDeathDifficultySystem.bosses.EnderDragonBoss;
import me.pirot.permaDeathDifficultySystem.bosses.TheCoreBoss;
import me.pirot.permaDeathDifficultySystem.commands.PDDSCommand;
import me.pirot.permaDeathDifficultySystem.commands.PDDSTabCompleter;
import me.pirot.permaDeathDifficultySystem.config.ConfigManager;
import me.pirot.permaDeathDifficultySystem.core.DayManager;
import me.pirot.permaDeathDifficultySystem.core.PeriodicEffectScheduler;
import me.pirot.permaDeathDifficultySystem.days.DayHandlerRegistry;
import me.pirot.permaDeathDifficultySystem.listeners.EnvironmentalEffectsListener;
import me.pirot.permaDeathDifficultySystem.listeners.MobSpawnListener;
import me.pirot.permaDeathDifficultySystem.missions.MissionListener;
import me.pirot.permaDeathDifficultySystem.missions.MissionManager;
import me.pirot.permaDeathDifficultySystem.missions.PenaltyManager;
import me.pirot.permaDeathDifficultySystem.mobs.AuraEffectListener;
import me.pirot.permaDeathDifficultySystem.mobs.MobAIManager;
import me.pirot.permaDeathDifficultySystem.mobs.MobAttributeManager;
import me.pirot.permaDeathDifficultySystem.mobs.SpecialMobManager;
import me.pirot.permaDeathDifficultySystem.mobs.SpawnerListener;
import me.pirot.permaDeathDifficultySystem.relics.RelicAbilityListener;
import me.pirot.permaDeathDifficultySystem.relics.RelicManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PermaDeath Difficulty System — Main Plugin Class
 *
 * A progressive 35-day hardcore difficulty system for Minecraft Java 1.19.2.
 * Features: day progression, mob upgrades, environmental hazards, missions,
 * rewards/penalties, relics, custom AI, and boss encounters.
 */
public final class PermaDeathDifficultySystem extends JavaPlugin {

    private static PermaDeathDifficultySystem instance;

    // Core
    private ConfigManager configManager;
    private DayManager dayManager;
    private PeriodicEffectScheduler periodicEffectScheduler;
    private me.pirot.permaDeathDifficultySystem.core.BlockIndex blockIndex;
    private EnvironmentalEffectsListener environmentalEffectsListener;

    // Days
    private DayHandlerRegistry dayHandlerRegistry;

    // Missions
    private MissionManager missionManager;
    private PenaltyManager penaltyManager;

    // Mobs
    private MobAttributeManager mobAttributeManager;
    private SpecialMobManager specialMobManager;
    private MobAIManager mobAIManager;
    private AuraEffectListener auraEffectListener;

    // Relics
    private RelicManager relicManager;

    // Bosses
    private BossBarManager bossBarManager;
    private EnderDragonBoss enderDragonBoss;
    private TheCoreBoss theCoreBoss;

    // Scoreboard
    private me.pirot.permaDeathDifficultySystem.scoreboard.ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("==============================================");
        getLogger().info("  PermaDeath Difficulty System v" + getDescription().getVersion());
        getLogger().info("  Initializing 35-day hardcore challenge...");
        getLogger().info("==============================================");

        // Phase 1: Core Framework
        configManager = new ConfigManager(this);
        dayManager = new DayManager(this);
        periodicEffectScheduler = new PeriodicEffectScheduler(this);

        blockIndex = new me.pirot.permaDeathDifficultySystem.core.BlockIndex(this);
        Bukkit.getPluginManager().registerEvents(blockIndex, this);

        // Phase 3: Mission System (initialized before days so day handlers can reference it)
        penaltyManager = new PenaltyManager(this);
        missionManager = new MissionManager(this, penaltyManager);

        // Phase 2: Daily Difficulty Progression
        dayHandlerRegistry = new DayHandlerRegistry(this);
        dayHandlerRegistry.registerAll();

        // Register environmental and mob spawn listeners
        environmentalEffectsListener = new EnvironmentalEffectsListener(this);
        Bukkit.getPluginManager().registerEvents(environmentalEffectsListener, this);
        environmentalEffectsListener.startTasks();

        MobSpawnListener mobSpawnListener = new MobSpawnListener(this);
        Bukkit.getPluginManager().registerEvents(mobSpawnListener, this);

        // Nether gated until Day 8, End gated until Day 21.
        Bukkit.getPluginManager().registerEvents(
                new me.pirot.permaDeathDifficultySystem.listeners.DimensionGateListener(this), this);

        // Phase 3: Mission Listeners
        Bukkit.getPluginManager().registerEvents(missionManager, this);
        Bukkit.getPluginManager().registerEvents(penaltyManager, this);
        MissionListener missionListener = new MissionListener(this, missionManager);
        Bukkit.getPluginManager().registerEvents(missionListener, this);

        // Phase 4: Mob Upgrades & Custom AI
        mobAttributeManager = new MobAttributeManager(this);

        specialMobManager = new SpecialMobManager(this);
        Bukkit.getPluginManager().registerEvents(specialMobManager, this);
        specialMobManager.startTasks();

        mobAIManager = new MobAIManager(this, mobAttributeManager);
        Bukkit.getPluginManager().registerEvents(mobAIManager, this);
        mobAIManager.startTasks();

        auraEffectListener = new AuraEffectListener(this);
        auraEffectListener.startTask();

        SpawnerListener spawnerListener = new SpawnerListener(this);
        Bukkit.getPluginManager().registerEvents(spawnerListener, this);

        // Phase 5: Relic System
        relicManager = new RelicManager(this);

        RelicAbilityListener relicAbilityListener = new RelicAbilityListener(this);
        Bukkit.getPluginManager().registerEvents(relicAbilityListener, this);
        relicAbilityListener.startTasks();

        // Phase 6: Boss Mechanics
        bossBarManager = new BossBarManager(this);

        enderDragonBoss = new EnderDragonBoss(this, bossBarManager);
        Bukkit.getPluginManager().registerEvents(enderDragonBoss, this);

        theCoreBoss = new TheCoreBoss(this, bossBarManager);
        Bukkit.getPluginManager().registerEvents(theCoreBoss, this);

        // Scoreboard
        scoreboardManager = new me.pirot.permaDeathDifficultySystem.scoreboard.ScoreboardManager(this);
        scoreboardManager.start();

        // Phase 7: Commands
        PDDSCommand pddsCommand = new PDDSCommand(this);
        PDDSTabCompleter tabCompleter = new PDDSTabCompleter(this);
        getCommand("pdds").setExecutor(pddsCommand);
        getCommand("pdds").setTabCompleter(tabCompleter);

        // Start timers
        dayManager.startTimer();
        periodicEffectScheduler.start();

        getLogger().info("==============================================");
        getLogger().info("  All systems initialized successfully!");
        getLogger().info("  Current day: " + dayManager.getCurrentDay() + "/35");
        getLogger().info("==============================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("PermaDeath Difficulty System shutting down...");

        // Stop timers. Cancel everything this plugin scheduled so a /reload doesn't
        // leave orphaned tasks running against a dead plugin instance.
        if (dayManager != null) dayManager.stopTimer();
        if (periodicEffectScheduler != null) periodicEffectScheduler.stop();
        if (scoreboardManager != null) scoreboardManager.stop();
        if (environmentalEffectsListener != null) environmentalEffectsListener.stopTasks();
        if (theCoreBoss != null) theCoreBoss.shutdown();
        if (enderDragonBoss != null) enderDragonBoss.shutdown();
        Bukkit.getScheduler().cancelTasks(this);

        // Save data
        if (missionManager != null) missionManager.saveData();
        if (penaltyManager != null) penaltyManager.saveData();

        // Remove boss bars
        if (bossBarManager != null) bossBarManager.removeAll();

        getLogger().info("PermaDeath Difficulty System disabled.");
    }

    // ==================== Accessors ====================

    public static PermaDeathDifficultySystem getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DayManager getDayManager() {
        return dayManager;
    }

    public me.pirot.permaDeathDifficultySystem.core.BlockIndex getBlockIndex() {
        return blockIndex;
    }

    public MissionManager getMissionManager() {
        return missionManager;
    }

    public RelicManager getRelicManager() {
        return relicManager;
    }

    public MobAttributeManager getMobAttributeManager() {
        return mobAttributeManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public EnderDragonBoss getEnderDragonBoss() {
        return enderDragonBoss;
    }

    public TheCoreBoss getTheCoreBoss() {
        return theCoreBoss;
    }

    public me.pirot.permaDeathDifficultySystem.scoreboard.ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
