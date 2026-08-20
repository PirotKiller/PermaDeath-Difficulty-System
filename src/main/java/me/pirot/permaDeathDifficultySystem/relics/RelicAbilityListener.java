package me.pirot.permaDeathDifficultySystem.relics;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles active abilities for relics that require event-based logic:
 * - Olympic Torch: Fire Resistance + torch damage immunity
 * - Sacred Pearl: Unlimited ender pearls + no teleport damage
 * - Lucky Clover / Curse Remover / Totem of True God: Prevent Totem failure
 * - Infernal Elytra: Lightning immunity
 * - Sacred Tree Apple: Mega buff on consume
 * - Poseidon's Trident: Multi-lightning on throw
 * - Hercules Chestplate: 100-damage Thorns
 * - Nautilus Helmet: Water effects
 * - Helmet of Hades: Invisibility
 * - Onigari-no-Ryuuou: Negative effect immunity + Fire Resistance
 * - Ultra Shield: Resistance V
 * - Curse Remover: removes penalties, breaks on death/totem use
 */
public class RelicAbilityListener implements Listener {

    private final PermaDeathDifficultySystem plugin;

    public RelicAbilityListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the periodic relic effect application task.
     */
    public void startTasks() {
        // Apply passive relic effects every 5 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::applyPassiveEffects, 100L, 100L);
    }

    /**
     * Applies passive effects from relics the player is holding/wearing.
     */
    private void applyPassiveEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check each relic type

            // Ultra Shield: Resistance V when held in off-hand
            if (hasRelic(player.getInventory().getItemInOffHand(), "Ultra Shield")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 4, false, false, true));
            }

            // Olympic Torch: Fire Resistance when held in main hand
            if (hasRelic(player.getInventory().getItemInMainHand(), "Olympic Torch")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 140, 0, false, false, true));
                player.setMetadata("pdds_torch_immune", new FixedMetadataValue(plugin, true));
            } else {
                if (player.hasMetadata("pdds_torch_immune")) {
                    player.removeMetadata("pdds_torch_immune", plugin);
                }
            }

            // Nautilus Helmet: Water Breathing + Invisibility + Dolphin's Grace + Resistance X
            if (hasRelic(player.getInventory().getHelmet(), "Nautilus Helmet")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 140, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 140, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 140, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 9, false, false, true));
            }

            // Helmet of Hades: Invisibility + Speed III + Resistance V
            if (hasRelic(player.getInventory().getHelmet(), "Helmet of Hades")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 140, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 2, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 4, false, false, true));
            }

            // Hercules Chestplate: Strength V + Resistance XXX
            if (hasRelic(player.getInventory().getChestplate(), "Hercules Chestplate")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 140, 4, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 29, false, false, true));
            }

            // Spartan Greaves: Resistance VIII
            if (hasRelic(player.getInventory().getLeggings(), "Spartan Greaves")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 7, false, false, true));
            }

            // Hermes Boots: Speed VII
            if (hasRelic(player.getInventory().getBoots(), "Hermes Boots")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 6, false, false, true));
            }

            // Onigari-no-Ryuuou: Resistance XX + Fire Resistance + negative effect immunity
            if (hasRelic(player.getInventory().getItemInMainHand(), "Onigari-no-Ryuuou")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 140, 19, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 140, 0, false, false, true));
                removeNegativeEffects(player);
            }

            // Curse Remover: Negative effect immunity + remove penalties
            if (hasRelicInInventory(player, "Curse Remover")) {
                removeNegativeEffects(player);
                player.setMetadata("pdds_totem_protect", new FixedMetadataValue(plugin, true));
                // Remove all inventory penalties
                if (plugin.getMissionManager() != null) {
                    plugin.getMissionManager().getPenaltyManager().removeAllPenalties(player.getUniqueId());
                }
            }

            // Lucky Clover / Totem of True God: Prevent Totem failure
            if (hasRelicInInventory(player, "Lucky Clover")
                    || hasRelicInInventory(player, "Totem of the True God")) {
                player.setMetadata("pdds_totem_protect", new FixedMetadataValue(plugin, true));
            }

            // Totem of True God: Regeneration III
            if (hasRelicInInventory(player, "Totem of the True God")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 140, 2, false, false, true));
            }
        }
    }

    /**
     * Sacred Pearl: Prevent Ender Pearl teleport damage.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnderPearlDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        if (hasRelicInInventory(player, "Sacred Pearl")) {
            if (player.hasMetadata("pdds_ender_pearl_thrown")) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Sacred Pearl: Don't consume the pearl on throw.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnderPearlLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        if (hasRelic(player.getInventory().getItemInMainHand(), "Sacred Pearl")) {
            // Restore the pearl (it was consumed on throw)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (hasRelic(mainHand, "Sacred Pearl")) {
                    mainHand.setAmount(16); // Maintain stack size
                }
            }, 1L);
        }
    }

    /**
     * Infernal Elytra: Lightning immunity.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLightningDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;

        if (hasRelic(player.getInventory().getChestplate(), "Infernal Elytra")) {
            event.setCancelled(true);
        }
    }

    /**
     * Sacred Tree Apple: Mega buff on consume.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSacredAppleConsume(PlayerItemConsumeEvent event) {
        if (!hasRelic(event.getItem(), "Sacred Tree Apple")) return;

        Player player = event.getPlayer();
        // Apply all buffs
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 36000, 4, false, false, true)); // +10 golden hearts
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 36000, 9, false, false, true)); // Resistance X
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 36000, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 36000, 9, false, false, true)); // Strength X
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 36000, 2, false, false, true)); // Speed III
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 36000, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 36000, 2, false, false, true));

        // Remove negative effects
        removeNegativeEffects(player);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        player.sendMessage(Component.text("§6§l✦ The Sacred Tree Apple fills you with divine power! ✦"));
    }

    /**
     * Hercules Chestplate: 100-damage Thorns.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamagedHercules(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        if (hasRelic(player.getInventory().getChestplate(), "Hercules Chestplate")) {
            attacker.damage(100.0, player);
        }
    }

    /**
     * Poseidon's Trident: Multiple lightning strikes on hit.
     */
    @EventHandler
    public void onTridentHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;

        // Check if it's Poseidon's Trident (by checking the item the player threw)
        if (!hasRelicInInventory(player, "Poseidon's Trident")) return;

        Location loc = trident.getLocation();
        // Multiple lightning strikes
        for (int i = 0; i < 5; i++) {
            Location strikeLoc = loc.clone().add(
                    Math.random() * 10 - 5, 0, Math.random() * 10 - 5);
            loc.getWorld().strikeLightning(strikeLoc);
        }

        // Damage entities within 10 blocks
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 10, 10, 10)) {
            if (entity instanceof LivingEntity le && !(entity instanceof Player)) {
                le.damage(20.0, player);
            }
        }
    }

    /**
     * Totem of True God: Infinite uses — don't consume on resurrect.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemResurrect(EntityResurrectEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if holding Totem of the True God
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (hasRelic(mainHand, "Totem of the True God") || hasRelic(offHand, "Totem of the True God")) {
            // Restore the totem after it's consumed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!hasRelic(mainHand, "Totem of the True God")
                        && !hasRelic(offHand, "Totem of the True God")) {
                    // Give it back
                    if (plugin.getRelicManager() != null) {
                        plugin.getRelicManager().giveRelic(player, "totem-of-true-god");
                    }
                }
            }, 1L);
        }
    }

    /**
     * Curse Remover: Breaks on death.
     */
    @EventHandler
    public void onPlayerDeathCurseRemover(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Remove Curse Remover from drops
        event.getDrops().removeIf(item -> hasRelic(item, "Curse Remover"));
    }

    // ==================== Helpers ====================

    private boolean hasRelic(ItemStack item, String relicName) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;
        String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        return displayName.contains(relicName);
    }

    private boolean hasRelicInInventory(Player player, String relicName) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (hasRelic(item, relicName)) return true;
        }
        // Check armor
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (hasRelic(item, relicName)) return true;
        }
        return false;
    }

    private void removeNegativeEffects(Player player) {
        PotionEffectType[] negativeEffects = {
                PotionEffectType.POISON, PotionEffectType.WITHER,
                PotionEffectType.WEAKNESS, PotionEffectType.SLOW,
                PotionEffectType.SLOW_DIGGING, PotionEffectType.HUNGER,
                PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION,
                PotionEffectType.LEVITATION, PotionEffectType.BAD_OMEN,
                PotionEffectType.DARKNESS
        };
        for (PotionEffectType type : negativeEffects) {
            player.removePotionEffect(type);
        }
    }
}
