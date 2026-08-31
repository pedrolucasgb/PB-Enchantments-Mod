---
name: lang-sync
description: Keep the PB Enchantments translations complete. Use whenever a feature adds, renames or removes user-facing text — new lang keys in en_us.json, new skill nodes, tiers, gates, commands or HUD strings — and before shipping any release jar. Ensures pt_br.json and es_es.json (plus the es_mx copy) carry every key en_us.json has.
---

# Lang Sync — every feature ships translated

PB Enchantments renders in the client's language. Three locales are maintained by
hand; everything else falls back to English automatically (vanilla behavior for
missing keys — never do anything special for other languages):

| File | Language |
|---|---|
| `src/main/resources/assets/pbenchants/lang/en_us.json` | English — the source of truth |
| `src/main/resources/assets/pbenchants/lang/pt_br.json` | Brazilian Portuguese |
| `src/main/resources/assets/pbenchants/lang/es_es.json` | Spanish |
| `src/main/resources/assets/pbenchants/lang/es_mx.json` | byte-for-byte copy of es_es.json (vanilla does NOT fall back es_mx→es_es, only →en_us) |

## The rule

**Any change to en_us.json must land in the same commit as the matching
change to pt_br.json and es_es.json, and es_mx.json must be refreshed as a
copy of es_es.json.** A key that exists only in en_us.json shows English to
Brazilian and Spanish players; a key that exists only in a translation is dead
weight. Neither ships.

## When adding a feature

1. Every user-facing string goes through a lang key — never a hardcoded
   English literal in Java (exception: `/mastery debug` op-only output may stay
   literal English).
2. Key conventions (follow the existing file): `screen.pbenchants.*` for GUI,
   `node.pbenchants.<id>` + `node.pbenchants.<fullId>.desc` for nodes,
   `tier.pbenchants.<tree>.<n>`, `tree.pbenchants.<id>` (+ `.short`),
   `gate.pbenchants.<counterId>` for gate achievement labels,
   `msg.pbenchants.*` for command/chat/screen feedback, `hud.pbenchants.*` for
   HUD lines, `material.pbenchants.tag.*`, `item.pbenchants.*`. Two legacy
   groups keep the old namespace forever because worlds persist ids that
   reference them: `advancements.toolmastery.*` and `enchantment.toolmastery.*`
   (plus the `item.toolmastery.death_note` / `item.toolmastery.biome_chart`
   duplicates for items already stamped with the old keys).
3. New `GateRequirement` ids need a `gate.pbenchants.<id>` entry in all three
   files — without one the HUD shows the raw title-cased id fallback.
4. Add the English text first, then translate it into pt_br.json and
   es_es.json **at the same position in the file** (all four files keep the
   same key order), then copy es_es.json over es_mx.json.

## Translation rules

- Placeholders `%s`, `%1$s`…`%4$s` are preserved verbatim — they may move
  within the sentence for natural word order, but never disappear, duplicate,
  or change number.
- Decorative glyphs (✓ □ ⚑ ⚗ ★ ✔ ✖ — •) stay.
- "PB Enchantments" is a brand — never translated. `/mastery` command text stays.
- "XP" stays "XP" in every language.
- Use official Minecraft vanilla terminology for each locale (pt_BR: bigorna,
  encantamento, Élitro, caixa de shulker, Sentinela; es: yunque, encantamiento,
  élitros, centinela…).
- Node/tier/tree/enchantment names are translated with flavor, like vanilla
  translates enchantment names — short and evocative, not word-by-word.

## Verify before committing (and before building any release jar)

Run the parity checker from the repo root:

```bash
pwsh -NoProfile -File .claude/skills/lang-sync/check-lang-parity.ps1
```

(on Windows PowerShell: `powershell -NoProfile -File .claude/skills/lang-sync/check-lang-parity.ps1`)

It fails the build if: a locale is missing a key en_us has, has a key en_us
does not, a value's placeholder set differs from English, or es_mx.json is not
identical to es_es.json. Fix every reported line, then re-run until clean.
