package me.pirot.permaDeathDifficultySystem.missions;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages inventory-slot penalties. Locked slots prevent players from placing,
 * taking, dropping, or swapping items in those slots, displaying a red "🔒 LOCKED SLOT" barrier item.
 *
 * Items are NEVER lost or deleted when a slot is locked: any real item present is automatically
 * moved to an open inventory slot (or dropped at feet if inventory is full).
 */
public class PenaltyManager implements Listener {

    private final PermaDeathDifficultySystem plugin;
    private final NamespacedKey barrierKey;

    // Player UUID -> Set of locked slot indices
    private final Map<UUID, Set<Integer>> playerPenalties = new HashMap<>();

    private File dataFile;
    private FileConfiguration data;

    public PenaltyManager(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
        this.barrierKey = new NamespacedKey(plugin, "locked_slot_barrier");
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "penalty_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create penalty_data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        if (data.contains("penalties")) {
            for (String uuidStr : data.getConfigurationSection("penalties").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Set<Integer> slots = new HashSet<>(data.getIntegerList("penalties." + uuidStr));
                playerPenalties.put(uuid, slots);
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Set<Integer>> entry : playerPenalties.entrySet()) {
            data.set("penalties." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save penalty_data.yml: " + e.getMessage());
        }
    }

    /**
     * Creates the visual indicator item for locked slots.
     */
    public ItemStack createBarrierItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("🔒 LOCKED SLOT 🔒", NamedTextColor.RED, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("This slot is locked as a penalty", NamedTextColor.GRAY));
            lore.add(Component.text("for a missed daily mission.", NamedTextColor.GRAY));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(barrierKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Checks if an item is a locked slot barrier indicator.
     */
    public boolean isBarrierItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(barrierKey, PersistentDataType.BYTE);
    }

    /**
     * Ensures a specific locked slot has a barrier item, relocating any real item in that slot
     * to a free inventory slot so items are NEVER lost or deleted.
     */
    private void ensureSlotLocked(Player player, int slot) {
        ItemStack current = player.getInventory().getItem(slot);
        if (current != null && current.getType() != Material.AIR && !isBarrierItem(current)) {
            // Real item found in a locked slot! Clear locked slot first
            player.getInventory().setItem(slot, null);

            // Find an open, non-locked slot in player inventory (0-35)
            int freeSlot = -1;
            Set<Integer> lockedSlots = getLockedSlots(player.getUniqueId());
            for (int i = 0; i < 36; i++) {
                if (!lockedSlots.contains(i)) {
                    ItemStack invItem = player.getInventory().getItem(i);
                    if (invItem == null || invItem.getType() == Material.AIR) {
                        freeSlot = i;
                        break;
                    }
                }
            }

            if (freeSlot != -1) {
                player.getInventory().setItem(freeSlot, current);
                player.sendMessage(Component.text("§eItem relocated to an open inventory slot because slot " + slot + " is locked."));
            } else {
                // Inventory full, drop at feet safely
                player.getWorld().dropItemNaturally(player.getLocation(), current);
                player.sendMessage(Component.text("§cInventory full! Item dropped at your feet because slot " + slot + " is locked."));
            }
        }
        // Set barrier item in locked slot
        player.getInventory().setItem(slot, createBarrierItem());
    }

    /**
     * Refreshes a player's inventory to ensure all locked slots contain barrier items,
     * and non-locked slots do not contain barrier items.
     */
    public void refreshInventory(Player player) {
        Set<Integer> lockedSlots = getLockedSlots(player.getUniqueId());

        // Ensure all locked slots have barriers and real items are relocated safely
        for (int slot : lockedSlots) {
            ensureSlotLocked(player, slot);
        }

        // Clean up any stray barrier items in non-locked slots
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (!lockedSlots.contains(slot)) {
                ItemStack item = player.getInventory().getItem(slot);
                if (isBarrierItem(item)) {
                    player.getInventory().setItem(slot, null);
                }
            }
        }
        player.updateInventory();
    }

    /**
     * Adds a penalty (locks a slot) for a player.
     */
    public void addPenalty(UUID playerUUID, int slot) {
        Set<Integer> penalties = playerPenalties.computeIfAbsent(playerUUID, k -> new HashSet<>());
        penalties.add(slot);
        saveData();

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            refreshInventory(player);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 0.8f);
        }

        plugin.getLogger().info("Added penalty slot " + slot + " for player " + playerUUID);
    }

    /**
     * Removes a penalty (unlocks a slot) for a player.
     */
    public void removePenalty(UUID playerUUID, int slot) {
        Set<Integer> penalties = playerPenalties.get(playerUUID);
        if (penalties != null) {
            penalties.remove(slot);
            if (penalties.isEmpty()) {
                playerPenalties.remove(playerUUID);
            }
            saveData();
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isBarrierItem(item)) {
                player.getInventory().setItem(slot, null);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
            player.updateInventory();
        }

        plugin.getLogger().info("Removed penalty slot " + slot + " for player " + playerUUID);
    }

    /**
     * Removes all penalties for a player.
     */
    public void removeAllPenalties(UUID playerUUID) {
        Set<Integer> locked = new HashSet<>(getLockedSlots(playerUUID));
        playerPenalties.remove(playerUUID);
        saveData();

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            for (int slot : locked) {
                ItemStack item = player.getInventory().getItem(slot);
                if (isBarrierItem(item)) {
                    player.getInventory().setItem(slot, null);
                }
            }
            player.updateInventory();
        }
    }

    /**
     * Checks if a slot is locked for a player.
     */
    public boolean isSlotLocked(UUID playerUUID, int slot) {
        Set<Integer> penalties = playerPenalties.get(playerUUID);
        return penalties != null && penalties.contains(slot);
    }

    /**
     * Gets all locked slots for a player.
     */
    public Set<Integer> getLockedSlots(UUID playerUUID) {
        return playerPenalties.getOrDefault(playerUUID, Collections.emptySet());
    }

    /**
     * Prevents players from placing, taking, or swapping items into locked slots.
     * Cancels the click and syncs the client so items remain in cursor or original slot without disappearing.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Set<Integer> lockedSlots = getLockedSlots(player.getUniqueId());
        if (lockedSlots.isEmpty()) return;

        InventoryView view = event.getView();
        int rawSlot = event.getRawSlot();
        ClickType click = event.getClick();

        boolean cancel = false;

        // 1. Direct click on player inventory slot
        if (rawSlot >= 0) {
            int topSize = view.getTopInventory().getSize();
            if (rawSlot >= topSize) {
                int playerSlot = view.convertSlot(rawSlot);
                if (lockedSlots.contains(playerSlot)) {
                    cancel = true;
                }
            }
        }

        // 2. Click involving barrier item
        if (isBarrierItem(event.getCurrentItem()) || isBarrierItem(event.getCursor())) {
            cancel = true;
        }

        // 3. Number Key swap (Hotbar 0-8)
        if (click == ClickType.NUMBER_KEY) {
            int hotbarButton = event.getHotbarButton(); // 0-8
            if (hotbarButton >= 0 && lockedSlots.contains(hotbarButton)) {
                cancel = true;
            }
        }

        // 4. Swap Offhand key
        if (click == ClickType.SWAP_OFFHAND) {
            int mainhandSlot = player.getInventory().getHeldItemSlot();
            if (lockedSlots.contains(40) || lockedSlots.contains(mainhandSlot)) {
                cancel = true;
            }
        }

        // 5. Shift Click transfer
        if (event.isShiftClick()) {
            if (rawSlot >= 0 && rawSlot < view.getTopInventory().getSize()) {
                // Shift click from container to player inventory
                for (int locked : lockedSlots) {
                    ItemStack targetItem = player.getInventory().getItem(locked);
                    if (targetItem == null || targetItem.getType() == Material.AIR || isBarrierItem(targetItem)) {
                        cancel = true;
                        break;
                    }
                }
            }
        }

        // 6. Double Click item collection
        if (click == ClickType.DOUBLE_CLICK) {
            if (isBarrierItem(event.getCursor())) {
                cancel = true;
            }
        }

        // 7. Drop key press (Q / Ctrl+Q)
        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            if (cancel || isBarrierItem(event.getCurrentItem())) {
                cancel = true;
            }
        }

        if (cancel) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            player.sendActionBar(Component.text("🔒 This slot is locked as a mission penalty!", NamedTextColor.RED, TextDecoration.BOLD));

            // Resync inventory immediately so cursor/items return to original state without disappearing
            player.updateInventory();
        }
    }

    /**
     * Prevents dragging items into locked slots.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Set<Integer> lockedSlots = getLockedSlots(player.getUniqueId());
        if (lockedSlots.isEmpty()) return;

        InventoryView view = event.getView();
        int topSize = view.getTopInventory().getSize();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) {
                int playerSlot = view.convertSlot(rawSlot);
                if (lockedSlots.contains(playerSlot)) {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                    player.sendActionBar(Component.text("🔒 One or more selected slots are locked!", NamedTextColor.RED, TextDecoration.BOLD));
                    player.updateInventory();
                    return;
                }
            }
        }
    }

    /**
     * Prevents offhand item swapping (F key) if mainhand or offhand is locked.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        int mainhandSlot = player.getInventory().getHeldItemSlot();

        if (isSlotLocked(player.getUniqueId(), 40) || isSlotLocked(player.getUniqueId(), mainhandSlot)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            player.sendActionBar(Component.text("🔒 Off-hand or main hand slot is locked!", NamedTextColor.RED, TextDecoration.BOLD));
            player.updateInventory();
        }
    }

    /**
     * Prevents dropping barrier items or dropping items from locked hotbar slots.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();
        int heldSlot = player.getInventory().getHeldItemSlot();

        if (isBarrierItem(dropped) || isSlotLocked(player.getUniqueId(), heldSlot)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            player.sendActionBar(Component.text("🔒 Cannot drop items from a locked slot!", NamedTextColor.RED, TextDecoration.BOLD));
            player.updateInventory();
        }
    }

    /**
     * Removes barrier items from player drops on death so barrier glass panes don't drop as world items.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isBarrierItem);
    }

    /**
     * Refreshes locked slots on join.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshInventory(player), 20L);
    }

    /**
     * Refreshes locked slots on respawn.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshInventory(player), 20L);
    }
}
