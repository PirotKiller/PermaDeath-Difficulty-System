package me.pirot.permaDeathDifficultySystem.missions;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/**
 * Data class representing a daily mission.
 */
public class Mission {

    public enum ObjectiveType {
        KILL_ENTITY,
        OBTAIN_ITEM,
        OBTAIN_SPECIFIC_ITEM,
        COMPLETE_RAID
    }

    private final int day;
    private final String name;
    private final ObjectiveType objectiveType;
    private final EntityType targetEntity;     // For KILL_ENTITY
    private final Material targetItem;         // For OBTAIN_ITEM / OBTAIN_SPECIFIC_ITEM
    private final int targetCount;
    private final ItemStack reward;
    private final String rewardDescription;
    private final int penaltySlot;             // Inventory slot to lock (-1 = no slot penalty)
    private final String penaltyDescription;

    public Mission(int day, String name, ObjectiveType objectiveType,
                   EntityType targetEntity, Material targetItem, int targetCount,
                   ItemStack reward, String rewardDescription,
                   int penaltySlot, String penaltyDescription) {
        this.day = day;
        this.name = name;
        this.objectiveType = objectiveType;
        this.targetEntity = targetEntity;
        this.targetItem = targetItem;
        this.targetCount = targetCount;
        this.reward = reward;
        this.rewardDescription = rewardDescription;
        this.penaltySlot = penaltySlot;
        this.penaltyDescription = penaltyDescription;
    }

    public int getDay() { return day; }
    public String getName() { return name; }
    public ObjectiveType getObjectiveType() { return objectiveType; }
    public EntityType getTargetEntity() { return targetEntity; }
    public Material getTargetItem() { return targetItem; }
    public int getTargetCount() { return targetCount; }
    public ItemStack getReward() { return reward; }
    public String getRewardDescription() { return rewardDescription; }
    public int getPenaltySlot() { return penaltySlot; }
    public String getPenaltyDescription() { return penaltyDescription; }
}
