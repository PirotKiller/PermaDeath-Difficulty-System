# Claude Code Build Prompt — Minecraft 1.19.2 Hardcore/PermaDeath Plugin

Copy everything below the line into Claude Code as your task prompt (or save it as `PROJECT_BRIEF.md` in the repo root and tell Claude Code to read it first).

---

## Project Goal

Build a **Paper/Spigot server plugin** for **Minecraft Java 1.19.2** that implements a progressive, configurable 35-day Hardcore/PermaDeath difficulty system. Every day of a Hardcore world's life introduces new mob behavior changes, environmental hazards, a daily mission with a reward/penalty, and eventually a custom Ender Dragon fight, a fully custom relic-item tier system, and a scripted final boss ("The Core").

This is a large, multi-system plugin. Work incrementally, get each system compiling and testable before moving to the next, and flag any mechanic below that cannot be implemented cleanly on Paper 1.19.2 without NMS/reflection or a supporting library — propose the best available approach and confirm before building it.

## Tech Stack & Project Setup

- **Target server software:** Paper 1.19.2 (fall back to Spigot API compatibility where reasonable, but assume Paper is available so its extended API, e.g. `Registry`, `PersistentDataContainer`, `EntityEffect`, adventure `Component` text, and Paper-specific events can be used).
- **Language:** Java 17 (required by 1.19.2).
- **Build tool:** Maven (or Gradle if you prefer — pick one and set up the full project skeleton, `plugin.yml`, and dependency shading).
- **Libraries to consider (propose before adding):**
  - NBT/PersistentDataContainer for tagging custom relic items (no external lib needed on Paper).
  - A scheduler/task library is unnecessary — use Bukkit's `BukkitScheduler`.
  - For mob AI overrides that Paper's public API doesn't expose (e.g. custom goal selectors, "see/target through walls"), use NMS access via reflection or Paper's `Mob` NMS handle, or evaluate a library like ProtocolLib only if packet-level tricks are required. Explain your approach before implementing anything that touches NMS internals, since these are version-fragile.
- Set up a clean package structure from the start, e.g.:
  ```
  com.<studio>.<pluginname>
  ├── config/        (config loading, per-mechanic settings)
  ├── progression/   (day tracker, day-effect registry)
  ├── mobs/          (per-mob modifiers, attribute/AI changes)
  ├── missions/      (daily mission definitions, tracking, rewards, penalties)
  ├── penalties/     (penalty application/removal, inventory-slot logic)
  ├── relics/        (relic item definitions, NBT tagging, ability listeners)
  ├── bosses/        (Ender Dragon phase, "The Core" final boss)
  ├── commands/      (admin/moderation commands)
  └── listeners/     (Bukkit event listeners wired to the above)
  ```

## Core Design Requirements

1. **Day progression system.** A `DayManager` tracks the current Hardcore day (1–35, then holds at 35 or loops per config). Each day's effects are additive — Day N includes everything introduced on days 1..N, unless the spec below says a mechanic replaces an earlier one. Persist current day across server restarts (flat file or existing world-data storage).
2. **Config-driven everything.** Every mechanic below should be individually enable/disable-able, with configurable damage, effect duration/amplifier, radius, probability, cooldown, and timer values, plus configurable mob health/damage/speed/equipment/spawn-count multipliers, and configurable mission rewards/penalties — all via a `config.yml` (or per-feature YAML files) with sane defaults matching the spec. Reloadable via `/hcconfig reload` or similar.
3. **Mission/reward/penalty framework** (see Section "Daily Missions" below) that:
   - Assigns each day's mission automatically when that day starts.
   - Grants the reward immediately on completion (drop reward on the ground if the player's inventory is full).
   - Leaves mission items in the player's inventory after completion (they are not consumed as a turn-in).
   - Applies the day's penalty automatically if the mission is not completed by the next daily reset.
   - Lets server moderators apply or remove any specific penalty from any player via command, independent of mission completion.
4. **Custom AI (optional/toggleable).** An intelligent targeting mode where affected mobs can detect/target players through walls, but lose their target if the player has Invisibility. Implement as a configurable, per-mob-type toggle.
5. **Relic system** — a fixed set of named, lore-flavored custom items (three tiers) with fixed enchantment-like effects delivered via potion-effect listeners, attribute modifiers, and custom item behavior (see full list below). **Every relic has Curse of Vanishing** (destroyed on death, no keep-inventory). Relics should be obtainable through whatever acquisition method you propose (e.g. mission rewards, boss drops, or admin/give commands) — confirm the acquisition approach before implementing loot tables.
6. **Ender Dragon overhaul and final boss ("The Core")** — see dedicated sections below.
7. **Admin/moderation commands** to manage day progression, mission state, and penalties per player (see Commands section).

---

## Day-by-Day Progression Spec (Days 1–35)

Implement each day's changes as additive, independently toggleable modifiers. All numeric values (health %, effect durations, radii, probabilities) must be config-driven; the numbers below are the intended defaults.

- **Day 1:** Spiders gain the ability to shoot webs at range. Spawners disable or destroy light sources (torches, lanterns, etc.) within 16 blocks.
- **Day 2:** Crops stop growing naturally. Zombies spawn with Resistance I and iron swords.
- **Day 3:** Players take a hunger tick (or forced hunger effect) every 30 minutes. Sleeping is disabled. Foxes can steal a random item or stack from a player and flee with it.
- **Day 4:** All mobs get +20% max health. Wolves apply Hunger to players within 16 blocks. Spiders and cave spiders get Resistance II and Strength I.
- **Day 5:** Lightning can strike players wearing a pumpkin (carved pumpkin on head). Cats apply Darkness to players within 16 blocks. Skeleton bows are enchanted with Power III and Flame.
- **Day 6:** Permanent storm/thunder weather. Rain exposure can cause Blindness. Outpost-associated illagers are stronger. Endermen can immobilize a player that looks at them for 3 seconds. Endermen receive a general upgrade (attributes/effects — define specifics). Fishing has a 3% chance to hook an upgraded aquatic mob instead of loot.
- **Day 7:** All mobs get +10% Strength. Rabbits are replaced/upgraded into "Killer Rabbits" (hostile, buffed). Witches gain access to a potion with a negative-effect payload. Zombie Villagers are upgraded.
- **Day 8:** The Nether becomes accessible (if previously locked) — or note: confirm whether this means "portals now function" vs. "Nether difficulty content activates," since the brief implies the Nether was gated until this point. Players take Mining Fatigue every 30 minutes. Spawners summon 5–10 mobs per trigger instead of 1. "Giant Slimes" are introduced as a new upgraded slime variant. Normal Slimes are upgraded (attributes).
- **Day 9:** Salmon are replaced by an upgraded Drowned variant when fished/encountered. Raids become significantly harder. Major upgrades to Drowned, Evokers, Ravagers, Vindicators, illager crossbow users (Pillagers), and Vexes.
- **Day 10:** Normally neutral/passive mobs become hostile toward players. Mob targeting range increases to 100 blocks. Blazes, Piglins, and Piglin Brutes are upgraded.
- **Day 11:** Lightning can strike players carrying a bed (in inventory or hand). Totem of Undying has a 10% chance to fail to activate. "Quantum Creepers" are introduced (define their signature ability — likely short-range teleport, confirmed further in Day 23/32). Wither Skeletons get upgraded equipment.
- **Day 12:** Players take Weakness every 30 minutes. All mobs get +30% movement speed. Zombies and Skeletons are further upgraded.
- **Day 13:** Squids and Glow Squids are replaced by Elder Guardians. Cod are replaced by Guardians. "Giant Magma Cubes" introduced. Regular Magma Cubes are strengthened. Phantoms apply an extreme Levitation effect to nearby players.
- **Day 14:** Diamond armor pieces can attract/trigger lightning strikes on the wearer. Natural health regeneration is disabled. Torches damage/burn players within 16 blocks. A dangerous Redstone Torch mechanic is introduced (define specifics — likely similar damage/burn behavior to torches, or an explosive trigger). Spawners are made harder (higher spawn rate/mob strength). Spiders/cave spiders receive a further upgrade.
- **Day 15:** Axolotls apply Poison II to players within 16 blocks. Cows can explode without destroying terrain (visual/damage effect only, no block griefing).
- **Day 16:** Players take Slowness every 30 minutes. Drowning damage/rate is greatly increased. Iron Golems are replaced by upgraded Vexes.
- **Day 17:** The Warden applies an extreme Slowness effect to players within 20 blocks. Zombies gain configurable block interaction (open doors, break/place blocks per config-defined restrictions). Endermen receive a further upgrade.
- **Day 18:** Lava deals 3× damage and causes Nausea to nearby players. Players take Poison II every 30 minutes. Fire deals 3× damage. Ghasts are upgraded.
- **Day 19:** Bats cause Blindness to players within 16 blocks and gain Strength III/Speed III themselves. Silverfish are replaced by invisible "Berserk Silverfish."
- **Day 20:** Mob targeting range increases to 256 blocks. Bare-hand (unarmed) attacks against players cause massive bonus damage. Ender Pearl teleportation self-damage is doubled.
- **Day 21 — Ender Dragon Stage:** See dedicated section below. Also: the End dimension opens (if previously locked). Players take damage above Y=150. "Giant Zombies" introduced. Players take a Wither effect every 30 minutes. Player max health is reduced to 2 hearts (define whether this is a debuff effect or an actual max-health attribute change, and how it interacts with relic health bonuses). Endermen can pick up most block types.
- **Day 22:** Shulker projectiles explode on impact and apply extreme Levitation. Shulkers explode on death (visual/effect only, no block destruction) and leave behind a lingering Dragon's Breath cloud. Endermen are further strengthened.
- **Day 23:** "Ender Quantum Creepers" can teleport and can spawn/appear in the End dimension. Ghasts are replaced by "Ender Ghasts" with lethal projectiles that also leave Dragon's Breath clouds.
- **Day 24:** Cows are replaced by Ravagers. Ravagers can charge through and destroy a 5×5 area of blocks and apply Bad Omen V on hit. Ravager attributes are significantly increased.
- **Day 25:** A Wither boss can periodically spawn at a random player's location (define spawn interval/probability via config).
- **Day 26:** Spiders move at greatly increased speed while in water.
- **Day 27:** Skeletons use bows enchanted with Power X. Their arrows can trigger lightning strikes on hit.
- **Day 28:** Endermites are replaced by "Endermites of Death" with approximately 1,000 HP and 500 attack damage.
- **Day 29:** Spawners increase their spawn rate while being mined (a defensive/griefing-deterrence mechanic).
- **Day 30:** Lightning can strike players wearing/using an Elytra.
- **Day 31:** Touching Bedrock or Obsidian applies Wither III to the player.
- **Day 32:** Ender Quantum Creepers can destroy Obsidian and water-containing blocks.
- **Day 33:** Phantoms explode when killed.
- **Day 34:** Rain becomes "Acid Rain," applying Poison II to exposed players.
- **Day 35 — Final Boss Stage:** See "The Core" section below.

## Ender Dragon Stage (Day 21)

Implement a custom Ender Dragon encounter layered on top of vanilla behavior:
- A "Dragon's Breath flood" mechanic — define and confirm the exact trigger and area-denial behavior (e.g. periodically floods the arena floor with lingering Dragon's Breath clouds).
- Telegraphed warnings before major dragon attacks (title/actionbar/sound cues).
- Additional enemies spawn from End Crystals during the fight.
- The dragon has substantially increased health and damage compared to vanilla.

## Final Boss: "The Core" (Day 35)

A fully custom, command-block-or-plugin-driven boss encounter (the brief describes it as originally command-block-based — reimplement natively in the plugin rather than relying on command blocks):
- A stationary or semi-mobile "Core" object/boss entity with a large health pool, using a boss bar.
- Players damage it by mining/attacking it directly.
- The Core defends itself with a rotating set of attack mechanics (propose a concrete kit — e.g. periodic AoE damage pulses, summoned adds, projectile attacks, environmental hazards — and confirm before building).
- On defeat, trigger an appropriate end-of-run reward/celebration sequence.

## Custom AI / Targeting

- Config-gated "intelligent targeting" mode. When enabled for a mob type, that mob can detect and path toward players even without line of sight (through walls), within a configurable range.
- A player with active Invisibility causes any mob currently targeting them (via this system) to lose/clear that target.
- Document clearly which mobs this applies to per day/config, since the brief ties several day-specific behaviors (e.g. Day 10, Day 20 targeting range increases) to this same underlying targeting system — implement one shared targeting/AI service that both the day-progression system and the "Custom AI" feature draw from, rather than duplicating logic.

## Relic System

All relics below have **Curse of Vanishing** and should be implemented as uniquely identifiable custom items (persistent-data-tagged, with custom name/lore/model as you see fit) whose effects are applied via equipment-check listeners (tick-based or event-based — propose the more efficient approach) rather than vanilla enchantments where the effect isn't natively expressible.

**Tier 1**
- Ultra Shield — Resistance V, Unbreaking.
- Ultra Totem — a Totem of Undying variant that also grants +5 hearts.
- Dragon Slayer — Sharpness VI diamond sword.
- Mountain Devourer — Efficiency VI diamond pickaxe.
- Elder Turtle Helmet — Respiration IV.

**Tier 2**
- Propulsion Trident — Riptide III, Unbreaking, and grants Speed II while held/used.
- Olympic Torch — grants Speed VIII while held; cannot be placed as a block; does not trigger the Day 14 torch-damage mechanic; grants Fire Resistance.
- Lucky Clover — grants +100 Luck; prevents Totem of Undying failure (interacts with the Day 11 Totem-failure mechanic).
- Infernal Elytra — increased flight speed, unbreakable, lightning immunity (interacts with Day 30's lightning-vs-Elytra mechanic).
- Sacred Pearl — unlimited-use Ender Pearl behavior; immunity to teleportation self-damage (interacts with Day 20's doubled teleport-damage mechanic).
- Sacred Tree Apple — a consumable/held item granting +10 golden hearts, Resistance X, Fire Resistance, Strength X, Speed III, Night Vision, and removal of all negative effects, all for 30 minutes.
- World Eater — unbreakable Netherite pickaxe with Fortune V and Efficiency VIII.
- Blade of Olympus — unbreakable-style Netherite sword with Sharpness VIII and no attack cooldown.
- Poseidon's Trident — unbreakable, Loyalty III, Channeling-like ability enhanced to strike multiple lightning bolts, and deals lightning damage to nearby entities within 10 blocks on use.

**Tier 3**
- Nautilus Helmet — Water Breathing, Invisibility, Dolphin's Grace, Resistance X (while worn).
- Hercules Chestplate — Strength V, Resistance XXX(30), 100-damage Thorns, unbreakable, plus advanced/beyond-vanilla chestplate enchantment effects (propose specifics).
- Helmet of Hades — Invisibility, Speed III, Resistance V, +10 hearts, unbreakable, plus advanced helmet enchantment effects (propose specifics).
- Aegis — unbreakable shield granting +50 Resistance and +10 hearts while held.
- Hermes Boots — Speed VII, unbreakable, plus advanced boot enchantment effects (propose specifics).
- Spartan Greaves — Resistance VIII, +5 hearts, unbreakable.
- Curse Remover — grants immunity to negative effects, removes active inventory-slot penalties, prevents Totem failure; the item itself breaks/is consumed if the player uses a Totem or dies.
- Totem of the True God — prevents Totem failure, infinite uses (does not consume on activation), grants +10 hearts and Regeneration III; usable from the main hand or hotbar (not just off-hand, unlike vanilla Totems).
- Onigari-no-Ryuuou — custom Netherite sword (custom name/lore, and a custom model if a resource pack is in scope — confirm), no attack cooldown, Resistance XX(20), Fire Resistance, immunity to negative effects, deals 1,000 damage per hit, unbreakable.

Confirm with the requester which relic effects that exceed vanilla enchantment caps (e.g. Sharpness VIII, Resistance XXX) should be implemented as: (a) custom enchantments registered via NBT + listener-applied effects, or (b) potion-effect auras applied while the item is worn/held. Recommend (b) for anything above vanilla enchantment level caps, since custom enchantment levels beyond vanilla's cap require additional NMS work to display/function correctly.

## Daily Mission System

Each day has exactly one mission: an objective, an immediate reward on completion, and an inventory-slot-based penalty applied if it's missed by the next daily reset. Implement the mission framework generically (objective type, target count, reward item/effect, penalty definition) so specific values are easy to configure/adjust; use the objective themes below as the intended content:

- **Days 1–6:** Find a Diamond; kill Zombies; obtain Phantom Membranes; obtain Spider Eyes; kill Cats and Wolves; kill Endermen.
- **Days 7–12:** Kill Zombie Villagers and Witches; kill a Giant Slime; complete a Level 5 Raid; kill Blazes; obtain a Creeper Head; obtain a Rabbit's Foot.
- **Days 13–18:** Obtain Sponges; kill a Giant Magma Cube; obtain an Axolotl in a Bucket; obtain a Conduit; obtain Sculk Catalysts; obtain a Netherite Block.
- **Days 19–24:** Obtain Diamond Blocks; collect required mob heads/items; obtain the Giant Zombie's special drop/block; obtain Shulker Shells; kill Ender Ghasts; kill Ravagers.
- **Days 25–30:** Obtain Wither Roses; kill Spiders; kill Skeletons; kill Endermites; kill Cave Spiders; kill 100 Endermen.
- **Days 31–34:** Kill Withers; kill Ender Quantum Creepers; kill Phantoms; kill Wardens.

Flag to the requester that the exact reward items and exact penalty (which inventory/armor/off-hand slot, what effect) for each of the 35 missions are **not fully specified in this brief** — the original master specification has the authoritative values. Build the framework to accept these as config values with reasonable placeholder defaults, and note clearly in the README/config comments that the per-day numbers should be reviewed against the master spec before going live.

Mission/penalty rules to implement regardless of exact values:
- Rewards are granted the instant the objective is completed, not at the next reset.
- If the player's inventory is full when a reward is granted, drop the reward item(s) on the ground at the player's feet.
- Items used to complete a mission remain in the player's inventory afterward (missions are progress-tracked, not "turn in and lose the item").
- If a mission is incomplete at the next daily reset, apply that day's configured penalty automatically.
- Moderators can apply or remove any specific penalty from any player at any time via command, independent of the mission system.

## Admin / Moderation Commands

Design a command set (propose exact syntax, e.g. under a `/hardcore` or `/hc` namespace) covering at minimum:
- View/set the current global day.
- Reload config.
- View a player's active mission status.
- Manually complete or fail a player's current mission (for support/edge cases).
- Apply a named penalty to a specific player.
- Remove a named penalty from a specific player.
- List a player's active penalties.
- Give a specific relic to a player (for testing/admin distribution).
Gate all of these behind an appropriate permission node per command (e.g. `hardcore.admin.day`, `hardcore.admin.penalty`), not a single blanket OP check.

## Suggested Development Order

Follow this order so each layer can be tested before the next depends on it:
1. Project skeleton, config system, and the `DayManager` (day tracking + reset scheduling).
2. Mission/reward/penalty framework (generic engine, wired to admin commands, with Days 1–6 as real test content).
3. Mob attribute/effect/behavior changes, implemented day-by-day and gated by the day/config system.
4. Custom AI/targeting service (shared by day-progression and the standalone toggle).
5. Relic system (item definitions, tagging, effect listeners).
6. Ender Dragon overhaul and "The Core" final boss.
7. Full playtesting/balancing pass across all 35 days on a real 1.19.2 Paper test server.

## Open Questions to Resolve Before/During Development

Surface these back to the requester rather than guessing silently:
1. Exact per-day mission reward items and penalty slot/effect values (only themes are given here).
2. Whether "Nether opens" (Day 8) and "End opens" (Day 21) imply portals are disabled before those days, and if so, how (block breaking, or a custom gate).
3. Exact specification of several vaguely-described mechanics: the Redstone Torch mechanic (Day 14), the Quantum Creeper ability set (Days 11/23/32), "upgraded" stat blocks for mobs mentioned only as "upgraded" without numbers (e.g. Day 6/9/10/17/18/22 Endermen, Blazes, Piglins, Ghasts), and The Core's specific attack kit (Day 35).
4. Whether relics are acquired via mission rewards, boss loot, crafting, or admin-only distribution.
5. Whether a resource pack (custom item models/textures, e.g. for Onigari-no-Ryuuou) is in scope, or whether relics should look like re-named vanilla items with custom NBT/lore only.

Acknowledge that the requester has stated a complete original specification exists as the master reference for exact numeric values, and that this brief is a cleaner overview for scoping/estimation — ask for that master spec if any Day/mission/relic value here is ambiguous during implementation.
