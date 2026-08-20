package me.pirot.permaDeathDifficultySystem.relics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Defines all 23 relics across 3 tiers.
 * All relics have Curse of Vanishing.
 */
public class RelicDefinitions {

    // ==================== TIER 1 ====================

    public ItemStack createUltraShield() {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Ultra Shield", NamedTextColor.AQUA, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 1 Relic", "Grants Resistance V", "Unbreaking enchanted"));
        meta.addEnchant(Enchantment.DURABILITY, 10, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createUltraTotem() {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Ultra Totem", NamedTextColor.GOLD, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 1 Relic", "+5 Hearts", "Enhanced Totem of Undying"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH,
                new AttributeModifier(UUID.randomUUID(), "pdds_ultra_totem", 10.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.OFF_HAND));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createDragonSlayer() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Dragon Slayer", NamedTextColor.RED, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 1 Relic", "Sharpness VI Diamond Sword", "Forged to slay dragons"));
        meta.addEnchant(Enchantment.DAMAGE_ALL, 6, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createMountainDevourer() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Mountain Devourer", NamedTextColor.GREEN, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 1 Relic", "Efficiency VI Diamond Pickaxe", "Devours mountains whole"));
        meta.addEnchant(Enchantment.DIG_SPEED, 6, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createElderTurtleHelmet() {
        ItemStack item = new ItemStack(Material.TURTLE_HELMET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Elder Turtle Helmet", NamedTextColor.DARK_GREEN, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 1 Relic", "Respiration IV", "Blessed by the ancient sea turtles"));
        meta.addEnchant(Enchantment.OXYGEN, 4, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== TIER 2 ====================

    public ItemStack createPropulsionTrident() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Propulsion Trident", NamedTextColor.BLUE, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "Riptide III + Unbreaking", "Speed II when held"));
        meta.addEnchant(Enchantment.RIPTIDE, 3, true);
        meta.addEnchant(Enchantment.DURABILITY, 10, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED,
                new AttributeModifier(UUID.randomUUID(), "pdds_propulsion", 0.04,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createOlympicTorch() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Olympic Torch", NamedTextColor.YELLOW, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "Speed VIII when held", "Fire Resistance", "Cannot be placed", "Immune to torch damage"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED,
                new AttributeModifier(UUID.randomUUID(), "pdds_olympic_torch", 0.16,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        // Mark as Olympic Torch via persistent data
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createLuckyClover() {
        ItemStack item = new ItemStack(Material.FERN);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Lucky Clover", NamedTextColor.GREEN, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "+100 Luck", "Prevents Totem failure"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addAttributeModifier(Attribute.GENERIC_LUCK,
                new AttributeModifier(UUID.randomUUID(), "pdds_lucky_clover", 100.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createInfernalElytra() {
        ItemStack item = new ItemStack(Material.ELYTRA);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Infernal Elytra", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "Increased flight speed", "Unbreakable", "Lightning immunity"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSacredPearl() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL, 16);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Sacred Pearl", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "Unlimited teleportation", "No teleport damage"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSacredTreeApple() {
        ItemStack item = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Sacred Tree Apple", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "+10 Golden Hearts", "Resistance X + Fire Resistance",
                "Strength X + Speed III + Night Vision", "Removes negative effects for 30 min"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createWorldEater() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("World Eater", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "Fortune V + Efficiency VIII", "Unbreakable Netherite Pickaxe"));
        meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 5, true);
        meta.addEnchant(Enchantment.DIG_SPEED, 8, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBladeOfOlympus() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blade of Olympus", NamedTextColor.GOLD, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "Sharpness VIII", "Unbreakable Netherite Sword", "No attack cooldown"));
        meta.addEnchant(Enchantment.DAMAGE_ALL, 8, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        // No attack cooldown via attribute
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
                new AttributeModifier(UUID.randomUUID(), "pdds_blade_olympus", 100.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
                new AttributeModifier(UUID.randomUUID(), "pdds_blade_dmg", 20.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createPoseidonsTrident() {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Poseidon's Trident", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 2 Relic", "Loyalty III + Channeling X", "Multiple lightning strikes",
                "Lightning damage within 10 blocks", "Unbreakable"));
        meta.addEnchant(Enchantment.LOYALTY, 3, true);
        meta.addEnchant(Enchantment.CHANNELING, 1, true); // Channeling doesn't go above 1, custom handled
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== TIER 3 ====================

    public ItemStack createNautilusHelmet() {
        ItemStack item = new ItemStack(Material.DIAMOND_HELMET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Nautilus Helmet", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "Water Breathing + Invisibility", "Dolphin's Grace + Resistance X"));
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
        meta.addEnchant(Enchantment.OXYGEN, 10, true);
        meta.addEnchant(Enchantment.WATER_WORKER, 1, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createHerculesChestplate() {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Hercules Chestplate", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "Strength V + Resistance XXX", "100-damage Thorns",
                "Unbreakable + Advanced enchantments"));
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 10, true);
        meta.addEnchant(Enchantment.THORNS, 10, true); // Custom high thorns
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR,
                new AttributeModifier(UUID.randomUUID(), "pdds_hercules_armor", 20.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS,
                new AttributeModifier(UUID.randomUUID(), "pdds_hercules_tough", 10.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createHelmetOfHades() {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Helmet of Hades", NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "Invisibility + Speed III + Resistance V",
                "+10 Hearts + Unbreakable", "Advanced helmet enchantments"));
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 10, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH,
                new AttributeModifier(UUID.randomUUID(), "pdds_hades_health", 20.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED,
                new AttributeModifier(UUID.randomUUID(), "pdds_hades_speed", 0.06,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR,
                new AttributeModifier(UUID.randomUUID(), "pdds_hades_armor", 10.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createAegis() {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Aegis", NamedTextColor.GOLD, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "Unbreakable Shield", "+50 Resistance + +10 Hearts"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH,
                new AttributeModifier(UUID.randomUUID(), "pdds_aegis_health", 20.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.OFF_HAND));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR,
                new AttributeModifier(UUID.randomUUID(), "pdds_aegis_armor", 50.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.OFF_HAND));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createHermesBoots() {
        ItemStack item = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Hermes Boots", NamedTextColor.AQUA, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "Speed VII", "Unbreakable + Advanced boot enchantments"));
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 10, true);
        meta.addEnchant(Enchantment.PROTECTION_FALL, 10, true);
        meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
        meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED,
                new AttributeModifier(UUID.randomUUID(), "pdds_hermes_speed", 0.14,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.FEET));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR,
                new AttributeModifier(UUID.randomUUID(), "pdds_hermes_armor", 8.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.FEET));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSpartanGreaves() {
        ItemStack item = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Spartan Greaves", NamedTextColor.RED, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "Resistance VIII + +5 Hearts", "Unbreakable"));
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 10, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH,
                new AttributeModifier(UUID.randomUUID(), "pdds_spartan_health", 10.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS));
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR,
                new AttributeModifier(UUID.randomUUID(), "pdds_spartan_armor", 20.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.LEGS));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createCurseRemover() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Curse Remover", NamedTextColor.WHITE, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "Immunity to negative effects",
                "Removes inventory penalties", "Prevents Totem failure",
                "§cBreaks if player uses a Totem or dies"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createTotemOfTrueGod() {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Totem of the True God", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "Infinite Totem uses", "Prevents Totem failure",
                "+10 Hearts + Regeneration III", "Main-hand/hotbar use"));
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH,
                new AttributeModifier(UUID.randomUUID(), "pdds_true_god_health", 20.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createOnigariNoRyuuou() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Onigari-no-Ryuuou", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        meta.lore(buildLore("Tier 3 Relic", "1,000 Damage", "No attack cooldown",
                "Resistance XX + Fire Resistance", "Negative effect immunity", "Unbreakable"));
        meta.addEnchant(Enchantment.DAMAGE_ALL, 10, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 5, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        // 1000 damage + no attack cooldown
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
                new AttributeModifier(UUID.randomUUID(), "pdds_onigari_dmg", 1000.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
                new AttributeModifier(UUID.randomUUID(), "pdds_onigari_speed", 100.0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== Helper ====================

    private List<Component> buildLore(String... lines) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        for (String line : lines) {
            NamedTextColor color = line.startsWith("§c") ? NamedTextColor.RED : NamedTextColor.GRAY;
            String cleanLine = line.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "");
            if (line.startsWith("Tier")) {
                lore.add(Component.text(cleanLine, NamedTextColor.DARK_PURPLE, TextDecoration.ITALIC));
            } else if (line.startsWith("§c")) {
                lore.add(Component.text(cleanLine, NamedTextColor.RED));
            } else {
                lore.add(Component.text("• " + cleanLine, NamedTextColor.GRAY));
            }
        }
        lore.add(Component.text(""));
        lore.add(Component.text("☠ Curse of Vanishing", NamedTextColor.DARK_RED, TextDecoration.ITALIC));
        return lore;
    }
}
