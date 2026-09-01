# PB Enchantments Mod

**Possibly Better Enchantments** — a progressive quality-of-life skill tree mod for **Minecraft 26.2 (Java Edition)** built on **Fabric**.

Every tool class has its own skill tree. You earn access by **playing the class** (achievement gates), pay for unlocks with **XP levels plus materials**, and receive **real enchantments** that integrate with the enchanting table, anvil and `/enchant` — but only at the levels you have unlocked.

> Version **0.7.0-beta** · mod id: `pbenchants` · package `dev.pbenchants` · save-data namespace: `toolmastery`
> (the mod shipped as *Tool Mastery* before the rename — everything a world persists kept the old namespace on purpose, so existing worlds carry all progress, advancements and enchanted items across the rename)
>
> **Download page: [pedrolucasgb.github.io/PB-Enchantments-Mod](https://pedrolucasgb.github.io/PB-Enchantments-Mod/)**

---

## How it works

1. **Play the class** — mining, chopping, smelting and crafting feed per-tier achievement counters.
2. **Complete the gate** — each tier requires class-specific achievements (e.g. *mine 500 stone*, *fell 100 trees with Logic*).
3. **Pay the access cost** — unlocking a tier consumes XP levels (5 / 10 / 15 / 20 / 30).
4. **Unlock nodes** — a one-off purchase: a few XP levels plus materials. Passives switch on immediately; enchantments join your enchanting-table pool.
5. **Enchant your tools** — an unlocked enchantment can be stamped onto the item in your hand for whole XP levels, as often as you like.
6. **Or buy the book** — librarians sell PB Enchantments enchanted books to anyone who has already unlocked the rank on the label.

### Unlock vs. Enchant

Two rules order the whole tree, and they bind **Unlock** only:

- **Tiers open in sequence.** Tier 3 needs tier 2, which needs tier 1 — finishing a later tier's gate early does not let you skip ahead.
- **Ranks are bought in order.** Dig Range III needs II, which needs I. Once a rank is unlocked, though, **Enchant** stamps it straight onto a bare tool: you never have to put I and II on an item on the way to III.

Both are enforced on the server, and the skill screen greys the Unlock button out with the reason in its tooltip rather than letting you click into a refusal.

Every node is bought once with **Unlock** — cheap in XP, paid partly in materials you gathered while playing the class. What that buys depends on the node:

| Node type | What Unlock gives you | Second step? |
|---|---|---|
| Passive | The effect, live immediately | None — the unlock *is* the skill |
| Enchantment | The enchantment starts appearing in **your** enchanting table offers (per player), capped at the level you unlocked | Optional **Enchant** |

**Enchant** is the repeatable, guaranteed route: 20 levels for a rank I, 35 for a rank II, 50 for a rank III (40 for the single-level Indestructible) — all in whole XP levels, applied to whatever is in your main hand. It is priced well above a 30-level table roll on purpose: the enchanting table (which also throws in vanilla enchantments) stays the better deal for anyone willing to grind for it, and Enchant is what you pay when you want *this* enchantment on *this* tool right now.

Both buttons explain themselves before they charge you: clicking either swaps the details panel for a confirmation card listing the exact cost, the materials you are carrying, and what changes — then Confirm or Cancel.

Enchant refuses impossible combinations before spending anything, and says why: Smelt on an axe (wrong tool class), or an enchantment that conflicts with something already on the item (Silk Touch, say). The same check runs on the client, so the button is greyed out with the reason in its tooltip.

Every tier you unlock is also a **vanilla advancement**: open the advancements screen (**L**) and the *PB Enchantments* tab shows one branch per class, five tiers each — earned tiers lit up, the rest as goals ahead.

Progress is **per player**, persisted with the world, and survives death.

## Requirements

- Minecraft **26.2** (Java Edition)
- **Fabric Loader** ≥ 0.19.3 + **Fabric API** 0.157.0+26.2
- **Java 25**

## Download & install

### Latest release

The **[landing page](https://pedrolucasgb.github.io/PB-Enchantments-Mod/)** always offers the newest build, and
every version ever released lives on the
[releases page](https://github.com/pedrolucasgb/PB-Enchantments-Mod/releases).

Each release ships exactly one asset, `pbenchants-<version>.jar` — the version is in the filename so you can
tell at a glance which build is already in your `mods` folder, and replacing it is unambiguous.

Releases are cut by tag: pushing a `vX.Y.Z` tag makes the [Release workflow](.github/workflows/release.yml)
build the jar from that commit (the tag supplies the mod version) and publish it as a GitHub Release — the
landing page reads the releases API live, so it picks the new jar up with no further step.

### Patch notes

Every released version has a notes file at `docs/patchnotes/<version>.md`, carrying the changelog in
English, Portuguese and Spanish in one file. The landing page renders it — the newest version under
**What is new**, and every entry in **All versions** can expand its own — and the release workflow attaches
it to the GitHub Release and uses its English block as the release body. **The workflow refuses to build a
tag with no notes file.** How to write one: [`.claude/skills/patch-notes/SKILL.md`](.claude/skills/patch-notes/SKILL.md),
with a checker at `.claude/skills/patch-notes/check-patch-notes.ps1`.

### Quick download (no toolchain)

Grab [`Quick-Download/PBEnchants-Installer.zip`](Quick-Download/PBEnchants-Installer.zip), unzip it anywhere and double-click **Install PB Enchantments.bat**. It installs Fabric Loader and Fabric API if they are missing, removes any older PB Enchantments jar (two copies of one mod id crash the game) and drops the current one into `.minecraft/mods`. The same files sit loose in [`Quick-Download/`](Quick-Download) if you would rather read them first.

### From source

Build it yourself:

```bash
git clone https://github.com/pedrolucasgb/PB-Enchantments-Mod.git
cd PB-Enchantments-Mod
./gradlew build
```

The mod jar lands in `build/libs/pbenchants-<version>.jar`. Drop it into your `.minecraft/mods` folder (Fabric Loader + Fabric API required).

For development, `./gradlew runClient` launches a ready-to-play instance with the mod loaded. On Windows, make sure `JAVA_HOME` points to a JDK 25.

## Usage

| Action | How |
|---|---|
| Open the skill trees | Press **K** (rebindable) — the same key closes them again — or the `/pbenchants` command |
| See the world while the tree is open | The backdrop button in the screen's top-right corner switches between a solid and a see-through backdrop (remembered in `config/pbenchants-client.json`) |
| Check progress / gates | `/pbenchants status <tree>` or click a tier header in the GUI |
| Unlock the next tier | GUI button, or `/pbenchants unlock <tree>` |
| Unlock a node | Click the node → **Unlock** → Confirm, or `/pbenchants unlock <tree> <node>` |
| Enchant the held item | Click the node → **Enchant** → Confirm, or `/pbenchants enchant <tree> <node>` |
| Other ways in | Enchanting table (any unlocked enchantment) · `/enchant @s toolmastery:<id> <level>` |

### Debug commands (operators)

```
/pbenchants debug maxall               # complete every gate + unlock every tier
/pbenchants debug unlockall            # unlock every node (enchantments unlocked, not applied)
/pbenchants debug kit                  # full kit: one enchanted tool per tier of every tree
/pbenchants debug kit pickaxe          # tier kit for one tree
/pbenchants debug kit pickaxe smelt    # one tool per level of a single enchantment
/pbenchants debug unlocktier pickaxe 3 # gates + tier + every node of exactly that tier
/pbenchants debug unlocktier all 1     # ...for every tree at once
/pbenchants debug add <tree> <counter> <amount>   # bump a gate counter

# ...and the same thing backwards, to re-test a feature from a clean slate
/pbenchants debug reset                # everything to zero: no tiers, no nodes, no counters
/pbenchants debug reset pickaxe        # wipe one tree only
/pbenchants debug tier pickaxe 2       # open exactly 2 tiers (re-locks nodes above)
/pbenchants debug lock pickaxe smelt_1 # re-lock one node, leaving tiers and counters alone
/pbenchants debug strip                # take every PB Enchantments enchantment off the held item
/pbenchants debug speed                # measure the speed passives on the held tool, in ticks
```

---

## Implemented so far (Pickaxe, Axe, Enchanter, Explorer, Artisan, Sword, Armor & Bow)

### Core systems
- ✅ Per-player skill progress (tiers, nodes, counters) persisted via data attachments
- ✅ Achievement gates fed by live block-break tracking (ores, stone, logs, leaves, checklists as bitmasks)
- ✅ Two-price economy: tier access costs, per-node unlock (XP levels + materials) and a repeatable per-node enchant price — all validated server-side
- ✅ Enchant-time compatibility checks (supported item + exclusive sets), mirrored on the client so the button explains itself before you spend
- ✅ Skill tree GUI (key **K**): class tabs with class icons, one column per named tier, item-icon nodes coloured by state, prerequisite connectors, type badges, gate and material checklists, unlock/enchant with a confirmation card, and the player XP bar along the bottom — future classes shown as *Coming soon*
- ✅ Every tier mirrored as a vanilla advancement in the **L** screen (*PB Enchantments* tab), with toast + chat announce on unlock — the tab's root is earned by opening the skill screen for the first time, and a one-line hint in chat on join points at the key that does it
- ✅ A level-up chime when a tier opens, and a lighter one when the goal pinned to the HUD scoreboard is finally ready to buy (never both for the same event)
- ✅ Client–server sync via custom payloads; all actions validated on the server
- ✅ `/pbenchants` command suite with tab completion

### Real enchantments (data-driven, era 26.x)
| Enchantment | Tool | Levels | Effect |
|---|---|---|---|
| **Dig Range** | Pickaxe | I–III | Breaks extra **pickaxe-mineable** blocks of **about the same hardness**: below → cross → 3×3 on the facing plane. Dirt and wood are skipped; stone next to obsidian leaves the obsidian standing — until your mining speed makes both near-instant |
| **Smelt** | Pickaxe | I–III | Chance to smelt ore drops on the spot: 25% / 50% / 100% |
| **Rich Vein** | Pickaxe | I–II | Vein miner: up to 8 / 16 connected ores |
| **Logic** | Axe | I–III | Timber: fells the whole tree in one swing at every level — I pays for it with a fixed, tool-blind chop (about 4s per log; Efficiency and the axe's own tier count for nothing), III clears the canopy too (requires real trees — log houses are safe) |
| **Environment** | Axe | I | Replants the sapling on the stump after a Logic III fell |
| **Indestructible** | Enchanter, any damageable item | I | The item never breaks — damage stops one point short, like an Elytra. Spent, it works like an empty hand until repaired |
| **Slipstream** | Explorer, Elytra | I–III | 10% / 25% / 50% of a firework's push carries over past the point where the boost would normally have died — same rocket, more distance |
| **Keen Edge** | Sword | I–III | Up to +1 / +2 / +3 damage, scaled by how full the attack cooldown was. Measured against the *vanilla* cooldown, so Nostalgy cannot turn a timing reward into a flat bonus |
| **Sweeping Arc** | Sword, axe with Broad Swing | I–II | The sweep lands at 50% / 100% of a full hit and reaches a block further. Adds to the vanilla sweep ratio, so it stacks with Sweeping Edge |
| **Executioner** | Any weapon | I–III | +15 / 30 / 45% damage against a target below 30% health. **Mobs only** |
| **Tidecaller** | Trident | I–II | The trident returns without Loyalty, and returns faster; II launches Riptide on dry land |
| **Gravity Well** | Mace | I–II | The smash converts 25% / 50% more of your fall. **Mobs only** |
| **Phalanx** | Spear | I–II | Reach +1 / +2 blocks, and a braced spear pricks anything standing against you. **Mobs only** |
| **Nostalgy** | Sword | I–IV | The attack cooldown recovers 25 / 50 / 75% faster, and at IV is gone entirely — 1.8 combat. **Works in PvP**, deliberately |
| **Sundering Blow** | Sword, axe, spear | I–II | Ignores 20% / 40% of the target's armour. **Mobs only** |

### Passive skills (pickaxe)
| Node | Levels | Effect |
|---|---|---|
| **Mason's Grip** | I–III | +20% / +40% / +60% mining speed on stone, deepslate and every ore |
| **Miner's Magnet** | — | Pickaxe drops go straight to the inventory (composes with Dig Range, Rich Vein and Smelt) |
| **Deep Haste** | — | Permanent Haste I below Y = 0 |
| **Obsidian Breaker** | — | Obsidian and crying obsidian break 50% faster |
| **Ancient Fortune** | capstone | Lifts your ceiling on vanilla Fortune from III to IV — your enchanting table can offer Fortune IV, nobody else's can |
| **Enduring Edge** | capstone | A Dig Range swing costs the pickaxe half the durability, rounded up: 1 per 2 blocks at I, 3 per 5 at II, 5 per 9 at III |

### Axe passives (Path of the Grove)
| Node | Tier | Effect |
|---|---|---|
| **Lumberjack's Arms** I–III | 1 / 2 / 3 | Axes chop wood +25% / +50% / +75% faster |
| **Logger's Magnet** | 1 | Blocks chopped with an axe go straight into your inventory, Logic fells included |
| **Fair Harvest** | 1 | +25% sapling chance from every leaf block you break, on top of the vanilla roll |
| **Pruner** | 3 | Leaves broken with an axe drop double loot (apples, saplings, sticks) |
| **Double Axe** I–II | 3 / 4 | 10% / 20% chance for a log to drop twice — every log of a Logic fell rolls on its own |
| **Shield Breaker** | 3 | Axes disable a blocking shield 2s longer and push 2 more damage through it |

**Tier 4, not built yet.** Four sides of the axe that have nothing to do with what it can hit: *Kindling* (logs come out as charcoal straight from the swing), *Woodcarver* (right-click cycles a wooden block through its stair, slab and fence forms in place), *Sap Tapper* (a stripped log weeps resin while the tree stands) and *Patina Hand* (sneak-scrape to age copper a stage instead of only scouring it back).

**Tier 5, not built yet.** Two finishers ship starred and orange so the shape of the tier is visible before it is playable: *Everbloom* (a tree you fell replants itself and grows back where it stood) and *Bountiful Grove* (felled logs yield more than they should, scaling with how much of the tree you took in one go).

### Enchanter nodes (Path of the Arcane)
| Node | Tier | Effect |
|---|---|---|
| **Arcane Insight** I–III | 1 / 2 / 3 | A panel beside the enchanting table reveals the *true* enchantments behind offer 1 / offers 1–2 / all three |
| **Scholar** I–III | 1 / 2 / 4 | +20% / +40% / +60% XP from every source (orbs, bottles, smelting, breeding) |
| **Inner Focus** | 3 | Enchanting no longer requires nor consumes lapis lazuli |
| **Indestructible** | 3 | Enchantment: the item never breaks (see the enchantment table above) |
| **Anvil Adept** I–II | 3 / 4 | I takes 30% off every anvil job; II removes *"Too Expensive!"* and caps any job at 40 levels |
| **Ancient Knowledge** | capstone | The three table offers become 35 / 40 / 45 — and the top one sometimes rolls a perfect item |
| **Greater Mending** | capstone | Lifts vanilla Mending from I to II — two Mending books merge into Mending II on *your* anvil, and it mends double per point of XP |
| **Reaper’s Wisdom** | capstone | A mob killed with a Looting weapon gives +25% XP per Looting level, stacking on top of Scholar |

**Anvil Adept, in two ranks.** The bill is rewritten at the one point vanilla computes it — the `Mth.clamp` that folds the prior-work penalty in — so everything downstream reads the same number: the too-expensive wall, the level check in `mayPickup`, and the levels `onTake` actually deducts.

Rank **I** takes 30% off, rounded up, and it lands *before* the anvil decides a job is too expensive, so a 55-level merge comes down to 39 and becomes possible again — that is most of what the rank is for. Rank **II** removes the wall outright and caps any job at 40 levels. *"Too Expensive!"* is a hard stop no amount of XP gets past; in the bytecode it is one `hasInfiniteMaterials` branch, so the perk answers that question instead, exactly as Inner Focus does for the lapis check. The screen needed the same answer separately — the server keeps the item takeable, but `AnvilScreen` decides on its own that any bill of 40 or more prints red.

**Greater Mending.** The Mending data file raises `max_level` to 2 for everybody, because a data pack cannot be per-player — the same trick as Ancient Fortune. What makes it a reward is the clamp back to 1 at the anvil for anyone without the node. The anvil is the only place Mending II can be made at all, since Mending is treasure and never rolls at a table.

**Ancient Knowledge.** Vanilla tops out at 30, and 30 is the reason a whole shelf of enchantments is folklore rather than gameplay: **Sharpness V** needs an enchanting level of 45 before it will even enter the draw, **Efficiency V** needs 41, and a 30-level offer on a diamond sword lands around 36 after the enchantability roll. Those are not rare at a vanilla table — they are *impossible*, and every one you have ever seen came off an anvil. At a fully powered table (15 bookshelves) this capstone makes the three offers ask for **35, 40 and 45**, which puts them back in the draw and drags a longer list in with them, because vanilla keeps rolling for extra enchantments as long as the level holds up.

The XP is a requirement, not a bill: vanilla charges the slot number in levels (1, 2 or 3) and only asks that you *have* the offered level. Ancient Knowledge asks you to be level 45, not to spend 45. It is gated on a full table on purpose — making it unconditional would delete the one ritual the class is built around.

And at the 45 offer there is a **chance, not a promise**, of a perfect item: every enchantment the item can legally carry, each at its own maximum, in a mutually compatible set. Two guard rails keep it a reward rather than a cheat — nothing ever rolls above an enchantment’s own `getMaxLevel()`, so a perfect sword is Sharpness V and never a Sharpness VI no other route in the game can produce; and the pool is the table’s own pool filtered to primary targets, so Mending and the other treasure enchantments are not in it and librarians keep the job they had. Which compatible set you get is rolled too — Sharpness or Smite, Fortune or Silk Touch are exclusive by design, and a fixed pick would mean every perfect axe in the world was the same axe. All of it runs off the same seeded `RandomSource` vanilla uses, so the offer stays deterministic for a given seed and the Arcane Insight preview still shows exactly what you are about to get.

### Explorer nodes (Path of the Horizon)
The first class that is not tied to a tool: it levels from **movement**, so its gates read distance travelled and places seen rather than blocks broken. The distance counters are diffed off the vanilla `*_ONE_CM` statistics once a second, from a baseline taken the first time the mod ever sees you — installing it on a world with a hundred kilometres on the clock does not hand you tier 4.

| Node | Tier | Effect |
|---|---|---|
| **Cartographer** | 1 | Walking into a biome you have never visited prints its name and your coordinates in the action bar, and the tree keeps the list |
| **Tireless** I–III | 1 / 2 / 4 | Moving under your own power — walking, sprinting, swimming, jumping — costs 20% / 40% / 60% less hunger |
| **Sea Legs** | 1 | A boat you are steering cruises ~15% faster |
| **Clear Sight** I–II | 2 / 3 | Underwater fog pushes out as far as Respiration III gives, and stacks on top of it; II clears the water fully and refills your breath about twice as fast |
| **Slipstream** I–III | 2 / 3 / 4 | **Enchantment** for the Elytra: 10% / 25% / 50% of a firework's push carries over past where it would normally have died |
| **Remember** | 3 | You respawn holding a named slip of paper with the coordinates, dimension and in-game day of your last death — it replaces the previous one, so it never becomes clutter |
| **Trailblazer** | 3 | Sprinting for 8 seconds without stopping ramps to +12% movement speed and lingers 2 seconds after you slow down |
| **Soft Landing** | 4 | Elytra wall-crash damage halved, and the first 3 blocks of any fall are free |
| **Waypoint Stone** | 4 | Sneak + right-click with a compass binds the spot you are standing on; the needle points there from then on |
| **Endless Horizon** | capstone | A quarter of the fireworks you burn flying are not consumed, and Slipstream carryover doubles |
| **Pufferfish Lungs** | capstone | Permanent Water Breathing — your breath meter never moves again, in any water, in any dimension |

**Slipstream is the momentum reading, not the refund one.** It lengthens the rocket's own life rather than re-applying a decaying slice of its velocity, so the acceleration curve, the collision handling and the client prediction all stay vanilla's — the extra distance is real, the physics is not reimplemented. The refund idea ships too, priced apart, as the Endless Horizon capstone, which is now the tier on its own.

**Night Eyes.** 26.2 did not remove the light-texture pipeline this needs — `LightTexture` was *renamed* to `Lightmap`, and its inputs were pulled out into `LightmapRenderState`, a per-frame struct with a `brightness` field on it. Writing there beats the usual approach twice over: it is not the player’s persisted gamma setting, so a crash mid-session leaves no video option they never chose; and it is not bounded by the slider, so the node can deliver a lift the options screen cannot. The lightmap shader reads brightness as `mix(colour, liftedColour, brightness)` and the lifted colour equals the original wherever the picture is already bright — so a value past 1 extrapolates in dark pixels and does nothing at all in daylight. Ambient light gets a very dark blue floor too, because a lift alone cannot help where the picture is pure black. The Darkness effect is subtracted afterwards exactly as vanilla does it, so a warden still blinds an Explorer.

### Artisan nodes (Path of Order)
Where Pickaxe and Axe are about *getting* resources, the Artisan is about *keeping them in order* — inventory and storage quality of life, earned instead of installed. Named Artisan rather than Crafter because the class barely crafts, and Minecraft already ships a block called the Crafter.

The controls are a row of slot-sized symbol buttons in the **top-right corner** of the inventory and of every container window, just outside the frame — the inside is where Inventory Profiles Next, Quark and Sophisticated Storage all put theirs. Hover one and it says what it is. Nothing appears until the node behind it is unlocked, and the row is laid out fresh every frame off the window’s live position, so opening the recipe book slides it along with the window instead of stranding it.

| Node | Tier | Effect |
|---|---|---|
| **Sorter's Hand** I–II | 1 / 2 | One Sort button, aimed by the screen: on the inventory it tidies your backpack, and once II is bought it also appears on chests, barrels and shulker boxes and tidies those. Rank I alone shows nothing on a chest — sorting containers is what II buys |
| **Seeker's Eye** I–II | 1 / 2 | Ctrl+F for slots: type in the magnifier field and every matching slot turns yellow — your inventory, then the open container too, with the query remembered from one chest to the next |
| **Steady Grid** | 1 | The 3×3 grid keeps its contents when you close a crafting table and hands them back next time |
| **Deft Hands** | 2 | A hotbar stack that runs out refills itself from your backpack |
| **Locked Slots** | 3 | Alt-click any slot to pin it — sorting, auto-refill, Quick Stack and Restock all step around it |
| **Tidy Storage** | 3 | A container is tidied again every time you close it, so one you sorted stays sorted |
| **Artisan's Order** | 3 | Pick the sort rule: category, name or count |
| **Quartermaster's Call** | 4 | Tops up the stacks you already carry from containers within 8 blocks — never hands you something new |
| **Hand of Order** | capstone | Terraria's *Quick Stack to Nearby Chests* — see below |

**Hand of Order.** Press the button and every item in your backpack flies to the nearby container that already keeps that kind of thing. The rule that makes it safe is that half: an item is only ever deposited into a container that **already knows its kind**, so Quick Stack joins the organisation you built and never invents one. Anything with no home stays on you.

- **What counts as the same kind:** the last word of the item’s registry name. `oak_planks`, `birch_planks` and a modded `ebony_planks` are all *planks*; `iron_ingot` and `ruby_ingot` are both *ingot*; a name with no underscore (`stone`, `cobblestone`) is its own kind. Exact matches are offered every container first and only the leftovers go looking for a same-kind chest, so cobblestone still lands in the cobblestone chest even when a nearer one holds stone. Item **tags** would have been the obvious choice and are the wrong one: vanilla’s item tags mostly describe behaviour (`#wolf_food`, `#piglin_loved`, `#breaks_decorated_pots`), which would file a golden apple with gold ingots. Names are a convention every mod already follows, so items from a mod that is not installed yet cost nothing.

- **Reach:** 8 blocks, loaded chunks only, at most 32 containers, nearest first.
- **Fill order:** partial stacks are topped up before empty slots are used, so a chest with three half-stacks of cobblestone ends with full stacks rather than five scattered piles.
- **Containers:** chests (a double chest counts as one), trapped chests, barrels and placed shulker boxes. Furnaces, hoppers, droppers, dispensers and brewing stands are excluded — accidentally filling a hopper is a griefing machine. Ender chests are out too: quietly emptying your pockets into a shared void would be a trap.
- **Access:** only containers you could legitimately open. A locked container without the key, or a chest under a solid block or a sitting cat, is simply not there — the same path a real right-click takes, so claim mods that hook it work by construction.
- **Never touched:** armour, offhand, the crafting grid, pinned slots, and the hotbar.
- **Server-authoritative.** Unlike every client-side storage mod, none of this can be done on the client: the tree state is the server's, so the client only expresses intent.

### Sword nodes (Path of the Blade)
The combat class, and the first tree that is **seven tiers** rather than five. It covers every weapon that hits — sword, trident, mace, axe-as-weapon and the 26.2 spear — because most of what makes a combat node interesting is weapon-agnostic, and four more tabs would not fit the strip. Seven tiers rather than five because the node list was long enough that a five-tier version handed out the class-defining nodes far too early.

| Tier | Name | Nodes |
|---|---|---|
| 1 | Duelist | Keen Edge I, Combat Magnet, Butcher's Cut |
| 2 | Skirmisher | Keen Edge II, Sweeping Arc I, Broad Swing, Second Wind, Hunter's Mark |
| 3 | Warblade | Keen Edge III, Sweeping Arc II, Executioner I, Tidecaller I, Riposte |
| 4 | Slayer | Executioner II, Gravity Well I, Phalanx I, Cleave, Nostalgy I |
| 5 | Champion | Executioner III, Gravity Well II, Phalanx II, Tidecaller II, Adrenaline, Storm Bearer, Shield Breaker, Nostalgy II |
| 6 | Warlord | Sundering Blow I–II, Bloodthirst, Headhunter, Nostalgy III |
| 7 | Legend | Nostalgy IV, then **one** of Spoils of War / Warlord's Wake — and past both of them, Death Eyes |

**The PvE-only rule.** Armour penetration, execute damage, stacking damage-per-kill and a shockwave on every kill are all fine against a zombie and all rewrite PvP into something nobody asked this mod to design. So the nodes that would are marked **mobs only**: when the target is a player the bonus does not scale down, it does not fire at all. One check, one place (`CombatPerks.appliesTo`), and the fact is on the node's own card in the skill screen so a PvP server does not have to find out in a fight. Executioner, Gravity Well, Phalanx, Cleave, Adrenaline, Sundering Blow, Bloodthirst, Headhunter, Warlord's Wake and Death Eyes are the list.

**Nostalgy is the deliberate exception.** A 1.8 attack cooldown is the one node here meant to be felt in a duel, so it works against players. Two ranks of care went into it: Keen Edge is measured against the vanilla cooldown so the two do not multiply, and a cooldown cannot be made target-specific — so the server switch does not slow the swing down, it makes the swing pay vanilla damage against players.

| Node | Tier | Effect |
|---|---|---|
| **Combat Magnet** | 1 | Drops and XP from mobs you kill fly to you. The third magnet, after the Miner's and the Logger's |
| **Butcher's Cut** | 1 | A food-yielding mob killed with a sword drops twice the food |
| **Broad Swing** | 2 | Axes sweep like swords, and accept vanilla **Sweeping Edge** at the table and the anvil |
| **Second Wind** | 2 | Every kill puts a point of saturation back |
| **Hunter's Mark** | 2 | The mob you last hit is outlined for five seconds, with its health on your action bar |
| **Riposte** | 3 | A shield raised in the half-second before a hit throws a quarter of it back — the counter to Shield Breaker |
| **Cleave** | 4 | An axe hit passes a third of itself to one mob beside the target. Mobs only |
| **Adrenaline** | 5 | Eight seconds of unbroken fighting ramps to +15% damage, lingering three seconds. Mobs only |
| **Storm Bearer** | 5 | Channeling without a thunderstorm, once per in-game day |
| **Shield Breaker** | 5 | *Moved here from the Axe tree*, where a pure PvP node had no business sitting in a woodcutting class. Saves that already bought it keep it |
| **Bloodthirst** | 6 | Each kill within five seconds of the last stacks +10% damage, to five stacks. Mobs only |
| **Headhunter** | 6 | Mobs that can drop their heads do so far more often. Mobs only |
| **Spoils of War** | 7 | Vanilla **Looting** reaches IV, at the table *and* the anvil |
| **Warlord's Wake** | 7 | A killing blow releases a shockwave dealing half of it within four blocks. Mobs only |
| **Death Eyes** | 7 | See below |

**Death Eyes.** You see every mob as one of the dead, and so does your sword: **Smite** applies its full bonus to every living target that is not a player. It is the first node in the mod gated on finishing its own tree — every other node bought, one capstone chosen — rather than on a tier.

Its scope is deliberately narrow. It changes *what Smite considers undead*, on your hits alone: not drops, not Bane of Arthropods, not zombie behaviour, not sunlight, and not what another player's weapon sees. Vanilla drives Smite off the `#minecraft:sensitive_to_smite` entity-type tag, and widening that tag in a data pack would hand the bonus to everyone on the server — so unlike Fortune IV and Looting IV, this one cannot be a data-pack trick with a clamp. The Smite bonus is recomputed on the damage path for the holder instead, and targets vanilla already counts as undead are skipped so nothing is paid twice.

### Server config
`config/pbenchants.json`, written on first run. Two switches, read by the Sword and Bow trees — everything else in the mod plays the same in a solo world and on a server.

| Key | Default | What it does |
|---|---|---|
| `pvp_perks` | `false` | Let the mobs-only nodes fire at players too |
| `nostalgy_pvp` | `true` | Nostalgy's shortened cooldown pays full damage against players. Off makes those hits pay vanilla-cooldown damage instead |
### Armor nodes (Path of the Bulwark)
The one class that cannot level from *doing* something, because wearing armour is not an action. It levels from **damage survived**: the gate counters read what your set absorbed, what your shield stopped and what you walked away from — which is also what makes it the only tree that advances while you play every other one.

Seven tiers, like the Sword, and for the same reason: the defensive kit splits into more distinct steps than five columns can hold without two unrelated ideas sharing a tier.

| Node | Tier | Effect |
|---|---|---|
| **Padded Lining** I–III | 1 / 2 / 4 | Armour takes 15 / 30 / 45% less durability damage |
| **Set Sense** | 1 | Your real armour, toughness and damage reduction, drawn above the armour bar |
| **Shield Wall** I–II | 1 / 3 | The shield is up the instant you raise it; II widens the arc it covers |
| **Steady Stance** | 1 | 25% less knockback from mobs |
| **Flashpoint** | 2 | Touching lava gives you 10 seconds of immunity to it |
| **Thermal Weave** I–II | 2 / 3 | ~25 / 50% less fire and lava damage, *on top of* Fire Protection |
| **Sure Footing** | 2 | Nothing underfoot slows you, plus half Depth Strider and half Soul Speed |
| **Second Skin** | 3 | A piece never spends its last durability point, and warns you |
| **Ablative Plating** I–II | 3 / 5 | Explosion damage −20 / −40%, and no creeper knockback at all |
| **Bulwark** I–III | 4 / 5 / 6 | The shield takes 25 / 50 / 75% less durability; III means an axe cannot disable it |
| **Thorned Plate** I–II | 4 / 6 | Thorns with no durability cost; II fires twice as often and twice as hard |
| **Last Stand** | 4 | Below four hearts: Resistance I for five seconds, once every thirty |
| **Repair Rites** | 5 | A point of durability a second, per piece, after ten seconds still and unhurt |
| **Kinetic Plating** | 5 | Boots: the first six blocks of a fall are free and the rest is halved |
| **Guardian's Aura** | 6 | Players and tamed animals within six blocks take 10% less damage |
| **Warden's Weight** | 6 | Nothing knocks you back while blocking |
| **Nightplate** | 6 | A full set of one material carries a bonus of its own |
| **Aegis** / **Immortal Line** / **Living Armor** | capstone | Pick one of three — see below |

**Where the tree inserts itself into damage.** This was the cross-cutting worry in [#28](../../issues/28), and it is answered once rather than per node: every effect that reduces damage *by type* — Thermal Weave, Ablative Plating — is a real data-driven `damage_protection` enchantment, so it lands in vanilla's own protection sum and composes with Protection, Resistance and absorption instead of racing them. Only what vanilla has no data hook for is Java: durability, knockback, blocking, fall grace and the capstones. Neither is in `#minecraft:exclusive_set/armor`, so both genuinely stack on top of the vanilla protections rather than competing for the slot.

**Flashpoint.** The window opens on the first point of lava damage and runs for ten seconds whatever you do with them. It **only arms again once you have stopped burning** — lava lights you for fifteen seconds, so you cannot be off the fire before the window you just spent has closed. It suppresses lava damage only: you still burn, so it buys a swim to the shore and not a bath.

**The three capstones are a real pick-one.** Aegis lifts your ceiling on vanilla **Protection from IV to V** — raised in the data pack for everybody and clamped back to IV at the table *and* the anvil for anyone without the node, the same trick Ancient Fortune and Greater Mending use. Immortal Line turns a killing blow into one heart with a few seconds of Regeneration and Resistance, once every ten minutes, consuming no totem. Living Armor puts the experience you pick up into the whole set at once, from any source and with no Mending on the pieces. Buying one puts the other two out of reach, which is what made a node's exclusivity a *list* rather than a single id.

**Shared items.** Nothing here changes the answer in the section below: the passives are on the player, not on the gear, and the five enchantments the tree hands out are ordinary enchantments that anyone can carry once they exist. The armour branch of the inert-item check is still owed.

**Gates.** Damage absorbed is the difference between raw and applied damage, counted only while all four slots are filled — a helmet on its own does not creep the counter forward. Shield blocking is measured where the shield actually soaks the hit rather than from the damage event, because a fully blocked hit deals no damage at all. Falls count off the *base* damage, so a fall softened by Feather Falling is still a fall you walked away from.


### Bow nodes (Path of the Arrow)
The ranged class ([#27](../../issues/27)): bow, crossbow and everything that leaves your hand and travels. Its whole identity is **distance** — nodes get better the further the shot, and none of them makes close-range archery the answer, because that fight already belongs to the Sword tree. Seven tiers, like Sword and Armor: this tree carries **eleven mobs-only nodes**, and spreading them across seven columns keeps each tier's power step honest.

| Tier | Name | Nodes |
|---|---|---|
| 1 | Fletcher | Fletcher's Hands I, Swift Draw I, Quiver Sense, Arrow Recovery I |
| 2 | Marksman | Swift Draw II, Long Shot I, Steady Aim, Fletcher's Bench |
| 3 | Sharpshooter | Fletcher's Hands II, Gale I, Long Shot II, Ricochet I, Piercing Sight |
| 4 | Hawkeye | Rapid Reload I, Arrow Recovery II, Gale II, Multishot Focus, Alchemist's Quiver |
| 5 | Deadeye | Fletcher's Hands III, Long Shot III, Rapid Reload II, Pinning Shot |
| 6 | Windrunner | Swift Draw III, Ricochet II, Aerial Hunter, Endless Quiver |
| 7 | Eye of the Storm | **One** of Deadeye / Storm of Arrows / Hunter's Bounty |

**You move while you aim.** Vanilla slows an aiming archer to 20% walking speed, and that slowdown is most of why the bow feels planted. **Swift Draw I–III** lifts it to 40/60/80%, and **Rapid Reload I** removes it for the crossbow outright — load at full speed, sprint included. Both work in PvP on purpose: moving while aiming is visible and symmetric, the class's Nostalgy. Rapid Reload II goes further and loads a stowed crossbow by itself, one every five seconds, using real ammunition.

**The PvE-only rule, at range.** Distance is a sharper knife against players than against mobs, so this tree marks more nodes **mobs only** than any other — eleven: Long Shot I–III, Ricochet I–II, Piercing Sight, Multishot Focus, Alchemist's Quiver, Pinning Shot, Aerial Hunter and the Deadeye capstone. Same single gate as the Sword tree (`pvp_perks` flips them all), and the fact is on each node's card. Multishot Focus is the one that cannot be gated at fire time — a volley in flight has no target yet — so its converged side arrows are tagged and land at a third against players, making a focused volley on a player worth about one vanilla shot.

| Node | Tier | Effect |
|---|---|---|
| **Fletcher's Hands I–III** | 1/3/5 | Bow draw and crossbow load 20/40/60% faster |
| **Swift Draw I–III** | 1/2/6 | Move at 40/60/80% speed while aiming (vanilla: 20%) |
| **Quiver Sense** | 1 | The HUD names the arrow the bow will actually fire, and how many you carry |
| **Arrow Recovery I–II** | 1/4 | 25/50% of your arrows come back — including kills and terrain losses. Never a skeleton's |
| **Long Shot I–III** | 2/3/5 | Enchantment, bow + crossbow: +10/20/30% damage past 25 blocks. Mobs only |
| **Steady Aim** | 2 | Drawing while sneaking removes the bow's natural inaccuracy entirely |
| **Fletcher's Bench** | 2 | Arrow crafting yields double; chickens and parrots drop one more feather |
| **Gale I–II** | 3/4 | Enchantment, bow: arrow gravity cut 30/60% — a flatter arc is range *and* less lead to guess |
| **Ricochet I–II** | 3/6 | Enchantment, bow: an arrow that kills bounces to a second target (8/12 blocks, 50/75%). Mobs only |
| **Piercing Sight** | 3 | The mob you hit is outlined three seconds, health on the action bar. Mobs only |
| **Rapid Reload I–II** | 4/5 | Crossbow loads at full speed, sprint included; II loads a stowed crossbow by itself |
| **Multishot Focus** | 4 | Multishot's three arrows converge on one target. Mobs only |
| **Alchemist's Quiver** | 4 | Tipped-arrow effects last 50% longer. Mobs only |
| **Pinning Shot** | 5 | Enchantment, crossbow only: a hit roots the target 1.5 s. Mobs only |
| **Aerial Hunter** | 6 | +50% damage to airborne targets, and while you fly an Elytra. Mobs only |
| **Endless Quiver** | 6 | Infinity also covers spectral arrows, and shares a bow with Mending at the anvil |
| **Deadeye** | 7 | Capstone: a fully drawn shot landing past 50 blocks deals double damage. Mobs only |
| **Storm of Arrows** | 7 | Capstone: over-drawing banks up to two more arrows — real ammunition — released as one volley |
| **Hunter's Bounty** | 7 | Capstone: vanilla **Power** reaches VI at the table and the anvil — the Ancient Fortune trick |

**Damage attribution.** Arrow damage is applied by the projectile, not the player, so the whole tree reads the arrow at impact: the weapon it left travels on the arrow entity, and so does the point it was launched from — distance is measured from the *shot*, not from wherever the archer backpedalled to. An arrow already in flight when the tree changes is judged at impact, same as a sword changing mid-swing.

**Shared items grew their ranged branch.** A lent Long Shot III bow is not a hole: a bow carrying an unearned PB Enchantments enchantment skips the entire enchantment pass at impact — it *fires*, because a bow that refuses to shoot reads as a bug, but the arrow lands at bare-bow damage with no Power, no Flame, no Punch behind it. Partial ownership clamps rank by rank, exactly like tools.

**Gates.** The trackers the class needed and vanilla never had: arrows fired vs. landed (per projectile — a Multishot volley is honestly three), shot distance at impact, kills at 30+/60+ blocks, phantoms killed in the air, Multishot volley kills, and a checklist of tipped-arrow types fired.

### Enchanting table integration
- ✅ Unlocked enchantments join the enchanting table pool — **per player**: locked enchantments are filtered out of the roll before selection (no empty offers)
- ✅ Rolls above your unlocked level are clamped down, never hidden
- ✅ Vanilla **Fortune** and **Looting** are raised to a max of IV in the data pack, then clamped back to III at the table *and the anvil* for anyone who has not bought Ancient Fortune / Spoils of War — a data pack cannot be per-player, this is what makes the capstones a reward. Mending II is the same trick, one tree over
- ✅ **Sweeping Edge** is widened to axes in the data pack the same way, and refused at the table and the anvil to anyone without Broad Swing
- ✅ Natural combinations with vanilla enchantments via the vanilla bonus mechanic

### Librarian book trades
Enchanted books are the third way onto a tool, next to the enchanting table and the skill screen's Enchant button — and the two halves are deliberately asymmetric.

- ✅ **The villager offers them at any stage.** Trade generation never looks at your tree. A brand-new player can walk into a village on day one and see *Dig Range III* in a librarian's list. The offer is bait: it advertises what the tree holds.
- ✅ **Only an unlocked player can buy.** Taking the book is refused server-side unless you own that node at that rank or higher — no emerald is spent and no book is produced.
- ✅ **Refused, not clamped.** A *Dig Range III* book offered to someone who owns only rank I is blocked outright. The enchanting table clamps because the roll is invisible until it lands; a trade puts the rank and the price on the label, so quietly handing over a weaker book for the full price would be a bug.
- ✅ **No click-into-refusal.** The offer renders with vanilla's barred arrow and the book's tooltip names the rank you still owe.
- ✅ **One book per librarian.** The offer joins the vanilla *apprentice* pool, from which a librarian draws two trades for life — so mod books never crowd the vanilla book trades out of a village.
- ✅ **Data-driven.** The offer rolls a random enchantment at a random rank from `#toolmastery:trade_pool`; adding a new enchantment to the tree means one tag entry, not new trade code.

**Price.** 24 emeralds flat plus vanilla's own rank-scaled book price, so a rank I lands around 30 and a rank III up near the 64 ceiling. Books are deliberately expensive: emeralds are a currency the progression does not otherwise touch, and a cheap book would make the skill screen's Enchant button dead UI.

**The anvil.** There is no unlock check on the anvil, so a bought book can be applied to any tool and handed to anyone. That is fine, and intended: under *shared items* below the resulting tool is inert in unearned hands, so the book is a certificate that only works for someone who has done the work anyway.

### Shared items — gear only works as well as its holder has unlocked
A gear-rich player can hand a beginner a Dig Range III / Smelt III netherite pickaxe. Trading stays legal; what changes is that the item does not do the beginner's growing for them.

> Hold an item carrying an enchantment you have not unlocked, and it behaves as if your hands were empty.

- ✅ **The whole item goes inert, not just the enchantment.** A borrowed pickaxe digs at the bare-hand rate and stone drops nothing; a borrowed axe hits for bare-hand damage. The item is intact and keeps its enchantments — it is carried, not usable, and it becomes a goal in your inventory instead of a shortcut past ten hours of play.
- ✅ **Partial ownership clamps instead.** Own Dig Range I and hold a Dig Range III pickaxe and it works at rank I. The tool wakes up further as the tree catches up.
- ✅ **A locked item takes no durability damage.** You are not really using it, and it closes the angle where handing someone a tool burns it out for them.
- ✅ **Both sides agree.** The check runs on the client and the server from the same synced state, so blocks never "heal" mid-swing.
- ✅ **You are told.** The item's tooltip names the rank to unlock, and swinging one puts a rate-limited note on the action bar. Silent degradation reads as a bug.
- ✅ **Creative mode bypasses it**, so testing every other feature does not mean grinding the tree first.

Environmental modifiers still apply on top: a locked pickaxe swung underwater is as slow as bare hands underwater, not as fast as bare hands on dry land.

**Attack speed, now that a node depends on it.** Nostalgy is the mod's first enchantment that changes how *fast* you swing rather than how hard, so a lent Nostalgy IV sword would have handed a beginner 1.8 combat. It does not: every reader of the ladder asks `ItemAuthority.effectiveLevel`, which is 0 on an item its holder has not earned and clamped to their rank otherwise. The same is true of the trident, mace and spear nodes, each of which goes through a different damage path than the sword.

**Not covered.** Vanilla enchantments have no unlock concept, so a plain Efficiency V pickaxe still transfers freely, and vanilla's own attack-speed attribute is untouched. Armour and elytra are out of scope; bows grew their own branch with the Bow tree — an unearned bow still fires, at bare-bow damage with no enchantment effects. The check is written generally enough that "inert" can mean something different per item class.

### Safety & feel
- ✅ Sneak disables all area effects (Dig Range, Rich Vein, Logic)
- ✅ Speed passives are computed on both sides (client animation + server validation), so blocks never "heal" mid-swing
- ✅ Deep Haste never overrides a stronger Haste from a beacon or potion
- ✅ Tools never break themselves: area perks stop at 2 durability
- ✅ Nothing is spent on an impossible enchant: the item, the level and the conflicting enchantments are all checked before the levels leave your bar
- ✅ Tree detection requires leaves, with a 256-log flood-fill cap

## Extending the tree

Everything the skill screen draws comes off the data in
[`SkillTrees.java`](src/main/java/dev/pbenchants/skill/SkillTrees.java) — the GUI has no per-class code in it.

**A new node** is one line in its tree's node list, plus its lang keys:

```java
SkillNode.chained("smelt_4", 3, 12, "smelt_3", SkillType.ENCHANTMENT).icon(Items.MAGMA_BLOCK)
    .costing(mat(Items.BLAZE_ROD, 8))
    .enchantFor(60),
```

The screen picks it up on its own: the icon goes on the tile, the type decides the badge colour and the
stripe under the icon, and `requires` draws the connector back to `smelt_3`. Add
`node.pbenchants.smelt_4.desc` to `en_us.json` (and `node.pbenchants.smelt` once per family, for the
name). `.future()` marks a node that is designed but not built yet: it shows up starred and orange and
refuses to be unlocked. `.icon(...)` is optional — a node without one falls back to the first item of
its price. `.pve()` marks a node whose effect never applies to another player, which the card says out
loud; `.endOfTree()` marks the one node that opens only once everything else in its tree is bought.

**A new class** is a `SkillTree` and an entry in `SkillTrees.ORDER`:

```java
public static final SkillTree SWORD = new SkillTree("sword", Items.DIAMOND_SWORD, TIERS, NODES);
...
public static final List<SkillTree> ORDER = List.of(PICKAXE, AXE, ENCHANTER, EXPLORER, ARTISAN, SWORD);
```

That is the whole GUI side — the tab, its icon, the tier columns and the details panel all follow. Drop
the class from `SkillTrees.PLANNED` (the greyed "coming soon" tabs), add `tree.pbenchants.sword`,
`tree.pbenchants.sword.short` and one `tier.pbenchants.sword.<n>` per tier to `en_us.json`, and one
advancement JSON per tier under `data/toolmastery/advancement/sword/` if they should show up in the **L**
screen. A tree is as many tiers as its list is long — the Sword tree is seven, and the GUI reads the
count rather than assuming five. Gate counters still need a tracker feeding them and perks still need their own code — but
nothing about *displaying* the class does.

## Roadmap

**Seven-tier trees scroll.** A tier column never shrinks below a readable width, so the Sword and Armor trees are wider than the window and the tree pans sideways: the wheel over the tree, or the bar under it, whose thumb is as wide a share of the track as the viewport is of the tree. Five-tier trees are laid out exactly as they always were and show no bar at all.

See the [open issues](../../issues) — one issue per upcoming feature, including the remaining passive nodes, the Axe finishers (Everbloom, Bountiful Grove), and the one future class still greyed out in the tab strip (Rod).

## License

All rights reserved (private project).
