# PB Enchantments Mod

**Possibly Better Enchantments** — a progressive quality-of-life skill tree mod for **Minecraft 26.2 (Java Edition)** built on **Fabric**.

Every tool class has its own skill tree. You earn access by **playing the class** (achievement gates), pay for unlocks with **XP levels plus materials**, and receive **real enchantments** that integrate with the enchanting table, anvil and `/enchant` — but only at the levels you have unlocked.

> Internal mod id: `toolmastery` · package `dev.toolmastery`

---

## How it works

1. **Play the class** — mining, chopping, smelting and crafting feed per-tier achievement counters.
2. **Complete the gate** — each tier requires class-specific achievements (e.g. *mine 500 stone*, *fell 100 trees with Logic*).
3. **Pay the access cost** — unlocking a tier consumes XP levels (5 / 10 / 15 / 20 / 30).
4. **Unlock nodes** — a one-off purchase: a few XP levels plus materials. Passives switch on immediately; enchantments join your enchanting-table pool.
5. **Enchant your tools** — an unlocked enchantment can be stamped onto the item in your hand for whole XP levels, as often as you like.

### Unlock vs. Enchant

Every node is bought once with **Unlock** — cheap in XP, paid partly in materials you gathered while playing the class. What that buys depends on the node:

| Node type | What Unlock gives you | Second step? |
|---|---|---|
| Passive | The effect, live immediately | None — the unlock *is* the skill |
| Enchantment | The enchantment starts appearing in **your** enchanting table offers (per player), capped at the level you unlocked | Optional **Enchant** |

**Enchant** is the repeatable, guaranteed route: 20 levels for a rank I, 35 for a rank II, 50 for a rank III (40 for the single-level Indestructible) — all in whole XP levels, applied to whatever is in your main hand. It is priced well above a 30-level table roll on purpose: the enchanting table (which also throws in vanilla enchantments) stays the better deal for anyone willing to grind for it, and Enchant is what you pay when you want *this* enchantment on *this* tool right now.

Both buttons explain themselves before they charge you: clicking either swaps the details panel for a confirmation card listing the exact cost, the materials you are carrying, and what changes — then Confirm or Cancel.

Enchant refuses impossible combinations before spending anything, and says why: Smelt on an axe (wrong tool class), or an enchantment that conflicts with something already on the item (Silk Touch, say). The same check runs on the client, so the button is greyed out with the reason in its tooltip.

Every tier you unlock is also a **vanilla advancement**: open the advancements screen (**L**) and the *Tool Mastery* tab shows one branch per class, five tiers each — earned tiers lit up, the rest as goals ahead.

Progress is **per player**, persisted with the world, and survives death.

## Requirements

- Minecraft **26.2** (Java Edition)
- **Fabric Loader** ≥ 0.19.3 + **Fabric API** 0.157.0+26.2
- **Java 25**

## Download & install

### Quick download (no toolchain)

Grab [`Quick-Download/ToolMastery-Installer.zip`](Quick-Download/ToolMastery-Installer.zip), unzip it anywhere and double-click **Install Tool Mastery.bat**. It installs Fabric Loader and Fabric API if they are missing, removes any older Tool Mastery jar (two copies of one mod id crash the game) and drops the current one into `.minecraft/mods`. The same files sit loose in [`Quick-Download/`](Quick-Download) if you would rather read them first.

### From source

Build it yourself:

```bash
git clone https://github.com/pedrolucasgb/PB-Enchantments-Mod.git
cd PB-Enchantments-Mod
./gradlew build
```

The mod jar lands in `build/libs/toolmastery-<version>.jar`. Drop it into your `.minecraft/mods` folder (Fabric Loader + Fabric API required).

For development, `./gradlew runClient` launches a ready-to-play instance with the mod loaded. On Windows, make sure `JAVA_HOME` points to a JDK 25.

## Usage

| Action | How |
|---|---|
| Open the skill trees | Press **K** (rebindable) — or the `/mastery` command |
| Check progress / gates | `/mastery status <tree>` or click a tier header (T1–T5) in the GUI |
| Unlock the next tier | GUI button, or `/mastery unlock <tree>` |
| Unlock a node | Click the node → **Unlock** → Confirm, or `/mastery unlock <tree> <node>` |
| Enchant the held item | Click the node → **Enchant** → Confirm, or `/mastery enchant <tree> <node>` |
| Other ways in | Enchanting table (any unlocked enchantment) · `/enchant @s toolmastery:<id> <level>` |

### Debug commands (operators)

```
/mastery debug maxall               # complete every gate + unlock every tier
/mastery debug unlockall            # unlock every node (enchantments unlocked, not applied)
/mastery debug kit                  # full kit: one enchanted tool per tier of every tree
/mastery debug kit pickaxe          # tier kit for one tree
/mastery debug kit pickaxe smelt    # one tool per level of a single enchantment
/mastery debug unlocktier pickaxe 3 # gates + tier + every node of exactly that tier
/mastery debug unlocktier all 1     # ...for all three trees at once
/mastery debug add <tree> <counter> <amount>   # bump a gate counter

# ...and the same thing backwards, to re-test a feature from a clean slate
/mastery debug reset                # everything to zero: no tiers, no nodes, no counters
/mastery debug reset pickaxe        # wipe one tree only
/mastery debug tier pickaxe 2       # open exactly 2 tiers (re-locks nodes above)
/mastery debug lock pickaxe smelt_1 # re-lock one node, leaving tiers and counters alone
/mastery debug strip                # take every Tool Mastery enchantment off the held item
/mastery debug speed                # measure the speed passives on the held tool, in ticks
```

---

## Implemented so far (Sprint 1 — Pickaxe, Axe & Enchanter)

### Core systems
- ✅ Per-player skill progress (tiers, nodes, counters) persisted via data attachments
- ✅ Achievement gates fed by live block-break tracking (ores, stone, logs, leaves, checklists as bitmasks)
- ✅ Two-price economy: tier access costs, per-node unlock (XP levels + materials) and a repeatable per-node enchant price — all validated server-side
- ✅ Enchant-time compatibility checks (supported item + exclusive sets), mirrored on the client so the button explains itself before you spend
- ✅ Skill tree GUI (key **K**): class tabs, tier columns, gate checklist panel, material checklist, unlock/enchant with a confirmation card — future classes shown as *Coming soon*
- ✅ Every tier mirrored as a vanilla advancement in the **L** screen (*Tool Mastery* tab), with toast + chat announce on unlock
- ✅ Client–server sync via custom payloads; all actions validated on the server
- ✅ `/mastery` command suite with tab completion

### Real enchantments (data-driven, era 26.x)
| Enchantment | Tool | Levels | Effect |
|---|---|---|---|
| **Dig Range** | Pickaxe | I–III | Breaks extra **pickaxe-mineable** blocks of **about the same hardness**: below → cross → 3×3 on the facing plane. Dirt and wood are skipped; stone next to obsidian leaves the obsidian standing — until your mining speed makes both near-instant |
| **Smelt** | Pickaxe | I–III | Chance to smelt ore drops on the spot: 25% / 50% / 100% |
| **Rich Vein** | Pickaxe | I–II | Vein miner: up to 8 / 16 connected ores |
| **Logic** | Axe | I–III | Timber: fells the whole tree in one swing at every level — I pays for it with a slower chop, III clears the canopy too (requires real trees — log houses are safe) |
| **Environment** | Axe | I | Replants the sapling on the stump after a Logic III fell |
| **Indestructible** | Enchanter, any damageable item | I | The item never breaks — damage stops one point short, like an Elytra. Spent, it works like an empty hand until repaired |

### Passive skills (pickaxe)
| Node | Levels | Effect |
|---|---|---|
| **Mason's Grip** | I–III | +20% / +40% / +60% mining speed on stone, deepslate and every ore |
| **Miner's Magnet** | — | Pickaxe drops go straight to the inventory (composes with Dig Range, Rich Vein and Smelt) |
| **Deep Haste** | — | Permanent Haste I below Y = 0 |
| **Obsidian Breaker** | — | Obsidian and crying obsidian break 50% faster |
| **Ancient Fortune** | capstone | Lifts your ceiling on vanilla Fortune from III to IV — your enchanting table can offer Fortune IV, nobody else's can |

### Axe passives (Path of the Grove)
| Node | Tier | Effect |
|---|---|---|
| **Lumberjack's Arms** I–III | 1 / 2 / 3 | Axes chop wood +25% / +50% / +75% faster |
| **Logger's Magnet** | 1 | Blocks chopped with an axe go straight into your inventory, Logic fells included |
| **Fair Harvest** | 1 | +25% sapling chance from every leaf block you break, on top of the vanilla roll |
| **Pruner** | 3 | Leaves broken with an axe drop double loot (apples, saplings, sticks) |
| **Double Axe** I–II | 3 / 4 | 10% / 20% chance for a log to drop twice — every log of a Logic fell rolls on its own |
| **Shield Breaker** | 3 | Axes disable a blocking shield 2s longer and push 2 more damage through it |

### Enchanter passives (Path of the Arcane)
| Node | Tier | Effect |
|---|---|---|
| **Arcane Insight** I–III | 1 / 2 / 3 | A panel beside the enchanting table reveals the *true* enchantments behind offer 1 / offers 1–2 / all three |
| **Scholar** I–III | 1 / 2 / 4 | +20% / +40% / +60% XP from every source (orbs, bottles, smelting, breeding) |
| **Inner Focus** | 3 | Enchanting no longer requires nor consumes lapis lazuli |
| **Indestructible** | 3 | Enchantment: the item never breaks (see the enchantment table above) |

### Enchanting table integration
- ✅ Unlocked enchantments join the enchanting table pool — **per player**: locked enchantments are filtered out of the roll before selection (no empty offers). Capstones are excluded by design: the tree is their only source
- ✅ Rolls above your unlocked level are clamped down, never hidden
- ✅ Natural combinations with vanilla enchantments via the vanilla bonus mechanic

### Safety & feel
- ✅ Sneak disables all area effects (Dig Range, Rich Vein, Logic)
- ✅ Speed passives are computed on both sides (client animation + server validation), so blocks never "heal" mid-swing
- ✅ Deep Haste never overrides a stronger Haste from a beacon or potion
- ✅ Tools never break themselves: area perks stop at 2 durability
- ✅ Nothing is spent on an impossible enchant: the item, the level and the conflicting enchantments are all checked before the levels leave your bar
- ✅ Tree detection requires leaves, with a 256-log flood-fill cap

## Roadmap

See the [open issues](../../issues) — one issue per upcoming feature, including the remaining passive/active nodes, custom items, gate counters for crafting/smelting, and the four future classes (Sword, Bow, Rod, Armor).

## License

All rights reserved (private project).
