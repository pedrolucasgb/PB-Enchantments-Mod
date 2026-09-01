---
name: patch-notes
description: Write the trilingual patch notes that ship with every PB Enchantments release. Use whenever a version is about to be tagged (vX.Y.Z), when the version in gradle.properties is bumped, or when asked what changed in a release. Produces docs/patchnotes/<version>.md in English, Portuguese and Spanish, which the landing page renders and the release workflow attaches to the GitHub Release.
---

# Patch Notes — every tag ships a changelog, in three languages

Since `0.7.1-beta`, **no version is tagged without notes.** One file per
released version:

```
docs/patchnotes/<version>.md      e.g. docs/patchnotes/0.7.1-beta.md
```

`<version>` is the tag with the leading `v` stripped — tag `v0.7.1-beta` →
file `0.7.1-beta.md`. It must match `version=` in `gradle.properties`.

Two things read that file, and neither needs anything else:

- **The landing page** (`docs/index.html`) fetches `patchnotes/<version>.md`
  relative to itself — same-origin on GitHub Pages, so no CORS and no API
  budget. It renders the "What is new" section from the newest release, and
  every row in **All versions** can expand its own notes.
- **The release workflow** (`.github/workflows/release.yml`) attaches the file
  to the GitHub Release next to the jar, so the notes travel with the download.

A version with no file is not an error: the page says so politely and points at
the GitHub release. That is the graceful path for everything before
`0.7.1-beta`, not a licence to skip one.

## The format

The whole grammar, and deliberately no more than this:

```markdown
# 0.7.1-beta
<!-- released: 2026-09-01 -->

## en

One or two sentences of lead-in, optional. Plain prose, no bullet.

### New
- **The headline, as a sentence.** Then what it means for the player.
- **A second one.** ...

### Fixed
- **What was broken, from the player's side.** What it does now.

### Changed
- ...

## pt

...same shape, in Portuguese, with translated group headings...

## es

...same shape, in Spanish...
```

Rules the parser actually enforces:

| Line | Meaning |
|---|---|
| `# <anything>` | version heading — decorative, ignored |
| `<!-- released: YYYY-MM-DD -->` | the date shown next to the version |
| `## en` / `## pt` / `## es` | starts a language block (exactly these three codes) |
| `### <heading>` | a group inside the current block; written **in that language** |
| `- <text>` | a bullet; `**bold**` and `` `code` `` render, nothing else does |
| anything else before the first `###` | the lead paragraph |
| a wrapped line | continues the bullet or paragraph above it |

Wrap prose at ~80 columns; wrapped lines are rejoined with a single space, so
line breaks inside a bullet are free.

Group headings are conventionally **New / Fixed / Changed**, translated:

| en | pt | es |
|---|---|---|
| New | Novidades | Novedades |
| Fixed | Correções | Correcciones |
| Changed | Mudanças | Cambios |
| Removed | Removido | Eliminado |

Use only the groups that have entries. Order them New → Fixed → Changed.

## How to write them

These are read by players, not by reviewers. The rules that matter:

1. **Lead with what the player sees**, not with the class you touched.
   "Searching for anything with an E in it closed the inventory" — not
   "`AbstractContainerScreen.keyPressed` no longer falls through".
2. **Bold the headline sentence** of each bullet, then explain. One bullet per
   user-visible change; internal refactors get no bullet.
3. **Say what still does not happen** when a fix could be misread. "Leaves that
   rot away on their own still do not count" saves a bug report.
4. **Say when saves are affected** — migrations, resets, anything a world
   carries. Never let a player discover that from the game.
5. Keep the mod's own vocabulary: node, tier, gate, skill tree, and the
   in-game names of perks as the lang files translate them. Cross-check
   `src/main/resources/assets/pbenchants/lang/<locale>.json` for the exact
   name a player sees — a patch note that invents its own name for a node is
   worse than one in English.
6. **PB Enchantments** is never translated. `XP` stays `XP`. Commands
   (`/pbenchants`), keys (`K`, `Esc`, `Q`) and filenames go in `` `code` ``.
7. Every bullet exists in all three languages, in the same order. A bullet in
   one language only is a bug the checker will catch.

## Before tagging

1. Bump `version=` in `gradle.properties`.
2. Write `docs/patchnotes/<version>.md`.
3. Mirror the highlights into `Quick-Download/README.txt` under a
   `WHAT IS NEW IN <version>` heading (that file is English-only, plain ASCII,
   and ships inside the installer zip).
4. Run the checker from the repo root:

   ```bash
   powershell -NoProfile -File .claude/skills/patch-notes/check-patch-notes.ps1
   ```

   It fails if the file for the current `gradle.properties` version is missing,
   if a language block is absent or empty, if the three blocks disagree on how
   many bullets they carry, or if a `released:` date is missing or malformed.
5. Commit, merge, then push the tag:

   ```bash
   git tag v<version> && git push origin v<version>
   ```

The workflow builds the jar from that commit and publishes both it and the
notes file. The landing page needs no deploy — it reads the new file the moment
`main` moves.

## Related

- `lang-sync` — the same discipline for the in-game strings. A release that
  adds player-facing text needs both.
