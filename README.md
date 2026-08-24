# PB Enchantments Mod

**Possibly Better Enchantments** — a progressive quality-of-life skill tree mod for **Minecraft 26.2 (Java Edition)** built on **Fabric**.

Every tool class has its own skill tree. You earn access by **playing the class** (achievement gates), pay for unlocks with **XP levels plus materials**, and receive **real enchantments** that integrate with the enchanting table, anvil and `/enchant` — but only at the levels you have unlocked.

> Version **0.2.0** · internal mod id: `toolmastery` · package `dev.toolmastery`

---

## How it works

1. **Play the class** — mining, chopping, smelting and crafting feed per-tier achievement counters.
2. **Complete the gate** — each tier requires class-specific achievements (e.g. *mine 500 stone*, *fell 100 trees with Logic*).
3. **Pay the access cost** — unlocking a tier consumes XP levels (5 / 10 / 15 / 20 / 30).
4. **Unlock nodes** — a one-off purchase: a few XP levels plus materials. Passives switch on immediately; enchantments join your enchanting-table pool.
5. **Enchant your tools** — an unlocked enchantment can be stamped onto the item in your hand for whole XP levels, as often as you like.
6. **Or buy the book** — librarians sell Tool Mastery enchanted books to anyone who has already unlocked the rank on the label.

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
| Check progress / gates | `/mastery status <tree>` or click a tier header in the GUI |
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
- ✅ Skill tree GUI (key **K**): class tabs with class icons, one column per named tier, item-icon nodes coloured by state, prerequisite connectors, type badges, gate and material checklists, unlock/enchant with a confirmation card, and the player XP bar along the bottom — future classes shown as *Coming soon*
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

### Enchanter nodes (Path of the Arcane)
| Node | Tier | Effect |
|---|---|---|
| **Arcane Insight** I–III | 1 / 2 / 3 | A panel beside the enchanting table reveals the *true* enchantments behind offer 1 / offers 1–2 / all three |
| **Scholar** I–III | 1 / 2 / 4 | +20% / +40% / +60% XP from every source (orbs, bottles, smelting, breeding) |
| **Inner Focus** | 3 | Enchanting no longer requires nor consumes lapis lazuli |
| **Indestructible** | 3 | Enchantment: the item never breaks (see the enchantment table above) |

### Enchanting table integration
- ✅ Unlocked enchantments join the enchanting table pool — **per player**: locked enchantments are filtered out of the roll before selection (no empty offers)
- ✅ Rolls above your unlocked level are clamped down, never hidden
- ✅ Vanilla **Fortune** is raised to a max of IV in the data pack, then clamped back to III at the table for anyone who has not bought Ancient Fortune — a data pack cannot be per-player, this is what makes the capstone a reward
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

**Not covered.** Vanilla enchantments have no unlock concept, so a plain Efficiency V pickaxe still transfers freely. Attack *speed* is left vanilla — only damage is suppressed — because it is read in many places including the client cooldown bar, and no weapon-tree enchantment exists yet to make the difference felt. Armour, bows and elytra are out of scope; the check is written generally enough that "inert" can mean something different per item class later.

### Safety & feel
- ✅ Sneak disables all area effects (Dig Range, Rich Vein, Logic)
- ✅ Speed passives are computed on both sides (client animation + server validation), so blocks never "heal" mid-swing
- ✅ Deep Haste never overrides a stronger Haste from a beacon or potion
- ✅ Tools never break themselves: area perks stop at 2 durability
- ✅ Nothing is spent on an impossible enchant: the item, the level and the conflicting enchantments are all checked before the levels leave your bar
- ✅ Tree detection requires leaves, with a 256-log flood-fill cap

## Extending the tree

Everything the skill screen draws comes off the data in
[`SkillTrees.java`](src/main/java/dev/toolmastery/skill/SkillTrees.java) — the GUI has no per-class code in it.

**A new node** is one line in its tree's node list, plus its lang keys:

```java
SkillNode.chained("smelt_4", 3, 12, "smelt_3", SkillType.ENCHANTMENT).icon(Items.MAGMA_BLOCK)
    .costing(mat(Items.BLAZE_ROD, 8))
    .enchantFor(60),
```

The screen picks it up on its own: the icon goes on the tile, the type decides the badge colour and the
stripe under the icon, and `requires` draws the connector back to `smelt_3`. Add
`node.toolmastery.smelt_4.desc` to `en_us.json` (and `node.toolmastery.smelt` once per family, for the
name). `.future()` marks a node that is designed but not built yet: it shows up starred and orange and
refuses to be unlocked. `.icon(...)` is optional — a node without one falls back to the first item of
its price.

**A new class** is a `SkillTree` and an entry in `SkillTrees.ORDER`:

```java
public static final SkillTree SWORD = new SkillTree("sword", Items.DIAMOND_SWORD, TIERS, NODES);
...
public static final List<SkillTree> ORDER = List.of(PICKAXE, AXE, ENCHANTER, SWORD);
```

That is the whole GUI side — the tab, its icon, the tier columns and the details panel all follow. Drop
the class from `SkillTrees.PLANNED` (the greyed "coming soon" tabs), add `tree.toolmastery.sword`,
`tree.toolmastery.sword.short` and `tier.toolmastery.sword.1`-`.5` to `en_us.json`, and the five
advancement JSONs under `data/toolmastery/advancement/sword/` if the tiers should show up in the **L**
screen. Gate counters still need a tracker feeding them and perks still need their own code — but
nothing about *displaying* the class does.

## Roadmap

See the [open issues](../../issues) — one issue per upcoming feature, including the remaining passive nodes, custom items (Miner's Helm, Rich Bark, Arcane Tome), gate counters for crafting and smelting, and the four future classes (Sword, Bow, Rod, Armor).

Known gap: the Fortune IV ceiling is gated at the enchanting table but not at the anvil, so combining two Fortune III books can still reach IV without the capstone.

## License

All rights reserved (private project).
