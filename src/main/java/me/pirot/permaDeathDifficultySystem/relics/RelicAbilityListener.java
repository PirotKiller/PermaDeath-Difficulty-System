package me.pirot.permaDeathDifficultySystem.relics;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Event-driven and tick-driven relic abilities.
 *
 * <p>Relics are matched by their persistent-data id via {@link RelicManager}, never by display
 * name — see the note on that class for why.
 *
 * <p>Effects that exceed vanilla enchantment caps (Resistance XXX, Sharpness VIII, Speed VIII)
 * are delivered as potion-effect auras refreshed on a timer, per the brief's recommendation (b).
 * The aura duration deliberately outruns the refresh interval so the effect never flickers.
 */
public class RelicAbilityListener implements Listener {

    /** Refresh interval for passive auras. */
    private static final long AURA_PERIOD_TICKS = 100L; // 5s
    /** Aura duration; longer than the period so effects don't gap between refreshes. */
    private static final int AURA_DURATION_TICKS = 140; // 7s

    private static final PotionEffectType[] NEGATIVE_EFFECTS = {
            PotionEffectType.POISON, PotionEffectType.WITHER,
            PotionEffectType.WEAKNESS, PotionEffectType.SLOW,
            PotionEffectType.SLOW_DIGGING, PotionEffectType.HUNGER,
            PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION,
            PotionEffectType.LEVITATION, PotionEffectType.BAD_OMEN,
            PotionEffectType.DARKNESS, PotionEffectType.UNLUCK
    };

    private final PermaDeathDifficultySystem plugin;

    public RelicAbilityListener(PermaDeathDifficultySystem plugin) {
        this.plugin = plugin;
    }

    public void startTasks() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::applyPassiveEffects,
                AURA_PERIOD_TICKS, AURA_PERIOD_TICKS);
    }

    private RelicManager relics() {
        return plugin.getRelicManager();
    }

    private void applyPassiveEffects() {
        RelicManager relics = relics();
        if (relics == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            // Ultra Shield — Resistance V in the off-hand.
            if (relics.isRelic(offHand, "ultra-shield")) {
                aura(player, PotionEffectType.DAMAGE_RESISTANCE, 4);
            }

            // Olympic Torch — Speed VIII (attribute) plus Fire Resistance and torch immunity.
            boolean holdingTorch = relics.isRelic(mainHand, "olympic-torch");
            setFlag(player, "pdds_torch_immune", holdingTorch);
            if (holdingTorch) {
                aura(player, PotionEffectType.FIRE_RESISTANCE, 0);
            }

            // Nautilus Helmet
            if (relics.isRelic(player.getInventory().getHelmet(), "nautilus-helmet")) {
                aura(player, PotionEffectType.WATER_BREATHING, 0);
                aura(player, PotionEffectType.INVISIBILITY, 0);
                aura(player, PotionEffectType.DOLPHINS_GRACE, 0);
                aura(player, PotionEffectType.DAMAGE_RESISTANCE, 9);
            }

            // Helmet of Hades
            if (relics.isRelic(player.getInventory().getHelmet(), "helmet-of-hades")) {
                aura(player, PotionEffectType.INVISIBILITY, 0);
                aura(player, PotionEffectType.SPEED, 2);
                aura(player, PotionEffectType.DAMAGE_RESISTANCE, 4);
            }

            // Hercules Chestplate
            if (relics.isRelic(player.getInventory().getChestplate(), "hercules-chestplate")) {
                aura(player, PotionEffectType.INCREASE_DAMAGE, 4);
                aura(player, PotionEffectType.DAMAGE_RESISTANCE, 29);
            }

            if (relics.isRelic(player.getInventory().getLeggings(), "spartan-greaves")) {
                aura(player, PotionEffectType.DAMAGE_RESISTANCE, 7);
            }

            if (relics.isRelic(player.getInventory().getBoots(), "hermes-boots")) {
                aura(player, PotionEffectType.SPEED, 6);
            }

            // Onigari-no-Ryuuou
            if (relics.isRelic(mainHand, "onigari-no-ryuuou")) {
                aura(player, PotionEffectType.DAMAGE_RESISTANCE, 19);
                aura(player, PotionEffectType.FIRE_RESISTANCE, 0);
                removeNegativeEffects(player);
            }

            // Curse Remover — negative-effect immunity and clears inventory-slot penalties.
            boolean hasCurseRemover = relics.hasRelic(player, "curse-remover");
            if (hasCurseRemover) {
                removeNegativeEffects(player);
                var missions = plugin.getMissionManager();
                var penalties = missions == null ? null : missions.getPenaltyManager();
                if (penalties != null && !penalties.getLockedSlots(player.getUniqueId()).isEmpty()) {
                    penalties.removeAllPenalties(player.getUniqueId());
                }
            }

            // Totem failure protection (Day 11). Cleared when the relic leaves the inventory —
            // the old code only ever set this flag, so protection outlived the relic.
            boolean totemProtected = hasCurseRemover
                    || relics.hasRelic(player, "lucky-clover")
                    || relics.hasRelic(player, "totem-of-true-god");
            setFlag(player, "pdds_totem_protect", totemProtected);

            if (relics.hasRelic(player, "totem-of-true-god")) {
                aura(player, PotionEffectType.REGENERATION, 2);
            }
        }
    }

    /** Sacred Pearl: no self-damage from teleporting. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnderPearlDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!player.hasMetadata("pdds_ender_pearl_thrown")) return;

        if (relics().hasRelic(player, "sacred-pearl")) {
            event.setCancelled(true);
        }
    }

    /**
     * Sacred Pearl: unlimited uses. Also marks the shooter so subsequent fall damage from
     * teleporting can be identified and cancelled.
     *
     * <p>The flag lives here rather than in a Day 20 handler because Sacred Pearl's
     * no-teleport-damage guarantee needs to work on every day, not just day 20 — {@link
     * #onEnderPearlDamage} reads the same flag. Day 20's own ender-pearl handler also sets
     * this flag (redundantly), which is harmless.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnderPearlLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        player.setMetadata("pdds_ender_pearl_thrown", new FixedMetadataValue(plugin, true));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.hasMetadata("pdds_ender_pearl_thrown")) {
                player.removeMetadata("pdds_ender_pearl_thrown", plugin);
            }
        }, 100L);

        // Resolve which hand holds the relic before the throw consumes it.
        boolean mainHand = relics().isRelic(player.getInventory().getItemInMainHand(), "sacred-pearl");
        boolean offHand = relics().isRelic(player.getInventory().getItemInOffHand(), "sacred-pearl");
        if (!mainHand && !offHand) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack stack = mainHand
                    ? player.getInventory().getItemInMainHand()
                    : player.getInventory().getItemInOffHand();
            if (relics().isRelic(stack, "sacred-pearl") && stack.getAmount() < 16) {
                stack.setAmount(16);
            }
        }, 1L);
    }

    /** Infernal Elytra: lightning immunity (Day 30). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLightningDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;

        if (relics().isRelic(player.getInventory().getChestplate(), "infernal-elytra")) {
            event.setCancelled(true);
        }
    }

    /** Sacred Tree Apple: 30 minutes of everything. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSacredAppleConsume(PlayerItemConsumeEvent event) {
        if (!relics().isRelic(event.getItem(), "sacred-tree-apple")) return;

        Player player = event.getPlayer();
        int duration = 30 * 60 * 20; // 30 minutes

        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 4, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 9, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 9, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false, true));
        removeNegativeEffects(player);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.5f);
        player.sendMessage(Component.text("§6§l✦ The Sacred Tree Apple fills you with divine power! ✦"));
    }

    /** Hercules Chestplate: 100-damage Thorns. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamagedHercules(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (attacker.equals(player)) return;
        if (!relics().isRelic(player.getInventory().getChestplate(), "hercules-chestplate")) return;

        // Guard against thorns-vs-thorns recursion between two wearers.
        if (attacker.hasMetadata("pdds_thorns_reflecting")) return;
        attacker.setMetadata("pdds_thorns_reflecting", new FixedMetadataValue(plugin, true));
        try {
            attacker.damage(100.0, player);
        } finally {
            attacker.removeMetadata("pdds_thorns_reflecting", plugin);
        }
    }

    /**
     * Poseidon's Trident: tag the projectile at launch, since once thrown the relic item is no
     * longer in the player's inventory to check against.
     */
    @EventHandler
    public void onTridentLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;

        if (relics().isRelic(player.getInventory().getItemInMainHand(), "poseidons-trident")
                || relics().isRelic(player.getInventory().getItemInOffHand(), "poseidons-trident")) {
            trident.setMetadata("pdds_poseidons_trident", new FixedMetadataValue(plugin, true));
        }
    }

    /** Poseidon's Trident: a volley of lightning plus a shock to everything within 10 blocks. */
    @EventHandler
    public void onTridentHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;
        if (!trident.hasMetadata("pdds_poseidons_trident")) return;

        Location location = trident.getLocation();
        for (int i = 0; i < 5; i++) {
            Location strike = location.clone().add(Math.random() * 10 - 5, 0, Math.random() * 10 - 5);
            location.getWorld().strikeLightning(strike);
        }

        for (Entity entity : location.getWorld().getNearbyEntities(location, 10, 10, 10)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                living.damage(20.0, player);
            }
        }
    }

    /** Totem of the True God: never consumed, and usable from the main hand. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTotemResurrect(EntityResurrectEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        boolean mainHand = relics().isRelic(player.getInventory().getItemInMainHand(), "totem-of-true-god");
        boolean offHand = relics().isRelic(player.getInventory().getItemInOffHand(), "totem-of-true-god");
        if (!mainHand && !offHand) return;

        // Re-stamp the stack after vanilla consumes it.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!relics().hasRelic(player, "totem-of-true-god")) {
                relics().giveRelic(player, "totem-of-true-god");
            }
        }, 1L);
    }

    /** Curse Remover: destroyed on death, and on a Totem activation. */
    @EventHandler
    public void onPlayerDeathCurseRemover(PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> relics().isRelic(item, "curse-remover"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTotemBreaksCurseRemover(EntityResurrectEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (!relics().isRelic(contents[slot], "curse-remover")) continue;

            ItemStack stack = contents[slot];
            if (stack.getAmount() > 1) {
                stack.setAmount(stack.getAmount() - 1);
            } else {
                player.getInventory().setItem(slot, null);
            }
            player.sendMessage(Component.text("§c§lYour Curse Remover shattered."));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.6f);
            return;
        }
    }

    // ==================== Helpers ====================

    private void aura(Player player, PotionEffectType type, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, AURA_DURATION_TICKS, amplifier, false, false, true));
    }

    /** Sets or clears a metadata marker so it tracks the relic instead of latching on. */
    private void setFlag(Player player, String key, boolean value) {
        if (value) {
            player.setMetadata(key, new FixedMetadataValue(plugin, true));
        } else if (player.hasMetadata(key)) {
            player.removeMetadata(key, plugin);
        }
    }

    private void removeNegativeEffects(Player player) {
        for (PotionEffectType type : NEGATIVE_EFFECTS) {
            player.removePotionEffect(type);
        }
    }
}
