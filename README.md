# PB Enchantments Mod

**Possibly Better Enchantments** — a progressive quality-of-life skill tree mod for **Minecraft 26.2 (Java Edition)** built on **Fabric**.

Every tool class has its own skill tree. You earn access by **playing the class** (achievement gates), pay for unlocks with **XP levels**, and receive **real enchantments** that integrate with the enchanting table, anvil and `/enchant` — but only at the levels you have unlocked.

> Internal mod id: `toolmastery` · package `dev.toolmastery`

---

## How it works

1. **Play the class** — mining, chopping, smelting and crafting feed per-tier achievement counters.
2. **Complete the gate** — each tier requires class-specific achievements (e.g. *mine 500 stone*, *fell 100 trees with Logic*).
3. **Pay the access cost** — unlocking a tier consumes XP levels (5 / 10 / 15 / 20 / 30).
4. **Buy nodes** — each node has its own XP cost. Enchantment nodes are applied to your held tool instantly and become available in your enchanting table.

Every tier you unlock is also a **vanilla advancement**: open the advancements screen (**L**) and the *Tool Mastery* tab shows one branch per class, five tiers each — earned tiers lit up, the rest as goals ahead.

Progress is **per player**, persisted with the world, and survives death.

## Requirements

- Minecraft **26.2** (Java Edition)
- **Fabric Loader** ≥ 0.19.3 + **Fabric API** 0.157.0+26.2
- **Java 25**

## Download & install

There is no published release yet — build from source:

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
| Buy a node | Click the node → **Buy**, or `/mastery buy <tree> <node>` |
| Apply an unlocked enchantment | Automatic on purchase (held tool) · enchanting table · `/enchant @s toolmastery:<id> <level>` |

### Debug commands (operators)

```
/mastery debug maxall               # complete every gate + unlock every tier
/mastery debug buyall               # grant every node (applies enchants to held tool)
/mastery debug kit                  # full kit: one enchanted tool per tier of every tree
/mastery debug kit pickaxe          # tier kit for one tree
/mastery debug kit pickaxe melt     # one tool per level of a single enchantment
/mastery debug add <tree> <counter> <amount>   # bump a gate counter
```

---

## Implemented so far (Sprint 1 — Pickaxe & Axe)

### Core systems
- ✅ Per-player skill progress (tiers, nodes, counters) persisted via data attachments
- ✅ Achievement gates fed by live block-break tracking (ores, stone, logs, leaves, checklists as bitmasks)
- ✅ XP-level economy: tier access costs + per-node costs, validated server-side
- ✅ Capstone exclusivity (choose one of two tier-5 nodes)
- ✅ Skill tree GUI (key **K**): class tabs, tier columns, gate checklist panel, buy/unlock — future classes shown as *Coming soon*
- ✅ Every tier mirrored as a vanilla advancement in the **L** screen (*Tool Mastery* tab), with toast + chat announce on unlock
- ✅ Client–server sync via custom payloads; all actions validated on the server
- ✅ `/mastery` command suite with tab completion

### Real enchantments (data-driven, era 26.x)
| Enchantment | Tool | Levels | Effect |
|---|---|---|---|
| **Dig Range** | Pickaxe | I–III | Breaks extra blocks: below → cross → 3×3 on the facing plane |
| **Melt** | Pickaxe | I–III | Chance to smelt ore drops on the spot: 25% / 50% / 100% |
| **Rich Vein** | Pickaxe | I–II | Vein miner: up to 8 / 16 connected ores |
| **Logic** | Axe | I–III | Timber: slow cascade → instant → leaves too (requires real trees — log houses are safe) |
| **Environment** | Axe | I | Replants the sapling on the stump after a Logic III fell |
| **Magma Touch** | Pickaxe | capstone | Everything with a furnace recipe drops pre-smelted (tree-only, never in the table) |

### Enchanting table integration
- ✅ Unlockable enchantments join the enchanting table pool — **per player**: locked enchantments are filtered out of the roll before selection (no empty offers)
- ✅ Rolls above your unlocked level are clamped down, never hidden
- ✅ Natural combinations with vanilla enchantments via the vanilla bonus mechanic

### Safety & feel
- ✅ Sneak disables all area effects (Dig Range, Rich Vein, Logic)
- ✅ Tools never break themselves: area perks stop at 2 durability
- ✅ Tree detection requires leaves, with a 128-log flood-fill cap

## Roadmap

See the [open issues](../../issues) — one issue per upcoming feature, including the remaining passive/active nodes, custom items, gate counters for crafting/smelting, and the five future classes (Sword, Bow, Rod, Armor, Enchanter).

## License

All rights reserved (private project).
