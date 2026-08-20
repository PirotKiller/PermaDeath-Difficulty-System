package me.pirot.permaDeathDifficultySystem.relics;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Registry and management for all relics. Provides creation methods and tracks granted relics.
 */
public class RelicManager {

    private final PermaDeathDifficultySystem plugin;
    private final Map<String, ItemStack> relicRegistry = new LinkedHashMap<>();

    public RelicManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
        registerRelics();
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

    private void register(String id, String tier, ItemStack item) {
        if (item != null && plugin.getConfigManager().isRelicEnabled(tier, id)) {
            relicRegistry.put(id, item);
        }
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
