TOOL MASTERY - a mod for Minecraft 26.2 (Fabric)
Version 0.3.0
================================================

HOW TO INSTALL
--------------
1. Have Minecraft Java Edition installed (official launcher) and run it at
   least once.
2. Double-click "Install Tool Mastery.bat".
   The installer will automatically:
     - install Fabric Loader (if you do not have it yet);
     - download Fabric API (if you do not have it yet);
     - remove an older Tool Mastery version, if there is one;
     - copy the Tool Mastery mod into your mods folder.
3. Open the Minecraft launcher, pick the "fabric-loader-26.2" profile and
   hit Play.
4. In game, press K to open the skill trees.

HOW IT WORKS
------------
Every skill in the tree has up to TWO buttons:

  UNLOCK  - a one-off purchase: a few XP levels plus materials you gather
            by playing the class. A passive switches on immediately. An
            enchantment starts showing up at your enchanting table.

  ENCHANT - repeatable: costs whole XP levels and applies the enchantment
            to the item in your hand. Use it as often as you like, on as
            many tools as you like.

Enchant is deliberately pricier than the enchanting table (20 levels for
rank I, 35 for II, 50 for III): the table stays the cheap route, Enchant is
the guaranteed one.

Both buttons explain themselves before charging you. Clicking either turns
the panel into a confirmation card with the exact cost, the materials you
are carrying and what changes. Nothing is spent until you press Confirm.

Enchant also checks whether it can work BEFORE spending a single level: the
wrong tool (Smelt on an axe) or a conflicting enchantment greys the button
out, with the reason in the tooltip.

Each rank of a skill describes only what that rank does - Dig Range II
describes the cross, not the whole family.

WHAT IS NEW IN 0.3.0
--------------------
- TWO NEW CLASSES, so the tab strip at the top of the skill screen now has
  five playable trees instead of three.

- EXPLORER (Path of the Horizon) - the first class that is not tied to a
  tool. It levels from MOVEMENT: how far you have walked, sprinted, swum,
  rowed and flown, how many biomes you have stood in, how many dimensions
  and structures you have seen. Distance is read from the statistics the
  game already keeps, counted from the moment you install the mod, so an
  old world does not hand you tier 4 on day one.
    Cartographer  - a new biome tells you its name and your coordinates
    Tireless      - moving costs 20/40/60% less hunger
    Sea Legs      - boats you steer cruise about 15% faster
    Clear Sight   - underwater fog opens up; rank II also refills breath
    Slipstream    - ENCHANTMENT for the Elytra: 10/25/50% of a firework's
                    push carries over. Same rocket, more distance.
    Remember      - you respawn holding a note with the coordinates,
                    dimension and day of your last death
    Trailblazer   - a long unbroken sprint ramps up to +12% speed
    Soft Landing  - half damage from flying into a wall, and the first 3
                    blocks of any fall are free
    Waypoint      - sneak + right-click a compass to bind a spot; the
                    needle points there from then on
    Capstone, pick one: ENDLESS HORIZON (a quarter of your fireworks are
    not consumed and Slipstream doubles) or WORLD'S MEMORY (once a day,
    right-click a compass for the bearing to the nearest structure of a
    kind you have already found).

- ARTISAN (Path of Order) - storage and inventory quality of life, earned
  instead of installed. Its buttons are small symbols in the top-right
  corner of the inventory and chest windows - hover one to see its name.
  They appear only once you have bought them.
    Sorter's Hand      - an S button, first for your bag, then for chests
    Seeker's Eye       - the magnifier: type a word and every matching slot
                         turns yellow, like CTRL+F. First your bag, then
                         the open chest too, remembering what you typed
                         from one chest to the next
    Steady Grid        - the crafting grid keeps its contents when you
                         close the table
    Deft Hands         - an empty hotbar slot refills itself from your bag
    Locked Slots       - ALT-CLICK any slot to pin it. Nothing the mod does
                         will ever move a pinned slot.
    Tidy Storage       - a chest is tidied again every time you close it
    Artisan's Order    - choose category / name / count as the sort rule
    Quartermaster's Call - tops up the stacks you already carry from
                         nearby chests
    Capstone: HAND OF ORDER - Terraria's Quick Stack. One button and
    everything in your bag flies to the nearby chest that ALREADY keeps
    that kind of thing - oak planks join the chest of birch and spruce,
    ruby ingots join the chest of iron and gold. It never starts a pile of
    its own, so anything without a home stays on you. Hotbar, armour and
    pinned slots are never touched, and furnaces, hoppers and ender chests
    are never filled.

- NOT BUILT YET, shown starred and orange in the tree: Night Eyes,
  Master's Batch and the rival capstone Craft from Storage.
WHAT IS NEW IN 0.2.0
--------------------
- LIBRARIANS SELL TOOL MASTERY BOOKS (new): from apprentice onwards a
  librarian may offer one Tool Mastery enchanted book, whatever stage you
  are at - a brand new player can see Dig Range III on day one. Buying it
  is another matter: the trade is refused unless you have already unlocked
  that skill at that rank or higher. The offer shows up barred and the book
  tooltip tells you what you still owe, so you never click into a refusal
  and never lose an emerald to one. Books are expensive on purpose - around
  30 emeralds for a rank I, near 64 for a rank III - so the Enchant button
  stays worth using.

- BORROWED GEAR ONLY WORKS AS WELL AS YOU HAVE EARNED (new): hold a tool
  carrying an enchantment you have not unlocked and it behaves as if your
  hands were empty - bare-hand speed, blocks that need a tool drop nothing,
  bare-hand damage. The tool is not damaged or changed in any way; it just
  sits there as a goal until your tree catches up, and it does not even
  lose durability while inert. If you own a LOWER rank than the tool
  carries - your Dig Range I against a Dig Range III pickaxe - it works at
  your rank instead, and improves as you buy the next one. The tooltip
  names what to unlock. Creative mode ignores all of this.

- THE SKILL SCREEN (K) IS A REAL TREE NOW: every skill is an item icon
  in a coloured frame - green is already yours, a pulsing gold one is
  ready to unlock, grey is still out of reach, orange is not built yet.
  Lines join a rank to the rank that unlocks it (Smelt I - II - III), the
  columns are headed by the tier name instead of T1-T5, each skill shows
  a badge saying whether it is a passive or an enchantment, and your XP
  bar runs along the bottom of the screen.

- INDESTRUCTIBLE (new, Enchanter tier 3, unlocked with phantom membrane):
  the item never breaks. Damage stops one point short of the end, like an
  Elytra. While it sits there spent it works like an empty hand - bare-hand
  speed, and blocks that need a tool drop nothing - until you repair it.

- ANCIENT FORTUNE (pickaxe capstone) is a PASSIVE now: it lifts the Fortune
  ceiling from III to IV. From then on YOUR enchanting table can offer
  Fortune IV.

- MASON'S GRIP and LUMBERJACK'S ARMS hit harder:
  +20/40/60% mining, +25/50/75% chopping.
  (The old values worked, they were just too small to feel.)

- DIG RANGE only breaks pickaxe-mineable blocks of roughly the same
  hardness as the one you hit. Never dirt, gravel or wood. Stone next to
  obsidian leaves the obsidian standing - unless your mining speed makes
  both of them near-instant.

- MELT is now SMELT. Skills you already bought stay bought, but tools that
  were already enchanted with Melt lose the enchantment when the world
  loads - just re-apply it with the Enchant button.

- REMOVED: the Magma Touch enchantment and every ACTIVE skill, including
  the Reroll button on the enchanting table.

IMPORTANT
---------
- The mod only works on Minecraft 26.2.
- "install.ps1" and "toolmastery-0.3.0.jar" have to stay in the same folder
  as the .bat.
- You need an internet connection the first time (to download Fabric Loader
  and Fabric API).
- If you already had an older version installed, the installer deletes the
  old jar for you. Never leave two versions of the mod in the mods folder.

COMMON PROBLEMS
---------------
- "Java not found": launch Minecraft 26.2 once from the official launcher
  (it downloads Java by itself) and run the installer again.
- The game crashes on launch: check for old mods from other versions in the
  mods folder (%appdata%\.minecraft\mods).
- The mod does not show up in game: make sure the profile selected in the
  launcher is "fabric-loader-26.2" and not the normal one.
- "I broke a block and nothing dropped": that is most likely Miner's
  Magnet, which sends drops straight to your inventory instead of letting
  them fall. Check your bag.
