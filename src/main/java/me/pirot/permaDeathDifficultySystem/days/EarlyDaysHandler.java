package me.pirot.permaDeathDifficultySystem.days;

import me.pirot.permaDeathDifficultySystem.PermaDeathDifficultySystem;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Random;

/**
 * Handles mechanics for Days 1–7:
 * Day 1: Spiders shoot webs; spawners destroy nearby light sources
 * Day 2: Crops stop growing; zombies get Resistance I and iron swords
 * Day 3: Hunger every 30min; sleeping disabled; foxes steal items
 * Day 4: All mobs +20% health; wolves apply Hunger; spiders get Resistance II + Strength I
 * Day 5: Lightning on pumpkin players; cats apply Darkness; skeleton bows Power III + Flame
 * Day 6: Permanent storm; rain blindness; Enderman immobilize; fishing mob spawn
 * Day 7: Mobs +10% Strength; Killer Rabbits; witch special potions; upgraded Zombie Villagers
 */
public class EarlyDaysHandler extends DayHandler {

    private final Random random = new Random();

    public EarlyDaysHandler(PermaDeathDifficultySystem plugin) {
        super(plugin);
    }

    @Override
    public boolean handlesDay(int day) {
        return day >= 1 && day <= 7;
    }

    @Override
    public void onDayStart(int day) {
        // Day 6: Set permanent storm
        if (day >= 6 && isDayActive(6)) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(true);
                world.setThundering(true);
                world.setWeatherDuration(Integer.MAX_VALUE);
                world.setThunderDuration(Integer.MAX_VALUE);
            }
        }
    }

    // ==================== Day 2: Crops stop growing ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onCropGrow(BlockGrowEvent event) {
        if (!isDayActive(2)) return;
        // Cancel all natural crop growth
        Material type = event.getBlock().getType();
        if (isCrop(type)) {
            event.setCancelled(true);
        }
    }

    private boolean isCrop(Material mat) {
        return mat == Material.WHEAT || mat == Material.CARROTS || mat == Material.POTATOES
                || mat == Material.BEETROOTS || mat == Material.MELON_STEM || mat == Material.PUMPKIN_STEM
                || mat == Material.SWEET_BERRY_BUSH || mat == Material.COCOA
                || mat == Material.NETHER_WART || mat == Material.BAMBOO;
    }

    // ==================== Day 3: Sleeping disabled ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!isDayActive(3)) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(
                net.kyori.adventure.text.Component.text("§c§lYou cannot sleep! The darkness won't let you rest.", net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    // ==================== Day 3: Foxes steal items ====================
    @EventHandler
    public void onPlayerInteractWithFox(PlayerInteractEntityEvent event) {
        if (!isDayActive(3)) return;
        if (!(event.getRightClicked() instanceof Fox)) return;

        Player player = event.getPlayer();
        // Fox steal mechanic: random item from inventory
        int slot = random.nextInt(player.getInventory().getSize());
        ItemStack item = player.getInventory().getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            Fox fox = (Fox) event.getRightClicked();
            // Drop the item near the fox and remove from player
            fox.getWorld().dropItemNaturally(fox.getLocation(), item.clone());
            player.getInventory().setItem(slot, null);
            player.sendMessage(net.kyori.adventure.text.Component.text("§6A fox stole your " + item.getType().name() + "!"));
            // Make the fox run
            fox.setTarget(null);
            fox.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2, false, false));
        }
    }

    // ==================== Day 5: Lightning on pumpkin-wearing players ====================
    @EventHandler
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isDayActive(5)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if player is wearing a pumpkin
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.getType() == Material.CARVED_PUMPKIN) {
            double chance = plugin.getConfigManager().getDaySetting(5, "pumpkin-lightning-chance", 0.05);
            if (random.nextDouble() < chance) {
                player.getWorld().strikeLightning(player.getLocation());
            }
        }
    }

    // ==================== Day 6: Rain causes blindness ====================
    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!isDayActive(6)) return;

        Player player = event.getPlayer();
        World world = player.getWorld();

        // Only apply if it's raining and player is exposed to sky
        if (world.hasStorm() && player.getLocation().getBlockY() >= world.getHighestBlockYAt(player.getLocation()) - 1) {
            double chance = plugin.getConfigManager().getDaySetting(6, "rain-blindness-chance", 0.1);
            if (random.nextDouble() < chance * 0.01) { // Per-tick chance, keep low
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false, true));
            }
        }
    }

    // ==================== Day 6: Endermen immobilize players ====================
    @EventHandler
    public void onEndermanTarget(EntityTargetLivingEntityEvent event) {
        if (!isDayActive(6)) return;
        if (!(event.getEntity() instanceof Enderman) || !(event.getTarget() instanceof Player player)) return;

        int immobilizeSeconds = plugin.getConfigManager().getDaySetting(6, "enderman-immobilize-seconds", 3);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, immobilizeSeconds * 20, 127, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, immobilizeSeconds * 20, 128, false, false, true));
        player.sendMessage(net.kyori.adventure.text.Component.text("§5An Enderman's gaze paralyzes you!"));
    }

    // ==================== Day 6: Fishing can spawn upgraded aquatic mob ====================
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!isDayActive(6)) return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        double chance = plugin.getConfigManager().getDaySetting(6, "fishing-mob-spawn-chance", 0.03);
        if (random.nextDouble() < chance) {
            // Spec: you hook a mob *instead of* loot, so drop the catch on the floor.
            if (event.getCaught() != null) {
                event.getCaught().remove();
            }
            event.setExpToDrop(0);

            Location loc = event.getHook().getLocation();
            Drowned drowned = (Drowned) loc.getWorld().spawnEntity(loc, EntityType.DROWNED);
            drowned.setCustomName("§bEnraged Drowned");
            drowned.setCustomNameVisible(true);

            // Upgrade the drowned
            AttributeInstance health = drowned.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40.0);
                drowned.setHealth(40.0);
            }
            AttributeInstance damage = drowned.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(10.0);
            }

            // Give trident
            ItemStack trident = new ItemStack(Material.TRIDENT);
            drowned.getEquipment().setItemInMainHand(trident);
            drowned.getEquipment().setItemInMainHandDropChance(0.0f);

            event.getPlayer().sendMessage(net.kyori.adventure.text.Component.text("§c§lSomething emerges from the depths!"));
        }
    }

    // ==================== Day 7: Witches throw a nastier potion ====================

    /**
     * Rewrites the witch's thrown potion payload. Vanilla witches only ever throw Slowness,
     * Poison, Weakness or Harming; this swaps in a heavier debuff cocktail.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWitchThrowPotion(ProjectileLaunchEvent event) {
        if (!isDayActive(7)) return;
        if (!(event.getEntity() instanceof ThrownPotion potion)) return;
        if (!(potion.getShooter() instanceof Witch)) return;
        if (!plugin.getConfigManager().getDaySetting(7, "witch-cursed-potions", true)) return;

        ItemStack splash = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) splash.getItemMeta();
        if (meta == null) return;

        meta.setBasePotionData(new PotionData(PotionType.WATER));
        meta.setColor(Color.fromRGB(60, 0, 80));
        meta.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 200,
                plugin.getConfigManager().getDaySetting(7, "witch-wither-level", 2) - 1), true);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 0), true);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 300, 1), true);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, 200, 2), true);
        splash.setItemMeta(meta);

        potion.setItem(splash);
    }

    // ==================== Day 2/4/5: Mob spawn modifications ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();

        // Day 2: Zombies get Resistance I and iron swords
        if (isDayActive(2) && entity instanceof Zombie zombie) {
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            ItemStack sword = new ItemStack(Material.IRON_SWORD);
            zombie.getEquipment().setItemInMainHand(sword);
            zombie.getEquipment().setItemInMainHandDropChance(0.05f);
        }

        // Day 4: All mobs +20% health
        if (isDayActive(4) && entity instanceof Monster) {
            AttributeInstance health = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                double bonus = plugin.getConfigManager().getDaySetting(4, "mob-health-bonus-percent", 20) / 100.0;
                double newHealth = health.getBaseValue() * (1.0 + bonus);
                health.setBaseValue(newHealth);
                entity.setHealth(newHealth);
            }
        }

        // Day 4: Spiders/Cave Spiders get Resistance II and Strength I
        if (isDayActive(4) && (entity instanceof Spider || entity instanceof CaveSpider)) {
            int resLevel = plugin.getConfigManager().getDaySetting(4, "spider-resistance-level", 2);
            int strLevel = plugin.getConfigManager().getDaySetting(4, "spider-strength-level", 1);
            entity.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, resLevel - 1, false, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, strLevel - 1, false, false));
        }

        // Day 5: Skeleton bows get Power III and Flame
        if (isDayActive(5) && entity instanceof Skeleton skeleton) {
            ItemStack bow = new ItemStack(Material.BOW);
            ItemMeta meta = bow.getItemMeta();
            int powerLevel = plugin.getConfigManager().getDaySetting(5, "skeleton-bow-power", 3);
            meta.addEnchant(Enchantment.ARROW_DAMAGE, powerLevel, true);
            if (plugin.getConfigManager().getDaySetting(5, "skeleton-bow-flame", true)) {
                meta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
            }
            bow.setItemMeta(meta);
            skeleton.getEquipment().setItemInMainHand(bow);
            skeleton.getEquipment().setItemInMainHandDropChance(0.05f);
        }

        // Day 7: Rabbits become Killer Rabbits
        if (isDayActive(7) && entity instanceof Rabbit rabbit) {
            if (plugin.getConfigManager().getDaySetting(7, "killer-rabbits", true)) {
                rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                rabbit.setCustomName("§c§lThe Killer Bunny");
                rabbit.setCustomNameVisible(true);
                AttributeInstance health = rabbit.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(20.0);
                    rabbit.setHealth(20.0);
                }
            }
        }

        // Day 7: Upgraded Zombie Villagers
        if (isDayActive(7) && entity instanceof ZombieVillager zv) {
            AttributeInstance health = zv.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(40.0);
                zv.setHealth(40.0);
            }
            AttributeInstance damage = zv.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(8.0);
            }
            zv.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
            zv.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        }

        // Day 7: Mobs receive +10% Strength
        if (isDayActive(7) && entity instanceof Monster) {
            AttributeInstance damage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damage != null) {
                double bonus = plugin.getConfigManager().getDaySetting(7, "mob-strength-bonus-percent", 10) / 100.0;
                damage.setBaseValue(damage.getBaseValue() * (1.0 + bonus));
            }
        }
    }
}
