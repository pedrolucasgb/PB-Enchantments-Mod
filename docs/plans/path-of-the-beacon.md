# Path of the Beacon — development plan

Tree id `beacon`, icon `Items.BEACON`, display name **"Beacon — Path of the Beacon"**.
Flagship feature: **instant-mine deepslate** by combining a beacon Haste with two nodes.

This plan is written for a code agent working in this repository. Every phase ends in a
buildable jar. Follow the repo's existing patterns (named below) rather than inventing new
infrastructure. In-game text is English only; the `lang-sync` skill fills pt_br / es_es / es_mx.

---

## 0. Design rules (read first)

1. **The beacon never reads anyone's skills.** A beacon is a shared block. Every node either
   changes what *the receiving player* gets from a vanilla beacon, or unlocks a **craftable
   block/item** whose behaviour is the same for everyone. No "owner", no "keeper", no per-player
   state stored on the block entity. This is what keeps the tree multiplayer-safe.
2. **One beacon mixin only**: `BeaconBlockEntityMixin` on `applyEffects` (the method that runs
   every 80 ticks, collects players in the AABB and calls `addEffect`). All receiver-side
   modifiers and all "time in beam" counting hang off that one point so nothing can desync.
3. **Every mining-speed number lives in `MiningSpeed`** (`perk/MiningSpeed.java`). The
   `PlayerMixin.getDestroySpeed` hook applies it on client and server, and
   `/pbenchants debug speed` reports it. Do not add a second speed path.
4. Node ids, gate ids, tier names and costs below are the contract with the lang file and the
   README. Change them only together with the lang keys.

---

## 1. The flagship: deepslate instamine

### Why Haste III alone is not enough

Vanilla: `destroyProgress = destroySpeed / hardness / 30` (correct tool), instant when >= 1.0.
Deepslate hardness is 3.0, so the player needs destroy speed **>= 90**.

| Setup (netherite pickaxe 9 + Efficiency V 26 = 35) | Speed |
|---|---|
| + Haste II (beacon, x1.4) | 49.0 |
| + Haste III (x1.6) | 56.0 |
| + Haste III + Mason's Grip III (x1.6) | 89.6 (2 ticks) |
| + Haste III + **Deepslate Breaker** (x1.75) | **98.0 instant** |
| Diamond (34) + Haste III + Deepslate Breaker | **95.2 instant** |
| + Haste II + Deepslate Breaker + Mason's Grip I (x1.2) | **102.9 instant** |
| + Haste II + Deepslate Breaker, no Mason's Grip | 85.8 (2 ticks) |

So the feature is two nodes that compose with the existing `MiningSpeed` factor:

- **Deepslate Breaker** (tier 2 node `deepslate_breaker`, PASSIVE): `+75%` destroy speed with a
  pickaxe on the deepslate family: `DEEPSLATE`, `COBBLED_DEEPSLATE`, `POLISHED_DEEPSLATE`,
  `DEEPSLATE_BRICKS`, `DEEPSLATE_TILES`, `CHISELED_DEEPSLATE`, `INFESTED_DEEPSLATE`, and the
  cracked variants. **Not** deepslate ores (hardness 4.5, and Fortune makes instamine ores a
  farm). Mirror `OBSIDIAN_BREAKER_BONUS` in `MiningSpeed.pickaxe(...)`. Deepslate is a
  `BASE_STONE_OVERWORLD` block, so Mason's Grip already applies to it: the deepslate bonus must
  **multiply with** Mason's Grip, not return early the way the obsidian branch does.
- **Resonant Haste** (tier 3 node `resonant_haste`, PASSIVE): while the player carries a
  **Haste II** effect (amplifier 1; in vanilla only a level-4 beacon grants that), the mod
  keeps a **Haste III** (amplifier 2) on them instead. Implementation is `DeepHaste`'s pattern:
  a new `perk/ResonantHaste.java` called from the once-a-second slow tick in `PBEnchants`
  (next to `DeepHaste.tick`). If `player.getEffect(HASTE)` has amplifier == 1 and the player
  owns the node, add `HASTE` amplifier 2, 100 ticks, ambient, no particles, refreshed when
  below 40 ticks. Vanilla `addEffect` never downgrades a stronger active effect, so the
  beacon's 80-tick Haste II re-application does not fight it. When the player leaves the beam
  the Haste II expires, the check fails and Haste III fades within 5 s. Never touch a Haste that
  is already amplifier >= 2 or infinite.

Side effects to state in the node description: Haste also raises attack speed (+10% per level);
Haste III below Y = 0 supersedes Deep Haste I (already handled: `DeepHaste` never overrides a
stronger Haste).

Acceptance (`/pbenchants debug speed` while looking at deepslate):
- Netherite Eff V, inside a level-4 Haste beacon, both nodes owned: reported speed >= 90, the
  block breaks in one tick, the crack animation never shows (client and server agree).
- Same without `resonant_haste`: speed about 85.8, two ticks. Add Mason's Grip I: instant.
- Deepslate iron ore under the same setup: not instant.

---

## 2. Tree definition (`skill/SkillTrees.java`)

Add `BEACON` after `GROUND` in the file and in `ORDER`; add `"beacon"` to `IN_TESTING`.
Tier levels 5 / 10 / 15 / 20 / 30 like the tool trees. Costs follow the tool-tree bands
(tier 1: 3-5 XP, tier 2: 5-6, tier 3: 6-10, tier 4: 8-14, capstones 20).

### Tiers and gates

| # | Tier name | Level | Gates (counter -> target) |
|---|---|---|---|
| 1 | Star Seeker | 5 | `kill_wither_skeletons` 25, `collect_wither_skull` 1, `place_metal_blocks` 9, `visit_fortress` 1 |
| 2 | Star Bearer | 10 | `slay_wither` 1, `craft_beacon` 1, `activate_beacon` 1, `pay_beacon` 8 |
| 3 | Pyramid Builder | 15 | `pyramid_tier_4` 1, `beacon_effect_checklist` 5, `minutes_in_beam` 120, `place_pyramid_blocks` 164 |
| 4 | Lightkeeper | 20 | `slay_wither` 5, `beacons_activated` 3, `minutes_in_beam` 600, `place_pyramid_blocks` 500 |
| 5 | Avatar of the Beam | 30 | `slay_wither` 15, `beacons_activated` 6, `minutes_in_beam` 2000, `minutes_regenerating` 60 |

`beacon_effect_checklist` is a closed list, so it goes in `GateChecklists`: Speed, Haste,
Resistance, Jump Boost, Strength (bits 0-4; Regeneration is tracked by `minutes_regenerating`
instead). `beacons_activated` counts distinct beacon positions via
`TreeProgress.see("beacon", posKey, "beacons_activated")`.

### Nodes

Tier 1 (index 0)
- `skull_collector` PASSIVE, 4 XP, icon WITHER_SKELETON_SKULL, cost 16 bone + 8 coal.
  Wither skeleton skull drop chance 2.5% -> 6% for this killer. Hook: `CombatDrops.onKill`
  already resolves `WITHER_SKELETON_SKULL`; add the roll there.
- `wither_ward` PASSIVE, 3 XP, icon WITHER_ROSE, cost 8 bone + 4 soul sand.
  Wither effect damage on the player -50% (`ServerLivingEntityEvents.ALLOW_DAMAGE` or
  `AFTER_DAMAGE` pattern from `ArmorTracker`, matching `damageSources().wither()`).
- `beam_sense` PASSIVE, 5 XP, icon SPYGLASS, cost 16 glass + 4 glowstone dust.
  Client HUD: active beacon effects with remaining time, and a bearing to the nearest *loaded*
  beacon block entity within 128 blocks (scan chunk block entities in render distance once a
  second). Pattern: `SetSenseHud`.

Tier 2 (index 1)
- `deepslate_breaker` PASSIVE, 6 XP, icon DEEPSLATE, cost 64 cobbled deepslate + 8 iron. See §1.
- `reach_of_the_beam_1` PASSIVE, 5 XP, icon BEACON, cost 8 iron block + 16 glass. Chain I->III.
  The player receives beacon effects from **+10 / +20 / +40** blocks beyond the beacon's range.
  Implementation in the beacon mixin: after vanilla collects players with its AABB, run a second
  `getEntitiesOfClass(ServerPlayer, aabb.inflate(40))`, and for each player not already served,
  apply the same effects if `distance <= range + reachBonus(player)`. One extra AABB query per
  beacon per 80 ticks.
- `lingering_light_1` PASSIVE, 5 XP, icon TORCH, cost 16 glowstone dust + 8 gold. Chain I->III.
  Beacon effects last **30 s / 90 s / 5 min** after leaving the beam instead of about 9 s. In the
  mixin, when an effect is applied to a player who owns the node, use `duration + bonus` for
  that player only.
- `thrifty_offering` PASSIVE, 6 XP, icon GOLD_INGOT, cost 4 emerald + 8 gold.
  25% chance the payment item is refunded. Hook: `BeaconMenuMixin` on the method that consumes
  the payment slot when the effect packet arrives (`updateEffects` in 26.2; verify with javap,
  see the toolchain memory on renames).

Tier 3 (index 2)
- `resonant_haste` PASSIVE, 10 XP, icon NETHERITE_PICKAXE, cost 1 diamond block + 32 glowstone
  dust + 1 nether star. See §1. This is the tree's selling point; declare it first in the tier.
- `reach_of_the_beam_2` chained, 8 XP.
- `lingering_light_2` chained, 7 XP.
- `early_regeneration` PASSIVE, 8 XP, icon GLISTERING_MELON_SLICE, cost 8 golden apple + 16
  glowstone dust. In a beacon of pyramid level >= 2 the player also receives Regeneration I
  (receiver-side; the mixin has `this.levels`).
- `prism_1` PASSIVE, 8 XP, icon PRISMARINE_CRYSTALS, cost 32 prismarine crystals + 4 amethyst.
  Chain I->II. **Personal attunement**: the player picks one extra effect they receive inside
  any active beacon. I: Night Vision, Fire Resistance. II adds Slow Falling, Saturation.
  Selection: `/pbenchants attune <effect>` in phase 3, a picker in the skill screen in phase 5.
  Stored as an index in the tree's `TreeProgress.counters` under `attune` (no new codec field).

Tier 4 (index 3)
- `reach_of_the_beam_3` chained, 10 XP.
- `lingering_light_3` chained, 9 XP.
- `brighter_beam` PASSIVE, 12 XP, icon DIAMOND_BLOCK, cost 4 diamond block + 1 nether star.
  In a level-4 pyramid the player receives the primary at **+1 amplifier** regardless of the
  secondary choice, so Regeneration can be the secondary. Haste stays capped at III (Resonant
  Haste already covers it; never stack to IV).
- `hallowed_core` ITEM, 10 XP, icon SOUL_LANTERN, cost 32 glowstone + 1 totem + 4 soul soil.
  Unlocks the **recipe** of a new block `pbenchants:hallowed_core`. Placed directly under a
  beacon it stops hostile natural spawns inside the beacon's *vanilla* range (never Reach).
  Global by design; every player in range benefits. Spawn hook: mixin on the natural-spawner
  validity check, with a per-level `Set<BlockPos>` of active cores maintained by the core's
  block entity on load/unload, so a spawn attempt is a hash lookup, not a block scan.
- `star_lantern` ITEM, 10 XP, icon LANTERN, cost 1 nether star + 1 lantern + 8 gold.
  Gives a `pbenchants:star_lantern` item. Sneak-use inside a beam stores the effects currently
  granted; use outside the beam replays them for 15 min, then it needs recharging in a beam.
- `beamwalk` ACTIVE, 14 XP, icon ENDER_EYE, cost 8 ender pearl + 1 echo shard + 4 amethyst.
  Sneak-use on a beacon registers it (max 5; only beacons **the player placed**: record the
  placer UUID in the beacon's block entity NBT from `BlockItemMixin`/`PlaceTracker`, one field
  that never changes). Pressing the ability key teleports to the next registered beacon (cycle)
  if the target chunk is loaded, the block is still an active beacon, and the player is in the
  same dimension; costs 1 ender pearl, 10 min cooldown. Pattern: `DiggyDiggyHole` +
  `AbilityStatePayload` for the HUD.

Tier 5 (index 4), capstones at 20 XP each
- `sunless_core` ITEM, icon CRYING_OBSIDIAN, cost 2 netherite ingot + 32 obsidian + 1 nether
  star. Recipe for `pbenchants:sunless_core`; a beacon standing on it ignores the sky check
  (works underground, in the Nether and the End). Global. Mixin target: the sky-obstruction
  check in `BeaconBlockEntity.tick` (the beam-section loop).
- `starfall` PASSIVE, icon NETHER_STAR, cost 16 wither skeleton skulls + 1 nether star.
  20% chance a Wither killed by this player drops a second nether star (`CombatDrops.onKill`).
- `phantom_tier` PASSIVE, icon EMERALD_BLOCK, cost 4 diamond block + 1 emerald block.
  For this player a pyramid counts one level higher (level 3 gives level-4 effects, including
  the secondary and Brighter Beam). Receiver-side: the mixin uses `min(4, levels + 1)` for owners.

---

## 3. Phases

### Phase 1: tree skeleton + the flagship
1. `SkillTrees.BEACON` with all tiers, gates and nodes; everything except the phase-1 nodes
   marked `.future()` (Artisan's `shulker_sight` shows the pattern).
2. Lang: `tree.pbenchants.beacon`, `.short`, `tier.pbenchants.beacon.1..5`, every
   `gate.pbenchants.<id>` (+ `.desc`), every `node.pbenchants.<id>` (+ `.desc`), and the five
   `advancements.toolmastery.beacon.tier_n.title/description` keys. Then run `lang-sync`.
3. Advancements: `data/toolmastery/advancement/beacon/tier_1..5.json`, copied from `ground/`.
4. Gate feeding:
   - `CombatTracker.onKill`: `kill_wither_skeletons` (WITHER_SKELETON) and `slay_wither`
     (WITHER, `addCount`, not `put`) in the **beacon** tree.
   - `ItemEntityMixin` pickup path: `collect_wither_skull`. `ItemGainTracker.onCraftTake`:
     `craft_beacon`.
   - `PlaceTracker.onPlace`: `place_metal_blocks` and `place_pyramid_blocks` for iron / gold /
     emerald / diamond / netherite blocks (both counters fed by the same placement is fine).
   - `BiomeTracker` structure scan: `visit_fortress` when the structure id is `minecraft:fortress`.
   - New `BeaconMenuMixin` (server): on a successful effect update with a payment consumed,
     `pay_beacon` +1, `activate_beacon` 1, `pyramid_tier_4` when `levels == 4`,
     `beacons_activated` via `see`. `BeaconBlockEntity.levels` needs an accessor mixin.
   - New `BeaconBlockEntityMixin.applyEffects`: for each served `ServerPlayer` add 4 to a
     `seconds_in_beam` counter and derive `minutes_in_beam` from it (document the choice);
     tick `beacon_effect_checklist` bits for the primary and secondary being applied;
     `minutes_regenerating` when Regeneration is among them.
5. `deepslate_breaker` in `MiningSpeed` and `resonant_haste` in `perk/ResonantHaste.java`
   (slow tick). Extend the `/pbenchants debug speed` output with both.
6. `PBEnchantsCommand` tree-id lists pick up `beacon` from `ORDER` automatically. Verify
   `/pbenchants debug add beacon minutes_in_beam 2000` and `unlocktier` work.
7. Client: the tab appears from `ORDER`. Check `SkillTreeScreen` layout with 21 nodes and the
   IN TESTING badge. Nothing else to add.
8. Acceptance: the §1 table; gates advance on a real wither fight in a test world
   (`/pbenchants debug kit` for gear).

### Phase 2: receiver-side beacon modifiers
`reach_of_the_beam_1..3`, `lingering_light_1..3`, `early_regeneration`, `brighter_beam`,
`phantom_tier`, all inside `BeaconBlockEntityMixin.applyEffects`. Write one helper
`perk/BeamReceiver.java` that, given (player, levels, primary, secondary, baseDuration), returns
the list of `MobEffectInstance` to apply for *that* player; the mixin only iterates. Unit-test
the helper's pure logic (levels / amplifier / duration table) with JUnit if the build has a test
source set; otherwise cover it through `/pbenchants debug` and write down the manual matrix.

### Phase 3: payment and combat perks
`thrifty_offering` (BeaconMenuMixin), `skull_collector`, `starfall`, `wither_ward`,
`prism_1..2` with `/pbenchants attune`. Prism effects are added by `BeamReceiver` from the
stored attunement.

### Phase 4: blocks and items
First blocks in the mod: add a `block/` package, `ModBlocks`, `ModItems`, block states, models,
textures, loot tables and recipes under `data/pbenchants/recipe/`. Recipes are **unlocked** by
the ITEM node; read `BiomeCharts` first to see how an ITEM node delivers, then gate the recipe
with a recipe-unlock hook. Content: `hallowed_core`, `sunless_core`, `star_lantern`. Sunless
needs the sky-check mixin; Hallowed needs the spawn mixin and the per-level core set.

### Phase 5: active and HUD
`beamwalk` (ability key, placer UUID, cooldown HUD via `AbilityStatePayload`), `beam_sense`
HUD, attunement picker in the skill screen replacing the command.

### Phase 6: release
Bump `gradle.properties` version, run `patch-notes` and `lang-sync`, add the README section
"Beacon nodes (Path of the Beacon)" in the same style as the other trees, build the jar into
`dist/` and `Quick-Download/`, install it into `.minecraft/mods`, write the in-game test
checklist, tag `vX.Y.Z`, then run `server-motd`.

---

## 4. Risks and decisions already made

- **26.2 method names**: `BeaconBlockEntity.applyEffects`, `BeaconMenu.updateEffects`, the
  `levels` field and the sky check must be confirmed with the javap trick from the toolchain
  memory before writing the mixins. Register both mixins in `pbenchants.mixins.json`.
- **Client/server agreement on instamine**: the effect is synced by vanilla and `MiningSpeed`
  runs on both sides, so no extra packet. If the client shows a crack for one frame, the
  client-side `PerkAccess` lookup for `deepslate_breaker` is missing; that is the bug, not the
  numbers.
- **Haste from non-beacon sources**: vanilla has no Haste potion, so "Haste II present" is a
  safe proxy for "inside a level-4 Haste beacon". Commands and other mods can grant Haste II
  and the node then upgrades those too. Acceptable; document it in the node description.
- **Deepslate ores are excluded** on purpose (Fortune + instamine = farm).
- **Hallowed Core affects mob farms** in range like any placed block would. It is a block, not
  a skill, so the remedy is the same as for any block: break it.
- **Beamwalk into other people's bases**: only beacons the player placed. No claims system.
- **Reach + Hallowed**: spawn suppression uses the vanilla range only, never Reach.
- **Counters are per tree**: `slay_wither` in the beacon tree is separate from the sword
  tree's `slay_boss`. Both trackers fire from the same `AFTER_DEATH` callback.
