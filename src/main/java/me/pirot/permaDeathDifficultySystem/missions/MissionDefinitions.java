package me.pirot.permaDeathDifficultySystem.missions;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;

/**
 * Static definitions for all 34 daily missions (Days 1–34).
 * Day 35 is the final boss — no standard mission.
 */
public class MissionDefinitions {

    private static final Map<Integer, Mission> MISSIONS = new HashMap<>();

    static {
        // ==================== Days 1–6 ====================
        MISSIONS.put(1, new Mission(1, "Find Diamond",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.DIAMOND, 1,
                createReward(Material.DIAMOND_SWORD, 1, "§bDiamond Sword"),
                "Diamond Sword", 36, "Armor slot locked")); // 36 = boots slot

        MISSIONS.put(2, new Mission(2, "Kill Zombies",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.ZOMBIE, null, 20,
                createReward(Material.IRON_INGOT, 16, "§fIron Ingots"),
                "16 Iron Ingots", 37, "Leggings slot locked")); // 37 = leggings

        MISSIONS.put(3, new Mission(3, "Obtain Phantom Membranes",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.PHANTOM_MEMBRANE, 4,
                createReward(Material.GOLDEN_APPLE, 4, "§6Golden Apples"),
                "4 Golden Apples", 38, "Chestplate slot locked")); // 38 = chestplate

        MISSIONS.put(4, new Mission(4, "Obtain Spider Eyes",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.SPIDER_EYE, 8,
                createReward(Material.ENDER_PEARL, 8, "§dEnder Pearls"),
                "8 Ender Pearls", 39, "Helmet slot locked")); // 39 = helmet

        MISSIONS.put(5, Mission.kills(5, "Kill Cats and Wolves", 10,
                createReward(Material.EXPERIENCE_BOTTLE, 16, "§aBottles o' Enchanting"),
                "16 Bottles o' Enchanting", 40, "Off-hand slot locked", // 40 = offhand
                EntityType.CAT, EntityType.WOLF));

        MISSIONS.put(6, new Mission(6, "Kill Endermen",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.ENDERMAN, null, 10,
                createReward(Material.DIAMOND, 8, "§bDiamonds"),
                "8 Diamonds", 0, "Hotbar slot 1 locked")); // 0 = hotbar slot 0

        // ==================== Days 7–12 ====================
        MISSIONS.put(7, Mission.kills(7, "Kill Zombie Villagers and Witches", 10,
                createReward(Material.ENCHANTED_GOLDEN_APPLE, 2, "§6Enchanted Golden Apples"),
                "2 Enchanted Golden Apples", 1, "Hotbar slot 2 locked",
                EntityType.ZOMBIE_VILLAGER, EntityType.WITCH));

        MISSIONS.put(8, new Mission(8, "Kill a Giant Slime",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.SLIME, null, 1,
                createReward(Material.SLIME_BLOCK, 32, "§aSlime Blocks"),
                "32 Slime Blocks", 2, "Hotbar slot 3 locked"));

        MISSIONS.put(9, new Mission(9, "Complete a Level 5 Raid",
                Mission.ObjectiveType.COMPLETE_RAID, null, null, 1,
                createReward(Material.TOTEM_OF_UNDYING, 1, "§6Totem of Undying"),
                "Totem of Undying", 3, "Hotbar slot 4 locked"));

        MISSIONS.put(10, new Mission(10, "Kill Blazes",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.BLAZE, null, 15,
                createReward(Material.BLAZE_ROD, 16, "§6Blaze Rods"),
                "16 Blaze Rods", 4, "Hotbar slot 5 locked"));

        MISSIONS.put(11, new Mission(11, "Obtain a Creeper Head",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.CREEPER_HEAD, 1,
                createReward(Material.TNT, 32, "§cTNT"),
                "32 TNT", 5, "Hotbar slot 6 locked"));

        MISSIONS.put(12, new Mission(12, "Obtain a Rabbit's Foot",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.RABBIT_FOOT, 1,
                createReward(Material.GOLDEN_CARROT, 32, "§6Golden Carrots"),
                "32 Golden Carrots", 6, "Hotbar slot 7 locked"));

        // ==================== Days 13–18 ====================
        MISSIONS.put(13, new Mission(13, "Obtain Sponges",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.SPONGE, 4,
                createReward(Material.TRIDENT, 1, "§bTrident"),
                "Trident", 7, "Hotbar slot 8 locked"));

        MISSIONS.put(14, new Mission(14, "Kill a Giant Magma Cube",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.MAGMA_CUBE, null, 1,
                createReward(Material.NETHERITE_INGOT, 2, "§dNetherite Ingots"),
                "2 Netherite Ingots", 8, "Hotbar slot 9 locked"));

        MISSIONS.put(15, new Mission(15, "Obtain an Axolotl in a Bucket",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.AXOLOTL_BUCKET, 1,
                createReward(Material.HEART_OF_THE_SEA, 1, "§bHeart of the Sea"),
                "Heart of the Sea", 9, "Inventory row 2 slot 1 locked"));

        MISSIONS.put(16, new Mission(16, "Obtain a Conduit",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.CONDUIT, 1,
                createReward(Material.DIAMOND_BLOCK, 4, "§bDiamond Blocks"),
                "4 Diamond Blocks", 10, "Inventory row 2 slot 2 locked"));

        MISSIONS.put(17, new Mission(17, "Obtain Sculk Catalysts",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.SCULK_CATALYST, 2,
                createReward(Material.NETHERITE_BLOCK, 1, "§dNetherite Block"),
                "Netherite Block", 11, "Inventory row 2 slot 3 locked"));

        MISSIONS.put(18, new Mission(18, "Obtain a Netherite Block",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.NETHERITE_BLOCK, 1,
                createReward(Material.ENCHANTED_GOLDEN_APPLE, 4, "§6Enchanted Golden Apples"),
                "4 Enchanted Golden Apples", 12, "Inventory row 2 slot 4 locked"));

        // ==================== Days 19–24 ====================
        MISSIONS.put(19, new Mission(19, "Obtain Diamond Blocks",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.DIAMOND_BLOCK, 4,
                createReward(Material.NETHERITE_INGOT, 4, "§dNetherite Ingots"),
                "4 Netherite Ingots", 13, "Inventory row 2 slot 5 locked"));

        MISSIONS.put(20, new Mission(20, "Collect Required Heads",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.SKELETON_SKULL, 3,
                createReward(Material.NETHERITE_SWORD, 1, "§dNetherite Sword"),
                "Netherite Sword", 14, "Inventory row 2 slot 6 locked"));

        MISSIONS.put(21, new Mission(21, "Obtain Giant Zombie Block",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.GIANT, null, 1,
                createReward(Material.NETHER_STAR, 1, "§eNether Star"),
                "Nether Star", 15, "Inventory row 2 slot 7 locked"));

        MISSIONS.put(22, new Mission(22, "Obtain Shulker Shells",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.SHULKER_SHELL, 4,
                createReward(Material.SHULKER_BOX, 1, "§dShulker Box"),
                "Shulker Box", 16, "Inventory row 2 slot 8 locked"));

        MISSIONS.put(23, new Mission(23, "Kill Ender Ghasts",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.GHAST, null, 5,
                createReward(Material.ENCHANTED_GOLDEN_APPLE, 8, "§6Enchanted Golden Apples"),
                "8 Enchanted Golden Apples", 17, "Inventory row 2 slot 9 locked"));

        MISSIONS.put(24, new Mission(24, "Kill Ravagers",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.RAVAGER, null, 5,
                createReward(Material.NETHERITE_BLOCK, 2, "§dNetherite Blocks"),
                "2 Netherite Blocks", 18, "Inventory row 3 slot 1 locked"));

        // ==================== Days 25–30 ====================
        MISSIONS.put(25, new Mission(25, "Obtain Wither Roses",
                Mission.ObjectiveType.OBTAIN_ITEM, null, Material.WITHER_ROSE, 8,
                createReward(Material.BEACON, 1, "§bBeacon"),
                "Beacon", 19, "Inventory row 3 slot 2 locked"));

        MISSIONS.put(26, new Mission(26, "Kill Spiders",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.SPIDER, null, 30,
                createReward(Material.DIAMOND_BLOCK, 8, "§bDiamond Blocks"),
                "8 Diamond Blocks", 20, "Inventory row 3 slot 3 locked"));

        MISSIONS.put(27, new Mission(27, "Kill Skeletons",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.SKELETON, null, 30,
                createReward(Material.NETHERITE_BLOCK, 4, "§dNetherite Blocks"),
                "4 Netherite Blocks", 21, "Inventory row 3 slot 4 locked"));

        MISSIONS.put(28, new Mission(28, "Kill Endermites",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.ENDERMITE, null, 5,
                createReward(Material.ENCHANTED_GOLDEN_APPLE, 16, "§6Enchanted Golden Apples"),
                "16 Enchanted Golden Apples", 22, "Inventory row 3 slot 5 locked"));

        MISSIONS.put(29, new Mission(29, "Kill Cave Spiders",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.CAVE_SPIDER, null, 20,
                createReward(Material.NETHERITE_INGOT, 8, "§dNetherite Ingots"),
                "8 Netherite Ingots", 23, "Inventory row 3 slot 6 locked"));

        MISSIONS.put(30, new Mission(30, "Kill 100 Endermen",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.ENDERMAN, null, 100,
                createReward(Material.NETHER_STAR, 2, "§eNether Stars"),
                "2 Nether Stars", 24, "Inventory row 3 slot 7 locked"));

        // ==================== Days 31–34 ====================
        MISSIONS.put(31, new Mission(31, "Kill Withers",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.WITHER, null, 3,
                createReward(Material.NETHER_STAR, 4, "§eNether Stars"),
                "4 Nether Stars", 25, "Inventory row 3 slot 8 locked"));

        MISSIONS.put(32, new Mission(32, "Kill Ender Quantum Creepers",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.CREEPER, null, 10,
                createReward(Material.NETHERITE_BLOCK, 8, "§dNetherite Blocks"),
                "8 Netherite Blocks", 26, "Inventory row 3 slot 9 locked"));

        MISSIONS.put(33, new Mission(33, "Kill Phantoms",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.PHANTOM, null, 20,
                createReward(Material.ENCHANTED_GOLDEN_APPLE, 32, "§6Enchanted Golden Apples"),
                "32 Enchanted Golden Apples", 27, "Inventory row 4 slot 1 locked"));

        MISSIONS.put(34, new Mission(34, "Kill Wardens",
                Mission.ObjectiveType.KILL_ENTITY, EntityType.WARDEN, null, 2,
                createReward(Material.NETHER_STAR, 8, "§eNether Stars"),
                "8 Nether Stars", 28, "Inventory row 4 slot 2 locked"));
    }

    public static Mission getMission(int day) {
        return MISSIONS.get(day);
    }

    public static Map<Integer, Mission> getAllMissions() {
        return MISSIONS;
    }

    private static ItemStack createReward(Material material, int amount, String displayName) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName + " §7(Mission Reward)");
            item.setItemMeta(meta);
        }
        return item;
    }
}
