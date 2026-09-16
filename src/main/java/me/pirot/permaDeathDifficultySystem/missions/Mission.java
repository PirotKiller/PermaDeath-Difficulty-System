package me.pirot.permaDeathDifficultySystem.missions;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A single daily mission: an objective, a reward granted the moment it completes, and an
 * inventory-slot penalty applied if the day rolls over unfinished.
 *
 * <p>A kill objective may accept more than one entity type (e.g. Day 5 counts both cats and
 * wolves). That lives here rather than as day-number special cases in the listener.
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
    private final Set<EntityType> targetEntities;  // for KILL_ENTITY
    private final Material targetItem;             // for OBTAIN_ITEM / OBTAIN_SPECIFIC_ITEM
    private final int targetCount;
    private final ItemStack reward;
    private final String rewardDescription;
    private final int penaltySlot;                 // inventory slot to lock (-1 = none)
    private final String penaltyDescription;

    public Mission(int day, String name, ObjectiveType objectiveType,
                   EntityType targetEntity, Material targetItem, int targetCount,
                   ItemStack reward, String rewardDescription,
                   int penaltySlot, String penaltyDescription) {
        this(day, name, objectiveType,
                targetEntity == null ? Collections.emptySet() : EnumSet.of(targetEntity),
                targetItem, targetCount, reward, rewardDescription, penaltySlot, penaltyDescription);
    }

    /**
     * Private so callers passing a literal {@code null} entity aren't ambiguous against the
     * single-entity constructor. Multi-entity objectives go through {@link #kills}.
     */
    private Mission(int day, String name, ObjectiveType objectiveType,
                    Set<EntityType> targetEntities, Material targetItem, int targetCount,
                    ItemStack reward, String rewardDescription,
                    int penaltySlot, String penaltyDescription) {
        this.day = day;
        this.name = name;
        this.objectiveType = objectiveType;
        this.targetEntities = targetEntities == null ? Collections.emptySet() : targetEntities;
        this.targetItem = targetItem;
        this.targetCount = targetCount;
        this.reward = reward;
        this.rewardDescription = rewardDescription;
        this.penaltySlot = penaltySlot;
        this.penaltyDescription = penaltyDescription;
    }

    /** Convenience factory for kill objectives that accept several entity types. */
    public static Mission kills(int day, String name, int targetCount,
                                ItemStack reward, String rewardDescription,
                                int penaltySlot, String penaltyDescription,
                                EntityType... entities) {
        return new Mission(day, name, ObjectiveType.KILL_ENTITY,
                EnumSet.copyOf(Arrays.asList(entities)), null, targetCount,
                reward, rewardDescription, penaltySlot, penaltyDescription);
    }

    /** Whether killing this entity type counts toward the objective. */
    public boolean matchesEntity(EntityType type) {
        return targetEntities.contains(type);
    }

    public int getDay() { return day; }
    public String getName() { return name; }
    public ObjectiveType getObjectiveType() { return objectiveType; }
    public Set<EntityType> getTargetEntities() { return targetEntities; }
    public Material getTargetItem() { return targetItem; }
    public int getTargetCount() { return targetCount; }
    public ItemStack getReward() { return reward; }
    public String getRewardDescription() { return rewardDescription; }
    public int getPenaltySlot() { return penaltySlot; }
    public String getPenaltyDescription() { return penaltyDescription; }
}
