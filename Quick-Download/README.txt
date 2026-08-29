TOOL MASTERY - a mod for Minecraft 26.2 (Fabric)
Version 0.5.0
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

WHAT IS NEW IN 0.5.0
--------------------
- THE BOW CLASS (Path of the Arrow) - the ranged tree, seven tiers like
  the Sword and the Armor. Everything about it is DISTANCE: nodes get
  better the further the shot, and none of them makes point-blank archery
  the answer. It levels from arrows fired, arrows landed, kills at 30+
  and 60+ blocks, phantoms shot out of the sky, Multishot volley kills
  and the tipped arrows you have fired.

- YOU MOVE WHILE YOU AIM. Vanilla slows an aiming archer to 20% walking
  speed - that crawl is most of why the bow feels planted.
    Swift Draw I-III - move at 40/60/80% speed while drawing a bow or
                       loading a crossbow
    Rapid Reload I   - the crossbow loads at FULL speed, sprint included
    Rapid Reload II  - a crossbow stowed in your inventory loads itself,
                       one every five seconds, using real ammunition
  Both work in PvP on purpose: moving while aiming is visible and
  symmetric, the class's Nostalgy.

- The rest of the quiver:
    Fletcher's Hands I-III - bow draw and crossbow load 20/40/60% faster
    Quiver Sense    - the HUD names the arrow the bow will ACTUALLY fire,
                      and how many of it you carry
    Arrow Recovery  - 25/50% of your arrows come back, even from kills
                      and terrain. Never a skeleton's
    Long Shot I-III - ENCHANTMENT, bow + crossbow: +10/20/30% damage past
                      25 blocks. Worth nothing in a corridor
    Steady Aim      - drawing while sneaking removes the bow's natural
                      inaccuracy entirely
    Fletcher's Bench- arrow crafting yields double; chickens and parrots
                      drop one more feather
    Gale I-II       - ENCHANTMENT, bow: arrow gravity cut 30/60% - a
                      flatter arc is range AND less lead to guess
    Ricochet I-II   - ENCHANTMENT, bow: an arrow that kills bounces to a
                      second target (8/12 blocks, 50/75% damage)
    Piercing Sight  - the mob you hit is outlined for three seconds, with
                      its health on the action bar
    Multishot Focus - Multishot's three arrows converge on ONE target
    Alchemist's Quiver - tipped-arrow effects last 50% longer
    Pinning Shot    - ENCHANTMENT, crossbow only: a hit roots the target
                      for 1.5 seconds
    Aerial Hunter   - +50% damage to airborne targets, and while YOU are
                      flying an Elytra
    Endless Quiver  - Infinity also covers spectral arrows, and finally
                      shares a bow with Mending at the anvil

- PICK ONE OF THREE at the end of the tree, and only one:
    Deadeye         - a fully drawn shot landing past 50 blocks deals
                      DOUBLE damage
    Storm of Arrows - holding the draw past full charge banks up to two
                      more arrows - real ammunition - released as a volley
    Hunter's Bounty - vanilla POWER reaches VI, at the table and the anvil

- MOBS ONLY, at range. Distance is a sharper knife against players than
  against mobs, so eleven of the class's nodes never fire at a player:
  Long Shot, Ricochet, Piercing Sight, Multishot Focus, Alchemist's
  Quiver, Pinning Shot, Aerial Hunter and Deadeye. The card says so in
  the skill screen, and config/toolmastery.json flips them for servers
  that disagree.

- A BORROWED BOW IS INERT: carrying an unearned Tool Mastery enchantment,
  it still fires - a bow that refuses to shoot reads as a bug - but the
  arrow lands at bare-bow damage with no enchantment effects behind it.

WHAT IS NEW IN 0.4.0
--------------------
- THE SWORD CLASS (Path of the Blade) - the combat tree, and the first one
  that is SEVEN tiers instead of five. It covers every weapon that hits:
  sword, trident, mace, axe-as-weapon and the new spear. It levels from
  fighting - kills, damage dealt, the species you have faced, mace slams,
  kills made at under three hearts.
    Keen Edge      - swords hit harder the fuller the attack cooldown was
    Combat Magnet  - drops and XP from your kills fly to you
    Butcher's Cut  - mobs killed with a sword drop twice the food
    Sweeping Arc   - the sweep hits harder and reaches further
    Broad Swing    - axes sweep too, and accept vanilla Sweeping Edge
    Second Wind    - every kill puts a point of saturation back
    Hunter's Mark  - the mob you last hit is outlined, with its health shown
    Executioner    - more damage against anything nearly dead
    Tidecaller     - the trident comes back without Loyalty
    Riposte        - a shield raised into the swing throws the hit back
    Gravity Well   - a mace smash converts more of your fall
    Phalanx        - a spear reaches further, and pricks what leans on you
    Cleave         - an axe hit splashes onto the mob next door
    Adrenaline     - a long fight ramps your damage up
    Storm Bearer   - Channeling without waiting for a thunderstorm
    Sundering Blow - ignores part of the target's armour
    Bloodthirst    - kills in quick succession stack damage
    Headhunter     - mob heads actually drop

- NOSTALGY - a four-rank enchantment that shortens the attack cooldown
  25%, 50%, 75% and finally removes it: 1.8 combat, back on a modern
  client. It is the one node in the class built to be felt in PvP.

- DEATH EYES - the end of the tree, and it only opens once every other
  node in it is bought. You see every mob as one of the dead, so SMITE
  applies in full to everything that is not a player.

- MOBS ONLY. Armour penetration, execute damage and stacking damage per
  kill are fine against a zombie and would rewrite PvP, so those nodes
  simply do not fire at a player. The skill screen says so on the card.
  config/toolmastery.json has the two switches if your server disagrees.

- THE ARMOR CLASS (Path of the Bulwark) - the class that cannot level from
  doing something, because wearing armour is not an action. It levels from
  DAMAGE SURVIVED: what your set absorbed, what your shield stopped, what
  you walked away from - which makes it the only tree that advances while
  you play every other one. Seven tiers, like the Sword.
    Padded Lining   - armour takes 15/30/45% less durability damage
    Set Sense       - your real armour, toughness and damage reduction,
                      drawn above the armour bar that hides them
    Shield Wall     - the shield is up the instant you raise it; II widens
                      the arc it covers
    Steady Stance   - 25% less knockback from mobs
    Flashpoint      - touching lava buys you ten seconds of immunity to it,
                      and only rearms once you are out and no longer burning
    Thermal Weave   - ENCHANTMENT: much less fire and lava damage, and it
                      adds to Fire Protection instead of replacing it
    Sure Footing    - nothing underfoot slows you down, plus half Depth
                      Strider and half Soul Speed without the enchantments
    Second Skin     - a piece never spends its last durability point
    Ablative Plating- ENCHANTMENT: explosion damage -20/-40%, and a creeper
                      no longer throws you at all
    Bulwark         - ENCHANTMENT for the shield: 25/50/75% less durability,
                      and at III an axe cannot disable it any more
    Thorned Plate   - ENCHANTMENT: Thorns with no durability cost
    Last Stand      - below four hearts, Resistance for five seconds
    Repair Rites    - armour mends itself while you stand still, unhurt
    Kinetic Plating - ENCHANTMENT for boots: six free blocks of fall, and
                      the rest hurts half as much
    Guardian's Aura - everyone near you takes 10% less damage. The first
                      node in the mod whose value is entirely for others
    Warden's Weight - nothing knocks you back while you are blocking
    Nightplate      - a full set of one material carries its own bonus

- PICK ONE OF THREE at the end of the Armor tree, and only one:
    Aegis         - your ceiling on vanilla PROTECTION goes from IV to V
    Immortal Line - a killing blow leaves you on one heart instead, once
                    every ten minutes, and it costs no totem
    Living Armor  - experience you pick up mends the whole set at once,
                    from any source, with no Mending on the pieces

- THE SKILL SCREEN SCROLLS SIDEWAYS. The two seven-tier trees are wider
  than the window, so the tree pans: the wheel over it, or the bar under
  it. Five-tier trees look exactly as they always did.

- SHIELD BREAKER MOVED from the Axe tree to the Sword tree, where a PvP
  node belongs. If you already bought it, you keep it - it moves with you.

- LOOTING IV, the sword capstone, built like Fortune IV. Both are now
  gated at the anvil as well as the enchanting table, which closes the
  old hole where two Fortune III books reached IV without the capstone.

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
    Night Eyes    - a small brightness lift above what the video settings
                    allow: dark corners read deep blue instead of black,
                    daylight is untouched, and a warden still blinds you
    Pufferfish Lungs - permanent Water Breathing. Your breath meter never
                    moves again, in any water, in any dimension
    Capstone: ENDLESS HORIZON - a quarter of the fireworks you burn while
    flying are not consumed, and your Slipstream carryover doubles.

- ARTISAN (Path of Order) - storage and inventory quality of life, earned
  instead of installed. Its buttons are small symbols in the top-right
  corner of the inventory and chest windows - hover one to see its name.
  They appear only once you have bought them.
    Sorter's Hand      - one S button, aimed by the screen it is on. Rank I
                         puts it on your inventory and sorts your bag;
                         rank II also puts it on chests and sorts those
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

- ENCHANTER additions
    Anvil Adept I     - every anvil job costs 30% fewer levels. The cut
                        lands before the anvil calls a job too expensive,
                        so a 55-level merge drops to 39 and works again
    Anvil Adept II    - the anvil never answers "Too Expensive!" again,
                        and no job ever costs more than 40 levels
    Greater Mending   - lifts vanilla Mending from I to II. Two Mending
                        books merge into Mending II on YOUR anvil, and it
                        mends twice the durability per point of XP
    Reaper's Wisdom   - a mob killed with a Looting weapon gives +25% XP
                        per Looting level, on top of Scholar
    Capstone: ANCIENT KNOWLEDGE - at a full table (15 bookshelves) the
    three offers ask for 35, 40 and 45 instead of stopping at 30. That is
    what finally puts Sharpness V and Efficiency V - impossible at a
    vanilla table - back in the draw. Nothing ever goes above an
    enchantment's own maximum. At the 45 offer there is a CHANCE, not a
    promise, of a perfect item: every enchantment the item can carry, each
    at its maximum, in a set that fits together.

- PICKAXE gains a second finisher: ENDURING EDGE - a Dig Range swing
  costs the pickaxe half the durability, rounded up. 1 point per 2 blocks
  at rank I, 3 per 5 at rank II, 5 per 9 at rank III.

- NOT BUILT YET, shown starred and orange in the tree: four Axe tier-4
  ideas (Kindling, Woodcarver, Sap Tapper, Patina Hand), four Artisan
  tier-4 ideas (Crate Labels, Shulker Sight, Blueprints, Salvage) and the
  two Axe finishers, Everbloom and Bountiful Grove.
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
- "install.ps1" and "toolmastery-0.5.0.jar" have to stay in the same folder
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
