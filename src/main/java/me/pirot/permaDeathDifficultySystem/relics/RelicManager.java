package me.pirot.permaDeathDifficultySystem.relics;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Registry and management for all relics.
 *
 * <p>Relics are identified by a persistent-data tag, never by display name. Matching on the
 * name would let any player rename a vanilla item in an anvil and inherit that relic's powers.
 */
public class RelicManager {

    /**
     * CustomModelData values the bundled resource pack keys its overrides off.
     * Numbered by tier (1xxx / 2xxx / 3xxx). Overridable per relic under
     * {@code relics.custom-model-data} in config.yml if they clash with another pack.
     */
    private static final Map<String, Integer> DEFAULT_MODEL_DATA = Map.ofEntries(
            Map.entry("ultra-shield", 1001),
            Map.entry("ultra-totem", 1002),
            Map.entry("dragon-slayer", 1003),
            Map.entry("mountain-devourer", 1004),
            Map.entry("elder-turtle-helmet", 1005),

            Map.entry("propulsion-trident", 2001),
            Map.entry("olympic-torch", 2002),
            Map.entry("lucky-clover", 2003),
            Map.entry("infernal-elytra", 2004),
            Map.entry("sacred-pearl", 2005),
            Map.entry("sacred-tree-apple", 2006),
            Map.entry("world-eater", 2007),
            Map.entry("blade-of-olympus", 2008),
            Map.entry("poseidons-trident", 2009),

            Map.entry("nautilus-helmet", 3001),
            Map.entry("hercules-chestplate", 3002),
            Map.entry("helmet-of-hades", 3003),
            Map.entry("aegis", 3004),
            Map.entry("hermes-boots", 3005),
            Map.entry("spartan-greaves", 3006),
            Map.entry("curse-remover", 3007),
            Map.entry("totem-of-true-god", 3008),
            Map.entry("onigari-no-ryuuou", 3009));

    private final PermaDeathDifficultySystem plugin;
    private final NamespacedKey relicIdKey;
    private final Map<String, ItemStack> relicRegistry = new LinkedHashMap<>();

    public RelicManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
        this.relicIdKey = new NamespacedKey(plugin, "relic_id");
        registerRelics();
    }

    /**
     * Returns the relic id stamped on this item, or {@code null} if it isn't a relic.
     */
    public String getRelicId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(relicIdKey, PersistentDataType.STRING);
    }

    /**
     * Checks whether the item is the specific relic identified by {@code id}.
     */
    public boolean isRelic(ItemStack item, String id) {
        return id.equals(getRelicId(item));
    }

    /**
     * Checks whether the player has the relic anywhere in their inventory, including armor.
     */
    public boolean hasRelic(Player player, String id) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isRelic(item, id)) return true;
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (isRelic(item, id)) return true;
        }
        return isRelic(player.getInventory().getItemInOffHand(), id);
    }

    private void registerRelics() {
        RelicDefinitions defs = new RelicDefinitions();

        // Tier 1
        register("ultra-shield", "tier-1", defs.createUltraShield());
        register("ultra-totem", "tier-1", defs.createUltraTotem());
        register("dragon-slayer", "tier-1", defs.createDragonSlayer());
        register("mountain-devourer", "tier-1", defs.createMountainDevourer());
        register("elder-turtle-helmet", "tier-1", defs.createElderTurtleHelmet());

        // Tier 2
        register("propulsion-trident", "tier-2", defs.createPropulsionTrident());
        register("olympic-torch", "tier-2", defs.createOlympicTorch());
        register("lucky-clover", "tier-2", defs.createLuckyClover());
        register("infernal-elytra", "tier-2", defs.createInfernalElytra());
        register("sacred-pearl", "tier-2", defs.createSacredPearl());
        register("sacred-tree-apple", "tier-2", defs.createSacredTreeApple());
        register("world-eater", "tier-2", defs.createWorldEater());
        register("blade-of-olympus", "tier-2", defs.createBladeOfOlympus());
        register("poseidons-trident", "tier-2", defs.createPoseidonsTrident());

        // Tier 3
        register("nautilus-helmet", "tier-3", defs.createNautilusHelmet());
        register("hercules-chestplate", "tier-3", defs.createHerculesChestplate());
        register("helmet-of-hades", "tier-3", defs.createHelmetOfHades());
        register("aegis", "tier-3", defs.createAegis());
        register("hermes-boots", "tier-3", defs.createHermesBoots());
        register("spartan-greaves", "tier-3", defs.createSpartanGreaves());
        register("curse-remover", "tier-3", defs.createCurseRemover());
        register("totem-of-true-god", "tier-3", defs.createTotemOfTrueGod());
        register("onigari-no-ryuuou", "tier-3", defs.createOnigariNoRyuuou());

        plugin.getLogger().info("Registered " + relicRegistry.size() + " relics.");
    }

    /**
     * Stamps the relic id into persistent data and adds it to the registry.
     * Tagging happens here so every relic is covered by construction, rather than relying on
     * each of the 23 factory methods to remember.
     */
    private void register(String id, String tier, ItemStack item) {
        if (item == null || !plugin.getConfigManager().isRelicEnabled(tier, id)) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(relicIdKey, PersistentDataType.STRING, id);

        // Drives the resource pack's model overrides. Identity still comes from the PDC tag —
        // CustomModelData is cosmetic only and must never be used to decide what an item is,
        // since a player can set it on any item with an anvil-crafted NBT copy.
        int modelData = plugin.getConfigManager()
                .getRelicModelData(id, DEFAULT_MODEL_DATA.getOrDefault(id, 0));
        if (modelData > 0) {
            meta.setCustomModelData(modelData);
        }
        item.setItemMeta(meta);

        relicRegistry.put(id, item);
    }

    /**
     * Gets a relic by its ID.
     */
    public ItemStack getRelic(String id) {
        ItemStack relic = relicRegistry.get(id);
        return relic != null ? relic.clone() : null;
    }

    /**
     * Gives a relic to a player.
     */
    public boolean giveRelic(Player player, String id) {
        ItemStack relic = getRelic(id);
        if (relic == null) return false;

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(relic);
        if (!overflow.isEmpty()) {
            for (ItemStack item : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        return true;
    }

    /**
     * Gets all registered relic IDs.
     */
    public Set<String> getRelicIds() {
        return relicRegistry.keySet();
    }

    /**
     * The CustomModelData stamped on a registered relic, or 0 if it has none.
     * Handy for lining the resource pack up against what the server actually issues.
     */
    public int getModelData(String id) {
        ItemStack relic = relicRegistry.get(id);
        if (relic == null || !relic.hasItemMeta()) return 0;
        ItemMeta meta = relic.getItemMeta();
        return meta.hasCustomModelData() ? meta.getCustomModelData() : 0;
    }

    /**
     * Gets the display name of a relic.
     */
    public String getRelicDisplayName(String id) {
        ItemStack relic = relicRegistry.get(id);
        if (relic != null && relic.hasItemMeta() && relic.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(relic.getItemMeta().displayName());
        }
        return id;
    }
}
