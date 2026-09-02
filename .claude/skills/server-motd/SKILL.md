---
name: server-motd
description: Update Pedro's home server for a new PB Enchantments version. Use whenever a version is about to be tagged (vX.Y.Z), right after the patch-notes skill runs for a version bump, or when asked to update the server MOTD, icon, or the mod jar on the server. Produces the exact motd= line for server.properties and the deploy checklist for the Crafty panel.
---

# Server MOTD — the join screen names the version, every tag

Since `0.8.2-beta` the mod refuses a client whose version does not match the
server's (`VersionGate`, configuration-phase handshake). The kick message is
the *last* resort; the MOTD in the server list is the warning a player reads
**before** clicking Join. Both have to move together on every release, and
the MOTD is the half that does not update itself.

## Where the server lives

- Panel: **Crafty Controller** at `https://192.168.0.250:8443/panel`
  (self-signed cert — the browser warning is expected).
- Claude never logs into the panel: entering the password is off-limits by
  policy. This skill's job is to hand Pedro the exact line and the exact
  steps, ready to paste. If the server folder is ever reachable as a local
  or network path, edit `server.properties` there directly instead.

## What to produce

Read `version=` from `gradle.properties` — never hard-code a version — and
emit this line, with `<version>` substituted:

```
motd=\u00A76PB Enchantments \u00A7a\u00A7lv<version>\n\u00A7eAtualize o mod antes de entrar!
```

Rules:

- `server.properties` is read as ISO-8859-1: every non-ASCII character goes
  in as a `\uXXXX` escape — `\u00A7` is the `§` of the color codes, `ã`
  would be `\u00E3`. Never paste a raw `§` or accented letter into the file.
- Two lines maximum (`\n` splits them), ~45 visible characters each — the
  server list truncates the rest.
- Color codes in use: `\u00A76` gold, `\u00A7a` green, `\u00A7l` bold,
  `\u00A7e` yellow. Keep the shape; the palette is settled.

## The checklist to hand over

1. Panel → the Minecraft server → **Files** → edit `server.properties` →
   replace the `motd=` line with the one produced above. Save.
2. Still in Files: upload `pbenchants-<version>.jar` (from `dist/`) into
   `mods/` and **delete the previous `pbenchants-*.jar`** — two copies of
   the mod id crash the server on boot.
3. **Restart the server** from the panel. MOTD and jar both load only at
   boot.
4. Remind: every player needs the same jar (the gate kicks by exact
   version). The download link is the GitHub Release the tag publishes.
5. `server-icon.png` (64x64 PNG, server root) is a one-time setup — check
   it exists, but only replace it when asked.

## Related

- `patch-notes` — runs first for the same tag; this skill is the "and tell
  the server" step of the same release.
