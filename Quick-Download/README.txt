PB ENCHANTMENTS (formerly Tool Mastery) - a mod for Minecraft 26.2 (Fabric)
Version 0.8.4-beta
================================================

HOW TO INSTALL
--------------
1. Have Minecraft Java Edition installed (official launcher) and run it at
   least once.
2. Double-click "Install PB Enchantments.bat".
   The installer will automatically:
     - install Fabric Loader (if you do not have it yet);
     - download Fabric API (if you do not have it yet);
     - remove an older Tool Mastery version, if there is one;
     - copy the PB Enchantments mod into your mods folder.
3. Open the Minecraft launcher, pick the "fabric-loader-26.2" profile and
   hit Play.
4. In game, press K to open the skill trees - and K again to close them.
   A line in the chat reminds you of the key every time you join a world.

HOW IT WORKS
------------
Every skill in the tree has up to TWO buttons:

  UNLOCK  - a one-off purchase: XP points plus materials you gather by
            playing the class. A passive switches on immediately. An
            enchantment starts showing up at your enchanting table.

  ENCHANT - repeatable: costs XP points and applies the enchantment
            to the item in your hand. Use it as often as you like, on as
            many tools as you like.

Enchant is deliberately pricier than the enchanting table (550 XP for
rank I, 2005 for II, 5345 for III): the table stays the cheap route,
Enchant is the guaranteed one.

Both buttons explain themselves before charging you. Clicking either turns
the panel into a confirmation card with the exact cost, the materials you
are carrying and what changes. Nothing is spent until you press Confirm.

Enchant also checks whether it can work BEFORE spending a single level: the
wrong tool (Smelt on an axe) or a conflicting enchantment greys the button
out, with the reason in the tooltip.

Each rank of a skill describes only what that rank does - Dig Range II
describes the cross, not the whole family.

WHAT IS NEW IN 0.8.4-beta
-------------------------
- AUTO BLOCK HAS AN ON/OFF SWITCH. A "B" button in your inventory,
  next to the other Artisan buttons, turns the packing on and off.
  Off, nine ingots stay nine ingots without pinning a slot. The
  button dims while it is off and chat confirms each flip.

- ARMOUR FOLLOWS THE INERT-ITEM RULE. A piece carrying a rank you
  have not unlocked - Protection V without Aegis, or a PB
  Enchantments armour rank above yours - stays on and keeps its
  enchantments, but protects like bare skin: no armour points, no
  toughness, no Protection or Feather Falling from it, and no wear.
  Its tooltip names the node to buy.

- PROTECTION V IS NO LONGER REWRITTEN TO IV. Enchanting or
  anvil-combining armour without Aegis used to hand you a working
  Protection IV. The rank now stays on the label and the piece is
  inert until you buy Aegis - the same rule the pickaxe follows for
  an unearned Dig Range III. Fortune IV, Looting IV, Power VI and
  Mending II follow it too: the table applies what it rolled, the
  anvil never lowers a rank an input already carries, and the
  librarian will not sell a book of a rank you cannot use. The anvil
  still refuses to forge a raised rank out of two lower ones. This
  reaches items you already own: a Fortune IV pickaxe made by someone
  who earned it is inert in the hands of someone who has not.

- AXE TIER 5 NO LONGER NEEDS ENVIRONMENT TO UNLOCK ENVIRONMENT.
  "Replants by Environment 200" is now "Plant saplings 300" - saplings
  you place by hand. Saplings planted so far already count.

- THE ENCHANTED BOOK COUNTER EXPLAINS ITSELF. The Enchanter tier-5
  gate was renamed from "Enchanted book checklist" and its description
  is now written in paragraphs: distinct enchantments across every
  enchanted book you carry, ranks ignored, best shelf remembered.
  Every gate tooltip now wraps its text.

- THE SAPLING CHECKLIST WANTS 13 KINDS. Crimson and warped fungus
  join the eleven overworld saplings, and the tooltip ticks off which
  kinds you still miss. It reads what you carry right now instead of
  remembering your best, so keep the saplings on you when you unlock
  the tier.

WHAT IS NEW IN 0.8.3-beta
-------------------------
- THE NOVICE LIBRARIAN SELLS ENCHANTED BOOKS. The random PB
  Enchantments book trade (24 emeralds + a book) moved down to
  villager level 1, and a second, dedicated trade sells the
  Indestructible book outright for 32 emeralds + a book. A librarian
  draws two trades per level, so re-roll the lectern until yours
  carries the one you want.

- THE SKILL SCREEN REMEMBERS YOUR TAB. Pressing K used to snap back
  to the Pickaxe tree every time; it now reopens on the tree you were
  reading, for the whole game session.

- FLASHPOINT COVERS ALL FIRE, ON A COOLDOWN. Its 10 seconds now block
  everything the game calls fire - lava, burning, magma, campfires,
  fireballs - the same list Fire Resistance reads. In exchange it
  rearms on a flat 2-minute cooldown from the moment it triggers,
  instead of waiting for you to stop burning.

- DOUBLE AXE ONLY DOUBLES GROWN TREES. Placing a log and chopping it
  used to roll the double drop, an infinite log printer. Logs you
  placed yourself never double now.

- HARVEST APPLES ACTUALLY COUNTS. Picking up a whole stack of apples
  counted as zero; every apple counts now.

- BOW GATES HALVED, ARCHMAGE TRIMMED. Every Arrows landed gate asks
  half of what it did (50 / 150 / 400 / 750 / 1250), and the
  Enchanter tier-5 gate Enchant items dropped from 200 to 100.
  Progress you already made still counts.

WHAT IS NEW IN 0.8.2-beta
-------------------------
- THE SERVER CHECKS YOUR MOD VERSION AT THE DOOR. A dedicated server
  running PB Enchantments now refuses a client whose mod version does
  not match its own - or that does not have the mod at all - before
  the world even loads. The message names both versions, so you
  always know which side is behind and what to download.

- EVERYONE UPDATES TOGETHER. The check is exact: the server and every
  player must run the same PB Enchantments version. This is also the
  first version that can answer the check - a player still on
  0.8.1-beta or older is turned away as if the mod were missing - so
  hand this jar to your players when you update the server. Nothing
  about your world or your progress changes.

- A GREEN THUMB HOE REFUSES TO TRAMPLE SEEDLINGS. While you own Green
  Thumb (the auto-replant node), a farmland crop that has not finished
  growing simply does not break under a hoe - the action bar says it is
  still growing. Only true farmland crops (wheat, carrots, potatoes,
  beetroot, torchflower, pitcher pod): sugar cane, cocoa, berries and
  nether wart are untouched, so cutting cane mid-stalk works as before.
  Sneak to break a seedling on purpose.

- LOGIC FELLS ONE TREE PER SWING. Cherry groves and jungle tangles
  whose trunks touched used to come down as one absurd harvest. Logic
  now finds each trunk's base on the ground and fells only the tree
  you hit. A 2x2 giant (jungle, spruce, dark oak) still counts as one
  tree.

- LOGIC III ONLY CLEARS ITS OWN CANOPY. A leaf that sits closer to a
  standing trunk stays on it, so neighbouring trees keep their crowns
  instead of being stripped to bare logs.

- THE FELL ALWAYS FINISHES THE TREE. Logic used to stop a couple of
  logs short of breaking the axe, leaving half a tree standing. Now
  every log costs durability and the tree comes down whole - if the
  wood outlasts the axe, the axe breaks mid-fell and the rest still
  falls. An Indestructible axe survives on its last durability point,
  spent, exactly as that enchantment already works.

- ENVIRONMENT REPLANTS WHEREVER YOU CHOP. The sapling only ever came
  back when the tree was cut at its bottom-most log; now it is planted
  at the tree's own rooted base whichever log you hit, and a 2x2 giant
  gets all four saplings back - one from your inventory per base.

- ONLY GROWN TREES FALL. Logs you place are remembered by the world
  and are never part of a tree: breaking one never triggers Logic, and
  a fell never crosses into a build. Trees grown from saplings fell
  normally. Logs placed before this update carry no memory - for
  those, the old rule (a tree needs leaves) still stands guard.

WHAT IS NEW IN 0.8.1-beta
-------------------------
- A RANK YOU HAVE NOT UNLOCKED NO LONGER WORKS AT ALL. Holding a Dig
  Range III pickaxe with only rank II bought used to give you a
  working rank II pickaxe. Now the whole item handles like a bare
  hand - bare-hand digging speed, no drops from blocks that need a
  tool, bare-hand damage, no durability spent - until you buy the
  rank on the label. It makes no difference how it reached you: the
  enchanting table, a librarian's book, an anvil, or another player.
  This reaches tools you already own, and the tooltip names the rank
  you still owe. Creative mode still bypasses the rule.

- SMELT II NO LONGER HANDS BACK INGOTS FOR EVERYTHING. The roll was
  repeated once for every block the swing broke, so a drop sitting in
  the middle of a Rich Vein or Dig Range swing was rolled eight times
  over - and eight tries at 50% is 99.6%. Every drop is rolled
  exactly once now, whatever else the swing took.

- SMELT PAYS WHAT A FURNACE PAYS. An ore that turns into an ingot
  mid-swing now drops the experience its smelting recipe would have
  given: raw iron and raw copper 0.7 apiece, raw gold 1.0.

- RICH VEIN TAKES THE VEIN AND NOTHING ELSE. Its size limit was only
  tested once per step of the search, so Rich Vein I could take
  thirty-three blocks instead of eight. And a pickaxe carrying Dig
  Range as well took the vein plus a 3x3 of the stone around the
  block that started it. The vein is the whole swing now, and Dig
  Range stands down on a block Rich Vein claims.

- THE ANVIL NO LONGER FORGES A RANK YOU HAVE NOT EARNED. Two Dig
  Range II books used to merge into a Dig Range III. Every PB
  Enchantments rank now caps at your own there, exactly as vanilla
  Fortune, Looting, Mending, Protection and Power already did.

- PROSPECTOR'S WISDOM, A NEW ENCHANTER CAPSTONE. A block broken with
  a Fortune tool gives more experience: +25% per level of Fortune,
  all four of them, so Fortune IV is +100%. It stacks on top of
  Scholar the same way Reaper's Wisdom does - and Reaper's Wisdom now
  rounds its own bonus up instead of down, so every rank is finally
  worth the 25% it advertises.

WHAT IS NEW IN 0.8.0-beta
-------------------------
- A NINTH CLASS: GROUND, THE PATH OF THE GROUND. One tab carrying two
  tools. The shovel moves the soil, the hoe wakes it up, and every
  tier is read twice - a shovel gate beside a hoe gate - so a player
  who only ever farms still opens the tier they are standing in.
  Twenty-five nodes across five tiers, badged "in testing" while its
  numbers get played.

- FLAT EARTH I-III, THE SHOVEL'S AREA DIG. A swing takes the pair
  above and below what you hit, then a 3x2, then a full 3x3 on the
  plane you are facing - and NEVER a block below the floor you are
  standing on. Dug at your feet that is a 3x3 patch of floor per
  swing: you clear one layer, step down, and clear the next. The hole
  stays flat and you stay in it. Sneak to disable.

- DIGGY DIGGY HOLE, THE FIRST HELD ABILITY IN THE MOD. Sneak and
  right-click with a shovel and everything within reach comes apart
  around you, block after block, until you switch it off. The name
  sits on your screen while it runs. Same floor rule: never below the
  block you are standing on, and never the one holding you up. It
  stops by itself - and says why - when the shovel leaves your hand,
  when it is about to break, when you get too hungry, or the moment
  your feet leave the ground.

- FORTUNE ON CROPS IS NOW THE HOE'S JOB, FOR EVERYBODY. A rule of the
  mod rather than something you buy: a Fortune pickaxe, axe or shovel
  no longer multiplies wheat, carrots, potatoes, beetroot, nether
  wart, cocoa, melons, pumpkins, berries or sugar cane. Only a hoe
  does. Ores and leaves are untouched, and Silk Touch is untouched -
  a Silk Touch pickaxe still pops a whole melon.

- HARVEST SWING I-III, AND THE FIELD REPLANTS ITSELF. A hoe harvests
  3x3, 5x5 then 7x7 around the crop you broke, taking only what is
  actually ripe and leaving seedlings to finish. Green Thumb puts
  every one of them straight back in the ground, paying its seed out
  of the harvest itself - so it works on the very first crop you cut,
  with an empty inventory, magnet or no magnet.

- THE REST OF THE FARM. Full Ears makes Fortune multiply the wheat and
  not just the seed. Gilded Roots turns up a golden carrot 5% then 10%
  of the time. Clean Crop cuts poisonous potatoes to a fiftieth.
  Furrow Hand tills the whole 3x3 for free. Bone Thrift gives a third
  of your bone meal back. Field Press bales wheat and presses melon
  slices - and always leaves the first 64 of each loose, because a
  farmer who baled all their wheat would have to break a bale to feed
  a cow.

- THE REST OF THE DIG. Spade's Grip I-III digs 20/40/60% faster.
  Digger's Magnet and Harvester's Magnet pocket what you break. Sifter
  makes gravel always give its flint and clay give a fifth ball. Soul
  Digger speeds up the Nether floor and sometimes doubles it. Concrete
  Setter brings concrete powder up already hardened. Gravedigger takes
  the fall damage out of a shaft you dug yourself - yours only.

- THE ROD TAB IS GONE, BUILDER TAKES ITS PLACE. The greyed "coming
  soon" tab now names the class that is actually coming next. Nothing
  was ever unlockable there, so no progress moved.
WHAT IS NEW IN 0.7.2-beta
-------------------------
- HOVER A GATE LINE TO SEE WHAT IT COUNTS. Under "Gate achievements",
  resting the mouse on a line opens what that counter actually
  measures - which tool it wants in your hand, what does not count,
  whether it is checked once a second. No more guessing why a number
  is not moving.

- CHECKLISTS SAY WHAT YOU ARE STILL MISSING. Ore checklist, the two
  wood checklists, the enchanted-gear checklist, the armour material
  list, the iron set and the new boss list all show every entry
  ticked or unticked, by name and in your language. "7/11" told you
  how far along you were; the list tells you it is quartz and ancient
  debris you still owe. Existing progress carries over - nothing
  resets.

- BOSS CHECKLIST CLOSES THE SWORD TREE. Legend, the Sword's tier 7,
  now asks for one of each: Elder Guardian, Wither, Warden and Ender
  Dragon. Slay the Ender Dragon moves down to tier 5, Champion.

- TRIM A FULL ARMOR SET. Aegis Bearer, the Armor tree's tier 6, has a
  new line for wearing four pieces with an armour trim on every one
  of them. Pattern and material are yours to pick - they do not have
  to match.

- HALF THE GRIND IN THE THREE COMBAT TREES. Kill hostile mobs is
  halved across all seven Sword tiers (200 becomes 100 at tier 1,
  5000 becomes 2500 at tier 7), Damage absorbed by armor across all
  seven Armor tiers (500 becomes 250, 40000 becomes 20000), and the
  Bow's Arrows fired and Ranged kills likewise. Arrows landed is
  unchanged, so it is that line pacing the early Bow tiers now.

- DEFT HANDS NO LONGER GRABS THE STACK OUT OF YOUR HAND. Picking a
  stack up off the hotbar with the inventory open looked exactly like
  spending it, so the slot refilled itself under your cursor and the
  stack you were carrying had nowhere to go back to. The perk stands
  down while an inventory or container screen is open, and picks up
  where it left off when you close it.

WHAT IS NEW IN 0.7.1-beta
-------------------------
- LEAVES COUNT WHATEVER BREAKS THEM. The Axe tree's leaf gate only
  ever saw leaves cleared with an axe. Shears, a sword, a bare hand
  and the canopy Timber III sweeps away all count now. Leaves that
  rot away on their own still do not - that was never the point.

- THE SEARCH BOX KEEPS YOUR KEYS. Typing an item name with an E in it
  used to slam the inventory shut, and Q threw the stack you were
  hovering. While the caret is in the Seeker's Eye field, the screen
  stops reading your typing as shortcuts. Clicking the magnifier now
  also puts the caret straight in the field - it used to take a
  second click.

- XP LEVELS SPENT IS NOW XP POINTS SPENT. Levels are not a fixed
  price - three levels off a level-40 player is several times the
  experience it is off a level-15 one - so the gate asked a different
  question of everybody. It counts experience points now, and it
  counts every way you spend them: the enchanting table, the anvil,
  and every tier, node and enchant bought in the skill tree. Progress
  already made is carried over, generously.

- ANVIL COMBINES MEANS ANY COMBINE. It only counted a sacrifice that
  carried enchantments, so two plain pickaxes were invisible to it.
  Anything that merges two items counts now - tool on tool, book on
  tool, book on book, enchanted or not. Repairing with raw material
  still does not, and neither does a bare rename.

- THE DOWNLOAD HAS ITS VERSION IN ITS NAME. Releases used to carry
  two copies of the same jar, and the one the download button handed
  you was the unversioned "pbenchants.jar" - so there was no way to
  tell which build was in your mods folder. There is one asset now,
  "pbenchants-<version>.jar", and the button points at it.

WHAT IS NEW IN 0.7.0-beta
-------------------------
- IT TELLS YOU IT IS THERE. Joining a world puts one line in the chat,
  in your own language, naming the key that opens the skill trees -
  and pressing that key for the first time earns the first
  advancement of the PB Enchantments tab.

- K OPENS AND K CLOSES. Whatever you have the skill-tree key bound to,
  it now works both ways, like E does for the inventory.

- SEE THE WORLD THROUGH THE TREE. A button in the top-right corner of
  the skill screen switches the backdrop between solid and
  see-through, so you can read the tree against the cave or the map
  you are standing on. The choice is remembered.

- YOU CAN HEAR PROGRESS. A tier opening plays a chime, and so does the
  moment the goal you pinned to the HUD scoreboard finally becomes
  buyable - never both for the same event.

- LOGIC I COSTS WHAT IT SHOULD. Felling a whole tree in one swing is
  paid for with a fixed, slow chop on the first log - about four
  seconds, the same on every axe. Efficiency and the axe's own tier
  no longer buy that price back. Logic II and III chop at full speed
  as before.

WHAT IS NEW IN 0.6.9-beta
-------------------------
- TOOL MASTERY IS NOW PB ENCHANTMENTS. New name, new command
  (/pbenchants - /mastery still answers with a pointer), new jar name
  (pbenchants-x.y.z.jar). YOUR WORLDS ARE SAFE: everything a save
  stores - your skill progress, the enchantments on your tools, your
  advancements - kept its old internal name on purpose, so existing
  worlds load with everything exactly where you left it.

- RELEASES ARE PUBLIC NOW: every version is published on the GitHub
  Releases page with a permanent download link, and the project has a
  landing page. This is the beta of the rename - 1.0.0 follows once
  it has settled.

WHAT IS NEW IN 0.6.8
--------------------
- THE HUD GOAL TRACKER TICKS LIVE now. Progress used to reach your
  client only on login and after skill-screen actions, so the pinned
  scoreboard froze on old numbers until you reopened the tree. The
  server now pushes your progress the moment it changes (checked once a
  second, sent only when something actually moved) - chop a log and the
  tracker line ticks up right there on your HUD.

WHAT IS NEW IN 0.6.7
--------------------
- THE SKILL SCREEN ANSWERS WHERE YOU CLICKED. Every verdict of a skill
  action - "unlocked!", "not enough XP", "no such biome within reach",
  "sell the higher rank first" - now appears as a banner inside the
  skill screen itself, green for success and red for the reason it
  failed, instead of a chat message behind the screen. Chat is only the
  fallback for a reply that arrives after you closed the screen.

WHAT IS NEW IN 0.6.6
--------------------
- AUTO BLOCK is live (Artisan, tier 4). Nine of any ore material in your
  inventory pack themselves into the block within a second - ingots, raw
  ore, coal, diamonds, emeralds, redstone, lapis, netherite. Everything
  but quartz. Nine gold or iron nuggets become an ingot, and a nugget
  windfall cascades all the way up to blocks. Your pinned slots are
  never touched, and renamed or enchanted stacks are left alone.

- BIOME CHART I & II are live (Explorer, tiers 2-3). Own the node and a
  "Buy map" button appears: each purchase drops a real filled map in
  your inventory with an X on the biome it found, named after it.
    Rank I  - 55 XP, points at a random biome of your dimension
    Rank II - the map only points at biomes you have NEVER visited, so
              every purchase is a guaranteed new discovery for the biome
              checklist. Price: 55 XP times your unlocked Explorer tiers
  If no biome can be found in range, nothing is spent.

WHAT IS NEW IN 0.6.5
--------------------
- THE MOD SPEAKS YOUR LANGUAGE. Full translations into Brazilian
  Portuguese and Spanish, following the language your client is set to.
  Every skill, tier, gate achievement, button, tooltip and chat message.
  Any other language falls back to English.

- AXE TREE, tiers 1 and 2: the wood targets were cut in half. Chop Logs
  256 -> 128, Make Charcoal 64 -> 32, Logs Chopped In Total 512 -> 256,
  Strip Logs 32 -> 16.

- New coming-soon nodes in the Explorer tree: BIOME CHART I (tier 2) -
  buy a real map to a random biome for a little XP, as often as you
  like; BIOME CHART II (tier 3) - the map now points only at biomes you
  have never visited, for more XP. The mod's first skills that put an
  item in your inventory.

WHAT IS NEW IN 0.6.4
--------------------
Bug fixes for the combat progress counters - if a gate seemed stuck, it
was, and these were why:

- CRIT KILLS count again. The game only remembered a critical hit AFTER
  the blow had already landed - too late for the kill it caused - and on
  a server it never remembered one at all.

- MELEE DAMAGE counts again, and it now counts what the hit actually
  took off the target: spam-clicking, hits during the red flash and
  overkill on a nearly dead mob are all worth what they did, nothing.

- DAMAGE ABSORBED counts again. It was being measured before the armour
  had done its work, so it read zero on every hit.

- The Sword damage skills (Keen Edge, Executioner, Adrenaline,
  Bloodthirst, Death Eyes), Hunter's Mark and Cleave now actually apply
  in real play - the same bug that froze the melee counter was
  swallowing their bonuses too.

WHAT IS NEW IN 0.6.3
--------------------
- INDESTRUCTIBLE finally means what it says everywhere:
    * A spent armour piece (durability run out) protects for NOTHING
      until repaired - your armour bar visibly drops, as if the piece
      were not there. The same happens to armour worn by mobs: a zombie's
      scavenged helmet, wolf armour, horse armour, nautilus armour.
    * A spent item's RIGHT CLICK is dead too: a bow will not draw, a
      crossbow will not load, a hoe will not till, a shovel will not
      path, an axe will not strip, a brush will not brush, a spear will
      not throw - nothing works until you repair it. (The mace never had
      a right-click job, so it is unchanged.)
    * The action bar tells you why: "Your <item> is spent - repair it
      before it works again."

WHAT IS NEW IN 0.6.2
--------------------
- NIGHT EYES actually works now. The old brightness lift was invisible in
  a truly dark night; the node is now a permanent Night Vision at 70% of
  the potion's strength. A real potion still beats it, and a warden still
  blinds you.

- PUFFERFISH LUNGS is no longer infinite air. Every time your head goes
  under you get 15 seconds of Water Breathing - one dive's worth. Stay
  down longer and the breath meter runs like anyone's; surface for a
  moment and the next dive refills it.

- New coming-soon node in the Artisan tree (tier 4): AUTO BLOCK - nine of
  any ore material in your inventory pack themselves into the block
  (ingots, raw ore, diamonds, emeralds, redstone, lapis - everything but
  quartz), and nine gold nuggets pack into an ingot.

WHAT IS NEW IN 0.6.1
--------------------
- E (inventory key) and K (the skill tree key) now CLOSE the skill tree,
  the same way E closes your inventory.

- AXE TREE cleaned up: every coming-soon node is gone. Double Axe I moved
  to tier 4; Double Axe II and the Environment enchantment are now the
  tier 5 finishers.

- ARTISAN TREE: Crate Labels, Blueprints and Salvage were removed. The
  only coming-soon node left is Shulker Sight.

- The SWORD, ARMOR and BOW classes are tagged IN TESTING: fully playable,
  but their numbers may still change. The tag shows on the class tab, on
  every node tooltip and in the details panel.

WHAT IS NEW IN 0.6.0
--------------------
- EVERYTHING IS PRICED IN XP POINTS now, not levels. Levels get more
  expensive the higher you are, so "20 levels" used to cost a level-50
  player far more experience than a level-30 one. A price is now a fixed
  number of points - the same for everyone. The skill screen shows your
  points on the XP bar, every button shows the exact points it will
  spend, and nothing changed about HOW you earn experience.

- SELL A SKILL BACK. An owned node's button becomes "Sell (+XP)": selling
  refunds one fifth of the unlock price in XP points. Materials are not
  refunded, and a rank that has higher ranks (or a capstone) bought on
  top of it has to wait until those are sold first. Buying it back later
  costs the full price again.

- TRACK A GOAL ON YOUR HUD. Select any node or tier and press the new
  Track button: a small scoreboard appears at the right edge of the
  screen with the gate achievements still missing for it, ticking up
  live as you play - no need to reopen the tree. One goal at a time;
  Track something else to switch, or Untrack to clear it.

- For testers: "/pbenchants debug master true" (op only) makes every unlock
  free - no XP, no materials, no gates, no tier locks. Rank chains still
  unlock in order. "false" turns it back off; the flag survives relogs.

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

- A BORROWED BOW IS INERT: carrying an unearned PB Enchantments enchantment,
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
- "install.ps1" and "pbenchants-1.0.0.jar" have to stay in the same folder
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
