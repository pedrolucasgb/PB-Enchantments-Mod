package dev.pbenchants.skill;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Static definitions of every skill tree — the design document in code.
 * Pickaxe, Axe, Enchanter, Explorer, Artisan, Sword, Armor and Bow are
 * playable; Rod is still coming.
 *
 * <p>Every node carries two prices. {@code of/chained/capstone} sets the XP half
 * of the <b>unlock</b>, {@code .costing(...)} its materials, and
 * {@code .enchantFor(n)} the repeatable <b>enchant</b> price. Enchant prices sit
 * far above unlock prices on purpose: 20 levels for a rank I, 35 for a rank II
 * and 50 for a rank III, so a table at 30 levels — which also throws in vanilla
 * enchantments — stays the better deal for anyone willing to grind for it.
 * Single-level enchantments are priced by what they are worth rather than by
 * rank, which is why Indestructible costs 40.
 */
public final class SkillTrees {
	private SkillTrees() {
	}

	private static MaterialCost mat(Item item, int count) {
		return MaterialCost.of(item, count);
	}

	private static MaterialCost mat(TagKey<Item> tag, int count) {
		return MaterialCost.of(tag, count);
	}

	// ---------- Pickaxe — Path of the Deep ----------
	public static final SkillTree PICKAXE = new SkillTree(
		"pickaxe",
		Items.DIAMOND_PICKAXE,
		List.of(
			// Tier 1 — Stone Apprentice
			new SkillTier(5, List.of(
				new GateRequirement("break_stone", 500),
				new GateRequirement("mine_coal", 64),
				new GateRequirement("craft_iron_pickaxe", 1)
			)),
			// Tier 2 — Excavator
			new SkillTier(10, List.of(
				new GateRequirement("mine_iron", 128),
				new GateRequirement("mine_copper", 64),
				new GateRequirement("reach_y0", 1),
				new GateRequirement("smelt_ores", 64)
			)),
			// Tier 3 — Mine Master
			new SkillTier(15, List.of(
				new GateRequirement("mine_gold", 32),
				new GateRequirement("mine_diamond", 16),
				new GateRequirement("mine_redstone", 64),
				new GateRequirement("mine_lapis", 64)
			)),
			// Tier 4 — Lord of the Depths
			new SkillTier(20, List.of(
				new GateRequirement("mine_ancient_debris", 8),
				new GateRequirement("break_deepslate", 1000),
				new GateRequirement("mine_mountain_emerald", 1)
			)),
			// Tier 5 — Heart of the Mountain
			new SkillTier(30, List.of(
				new GateRequirement("ore_checklist", 11),
				new GateRequirement("break_total", 10000)
			))
		),
		List.of(
			// Tier 1
			SkillNode.of("masons_grip_1", 0, 3, SkillType.PASSIVE).icon(Items.COBBLESTONE)
				.costing(mat(Items.COBBLESTONE, 64), mat(Items.COAL, 8)),
			SkillNode.of("miners_magnet", 0, 5, SkillType.PASSIVE).icon(Items.HOPPER)
				.costing(mat(Items.IRON_INGOT, 8), mat(Items.HOPPER, 1)),
			SkillNode.of("smelt_1", 0, 4, SkillType.ENCHANTMENT).icon(Items.FURNACE)
				.costing(mat(Items.COAL, 16), mat(Items.IRON_INGOT, 8))
				.enchantFor(20),
			// Tier 2
			SkillNode.chained("masons_grip_2", 1, 5, "masons_grip_1", SkillType.PASSIVE).icon(Items.COBBLED_DEEPSLATE)
				.costing(mat(Items.COBBLESTONE, 128), mat(Items.IRON_INGOT, 16)),
			SkillNode.of("dig_range_1", 1, 6, SkillType.ENCHANTMENT).icon(Items.IRON_PICKAXE)
				.costing(mat(Items.IRON_INGOT, 12), mat(Items.FLINT, 8))
				.enchantFor(20),
			SkillNode.chained("smelt_2", 1, 6, "smelt_1", SkillType.ENCHANTMENT).icon(Items.BLAST_FURNACE)
				.costing(mat(Items.COAL, 32), mat(Items.BLAST_FURNACE, 1))
				.enchantFor(35),
			// Tier 3
			SkillNode.chained("masons_grip_3", 2, 8, "masons_grip_2", SkillType.PASSIVE).icon(Items.DEEPSLATE_TILES)
				.costing(mat(Items.COBBLED_DEEPSLATE, 128), mat(Items.GOLD_INGOT, 8)),
			SkillNode.chained("dig_range_2", 2, 9, "dig_range_1", SkillType.ENCHANTMENT).icon(Items.DIAMOND_PICKAXE)
				.costing(mat(Items.DIAMOND, 6), mat(Items.REDSTONE, 32))
				.enchantFor(35),
			SkillNode.chained("smelt_3", 2, 9, "smelt_2", SkillType.ENCHANTMENT).icon(Items.BLAZE_POWDER)
				.costing(mat(Items.BLAZE_POWDER, 8), mat(Items.COAL, 32))
				.enchantFor(50),
			SkillNode.of("rich_vein_1", 2, 10, SkillType.ENCHANTMENT).icon(Items.IRON_ORE)
				.costing(mat(Items.DIAMOND, 4), mat(Items.LAPIS_LAZULI, 32))
				.enchantFor(20),
			// Tier 4
			SkillNode.of("deep_haste", 3, 8, SkillType.PASSIVE).icon(Items.BEACON)
				.costing(mat(Items.DIAMOND_BLOCK, 1), mat(Items.GLOWSTONE_DUST, 16)),
			SkillNode.chained("dig_range_3", 3, 14, "dig_range_2", SkillType.ENCHANTMENT).icon(Items.NETHERITE_PICKAXE)
				.costing(mat(Items.NETHERITE_INGOT, 1), mat(Items.DIAMOND, 8))
				.enchantFor(50),
			SkillNode.of("obsidian_breaker", 3, 6, SkillType.PASSIVE).icon(Items.OBSIDIAN)
				.costing(mat(Items.OBSIDIAN, 16), mat(Items.DIAMOND, 4)),
			SkillNode.chained("rich_vein_2", 3, 10, "rich_vein_1", SkillType.ENCHANTMENT).icon(Items.DEEPSLATE_DIAMOND_ORE)
				.costing(mat(Items.DIAMOND, 8), mat(Items.EMERALD_BLOCK, 1))
				.enchantFor(35),
			// Tier 5 — two finishers, buyable together
			SkillNode.of("ancient_fortune", 4, 20, SkillType.PASSIVE).icon(Items.ANCIENT_DEBRIS)
				.costing(mat(Items.NETHERITE_INGOT, 2), mat(Items.EMERALD, 32), mat(Items.AMETHYST_SHARD, 16)),
			SkillNode.of("enduring_edge", 4, 20, SkillType.PASSIVE).icon(Items.NETHERITE_SCRAP)
				.costing(mat(Items.NETHERITE_SCRAP, 4), mat(Items.OBSIDIAN, 32), mat(Items.AMETHYST_SHARD, 16))
		)
	);

	// ---------- Axe — Path of the Grove ----------
	public static final SkillTree AXE = new SkillTree(
		"axe",
		Items.DIAMOND_AXE,
		List.of(
			// Tier 1 — Apprentice Lumberjack. Wood targets halved in 0.6.5:
			// the old numbers made tier 1 the grindiest opener of any tree.
			new SkillTier(5, List.of(
				new GateRequirement("chop_logs", 128),
				new GateRequirement("craft_iron_axe", 1),
				new GateRequirement("make_charcoal", 32)
			)),
			// Tier 2 — Feller (wood targets halved in 0.6.5 too)
			new SkillTier(10, List.of(
				new GateRequirement("overworld_wood_checklist", 6),
				new GateRequirement("chop_logs_total", 256),
				new GateRequirement("strip_logs", 16)
			)),
			// Tier 3 — Master Lumberjack
			new SkillTier(15, List.of(
				new GateRequirement("fell_with_logic", 100),
				new GateRequirement("nether_wood_checklist", 2),
				new GateRequirement("break_leaves", 1000)
			)),
			// Tier 4 — Grove Warden
			new SkillTier(20, List.of(
				new GateRequirement("overworld_wood_checklist", 9),
				new GateRequirement("plant_saplings", 128),
				new GateRequirement("harvest_apples", 32),
				new GateRequirement("fell_trees_total", 500)
			)),
			// Tier 5 — Forest Spirit
			new SkillTier(30, List.of(
				new GateRequirement("fell_trees_grand_total", 1000),
				new GateRequirement("sapling_checklist", 11),
				new GateRequirement("replant_with_environment", 200)
			))
		),
		List.of(
			// Tier 1
			SkillNode.of("lumberjacks_arms_1", 0, 3, SkillType.PASSIVE).icon(Items.STONE_AXE)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.IRON_INGOT, 4)),
			SkillNode.of("loggers_magnet", 0, 5, SkillType.PASSIVE).icon(Items.HOPPER)
				.costing(mat(Items.IRON_INGOT, 8), mat(Items.HOPPER, 1)),
			SkillNode.of("fair_harvest", 0, 4, SkillType.PASSIVE).icon(Items.OAK_SAPLING)
				.costing(mat(ItemTags.SAPLINGS, 16), mat(Items.BONE_MEAL, 16)),
			// Tier 2
			SkillNode.chained("lumberjacks_arms_2", 1, 5, "lumberjacks_arms_1", SkillType.PASSIVE).icon(Items.IRON_AXE)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.IRON_INGOT, 16)),
			SkillNode.of("logic_1", 1, 8, SkillType.ENCHANTMENT).icon(Items.OAK_LOG)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.IRON_INGOT, 8))
				.enchantFor(20),
			// Tier 3
			SkillNode.chained("lumberjacks_arms_3", 2, 8, "lumberjacks_arms_2", SkillType.PASSIVE).icon(Items.DIAMOND_AXE)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.GOLD_INGOT, 8)),
			SkillNode.of("pruner", 2, 6, SkillType.PASSIVE).icon(Items.SHEARS)
				.costing(mat(ItemTags.LEAVES, 64), mat(Items.SHEARS, 2)),
			SkillNode.chained("logic_2", 2, 9, "logic_1", SkillType.ENCHANTMENT).icon(Items.OAK_WOOD)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.DIAMOND, 4))
				.enchantFor(35),
			// Tier 4
			SkillNode.chained("logic_3", 3, 12, "logic_2", SkillType.ENCHANTMENT).icon(Items.OAK_LEAVES)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.DIAMOND, 8))
				.enchantFor(50),
			SkillNode.of("double_axe_1", 3, 7, SkillType.PASSIVE).icon(Items.GOLDEN_AXE)
				.costing(mat(ItemTags.LOGS, 32), mat(Items.DIAMOND, 4)),
			// Tier 5 — the tree closes on its two payoffs: the doubled harvest
			// and the enchantment that replants what Logic fells.
			SkillNode.chained("double_axe_2", 4, 8, "double_axe_1", SkillType.PASSIVE).icon(Items.GOLD_BLOCK)
				.costing(mat(Items.DIAMOND, 8), mat(Items.EMERALD_BLOCK, 1)),
			SkillNode.chained("environment", 4, 8, "logic_3", SkillType.ENCHANTMENT).icon(Items.BONE_MEAL)
				.costing(mat(ItemTags.SAPLINGS, 64), mat(Items.BONE_MEAL, 32))
				.enchantFor(25)
		)
	);

	// ---------- Enchanter — Path of the Arcane ----------
	public static final SkillTree ENCHANTER = new SkillTree(
		"enchanter",
		Items.ENCHANTING_TABLE,
		List.of(
			// Tier 1 — Apprentice Scribe
			new SkillTier(5, List.of(
				new GateRequirement("craft_enchanting_table", 1),
				new GateRequirement("craft_bookshelves", 15),
				new GateRequirement("enchant_items", 5)
			)),
			// Tier 2 — Rune Reader
			new SkillTier(10, List.of(
				new GateRequirement("enchant_items", 20),
				new GateRequirement("mine_lapis", 64),
				new GateRequirement("craft_books", 32),
				new GateRequirement("buy_enchanted_books", 3)
			)),
			// Tier 3 — Arcanist
			new SkillTier(15, List.of(
				new GateRequirement("reach_level_30", 1),
				new GateRequirement("max_slot_enchants", 10),
				new GateRequirement("enchant_type_checklist", 6),
				new GateRequirement("anvil_combines", 15)
			)),
			// Tier 4 — Master Enchanter
			new SkillTier(20, List.of(
				new GateRequirement("spend_points", 10000),
				new GateRequirement("collect_xp", 10000),
				new GateRequirement("grindstone_disenchants", 20),
				new GateRequirement("buy_enchanted_books", 12)
			)),
			// Tier 5 — Archmage
			new SkillTier(30, List.of(
				new GateRequirement("enchant_items", 200),
				new GateRequirement("max_slot_enchants", 50),
				new GateRequirement("enchanted_book_checklist", 12)
			))
		),
		List.of(
			// Tier 1
			SkillNode.of("arcane_insight_1", 0, 4, SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.LAPIS_LAZULI, 16), mat(Items.BOOK, 4)),
			SkillNode.of("scholar_1", 0, 3, SkillType.PASSIVE).icon(Items.EXPERIENCE_BOTTLE)
				.costing(mat(Items.BOOK, 8), mat(Items.BOOKSHELF, 4)),
			// Tier 2
			SkillNode.chained("arcane_insight_2", 1, 6, "arcane_insight_1", SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.LAPIS_LAZULI, 32), mat(Items.BOOKSHELF, 8)),
			SkillNode.chained("scholar_2", 1, 5, "scholar_1", SkillType.PASSIVE).icon(Items.EXPERIENCE_BOTTLE)
				.costing(mat(Items.BOOK, 16), mat(Items.BOOKSHELF, 8)),
			// Tier 3
			SkillNode.chained("arcane_insight_3", 2, 9, "arcane_insight_2", SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.BOOKSHELF, 16)),
			SkillNode.of("inner_focus", 2, 8, SkillType.PASSIVE).icon(Items.LAPIS_LAZULI)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.ENCHANTING_TABLE, 1)),
			SkillNode.of("indestructible", 2, 10, SkillType.ENCHANTMENT).icon(Items.PHANTOM_MEMBRANE)
				.costing(mat(Items.PHANTOM_MEMBRANE, 16), mat(Items.LAPIS_LAZULI, 32))
				.enchantFor(40),
			SkillNode.of("anvil_adept_1", 2, 7, SkillType.PASSIVE).icon(Items.ANVIL)
				.costing(mat(Items.IRON_BLOCK, 4), mat(Items.EMERALD, 8)),
			// Tier 4
			SkillNode.chained("scholar_3", 3, 8, "scholar_2", SkillType.PASSIVE).icon(Items.EXPERIENCE_BOTTLE)
				.costing(mat(Items.BOOK, 32), mat(Items.BOOKSHELF, 16), mat(Items.EMERALD, 8)),
			SkillNode.chained("anvil_adept_2", 3, 12, "anvil_adept_1", SkillType.PASSIVE).icon(Items.CHIPPED_ANVIL)
				.costing(mat(Items.IRON_BLOCK, 8), mat(Items.DIAMOND, 8), mat(Items.EMERALD, 16)),
			// Tier 5 — three finishers, buyable together
			SkillNode.of("ancient_knowledge", 4, 20, SkillType.PASSIVE).icon(Items.ENCHANTED_BOOK)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.BOOKSHELF, 16), mat(Items.EMERALD_BLOCK, 4)),
			SkillNode.of("greater_mending", 4, 20, SkillType.PASSIVE).icon(Items.EXPERIENCE_BOTTLE)
				.costing(mat(Items.EXPERIENCE_BOTTLE, 32), mat(Items.EMERALD_BLOCK, 2), mat(Items.NETHERITE_INGOT, 1)),
			SkillNode.of("reapers_wisdom", 4, 20, SkillType.PASSIVE).icon(Items.SCULK_CATALYST)
				.costing(mat(Items.SCULK_CATALYST, 1), mat(Items.ECHO_SHARD, 4), mat(Items.EMERALD_BLOCK, 2))
		)
	);

	// ---------- Explorer — Path of the Horizon ----------

	/**
	 * The first class that is not tied to a tool: it levels from <em>movement</em>,
	 * so its gates read distance travelled and places seen instead of blocks
	 * broken. The distance counters are diffed off the vanilla {@code *_ONE_CM}
	 * statistics once a second, which survives teleports and deaths for free.
	 */
	public static final SkillTree EXPLORER = new SkillTree(
		"explorer",
		Items.COMPASS,
		List.of(
			// Tier 1 — Wanderer
			new SkillTier(5, List.of(
				new GateRequirement("walk_blocks", 5000),
				new GateRequirement("biome_checklist", 8),
				new GateRequirement("craft_boat", 1)
			)),
			// Tier 2 — Traveler
			new SkillTier(10, List.of(
				new GateRequirement("travel_total", 25000),
				new GateRequirement("biome_checklist", 16),
				new GateRequirement("boat_blocks", 3000),
				new GateRequirement("swim_blocks", 1000)
			)),
			// Tier 3 — Pathfinder
			new SkillTier(15, List.of(
				new GateRequirement("biome_checklist", 24),
				new GateRequirement("elytra_blocks", 5000),
				new GateRequirement("structure_checklist", 5),
				new GateRequirement("dimension_checklist", 2),
				new GateRequirement("swim_blocks", 5000)
			)),
			// Tier 4 — Voyager
			new SkillTier(20, List.of(
				new GateRequirement("elytra_blocks", 25000),
				new GateRequirement("biome_checklist", 32),
				new GateRequirement("dimension_checklist", 3),
				new GateRequirement("structure_checklist", 10),
				new GateRequirement("travel_total", 100000)
			)),
			// Tier 5 — Worldwalker
			new SkillTier(30, List.of(
				new GateRequirement("travel_total", 250000),
				new GateRequirement("elytra_blocks", 100000),
				new GateRequirement("biome_checklist", 45)
			))
		),
		List.of(
			// Tier 1
			SkillNode.of("cartographer_1", 0, 3, SkillType.PASSIVE).icon(Items.MAP)
				.costing(mat(Items.PAPER, 8), mat(Items.COMPASS, 1)),
			SkillNode.of("tireless_1", 0, 4, SkillType.PASSIVE).icon(Items.SUGAR)
				.costing(mat(Items.LEATHER, 16), mat(Items.SUGAR, 8)),
			SkillNode.of("sea_legs", 0, 4, SkillType.PASSIVE).icon(Items.OAK_BOAT)
				.costing(mat(Items.OAK_PLANKS, 32), mat(Items.STRING, 16)),
			// Tier 2
			SkillNode.of("night_eyes", 1, 5, SkillType.PASSIVE).icon(Items.GLOWSTONE_DUST)
				.costing(mat(Items.GLOWSTONE_DUST, 16), mat(Items.GOLDEN_CARROT, 8)),
			SkillNode.of("clear_sight_1", 1, 5, SkillType.PASSIVE).icon(Items.PRISMARINE_SHARD)
				.costing(mat(Items.PRISMARINE_SHARD, 16), mat(Items.GLASS, 4)),
			SkillNode.chained("tireless_2", 1, 6, "tireless_1", SkillType.PASSIVE).icon(Items.RABBIT_FOOT)
				.costing(mat(Items.LEATHER, 32), mat(Items.SUGAR, 16), mat(Items.RABBIT_FOOT, 4)),
			SkillNode.of("slipstream_1", 1, 6, SkillType.ENCHANTMENT).icon(Items.PHANTOM_MEMBRANE)
				.costing(mat(Items.PHANTOM_MEMBRANE, 8), mat(Items.FIREWORK_ROCKET, 16))
				.enchantFor(20),
			// The mod's first buy-an-item nodes: each purchase drops a real map
			// in the inventory. Rank I points anywhere; rank II, a tier up,
			// points only at biomes the player has never set foot in — the
			// price climbing with the tier. See BiomeCharts.
			SkillNode.of("biome_chart_1", 1, 4, SkillType.ITEM).icon(Items.FILLED_MAP)
				.costing(mat(Items.PAPER, 16), mat(Items.COMPASS, 1)),
			// Tier 3
			SkillNode.of("remember", 2, 9, SkillType.PASSIVE).icon(Items.WRITTEN_BOOK)
				.costing(mat(Items.PAPER, 16), mat(Items.COMPASS, 1), mat(Items.ENDER_PEARL, 4)),
			SkillNode.chained("slipstream_2", 2, 8, "slipstream_1", SkillType.ENCHANTMENT).icon(Items.FIREWORK_ROCKET)
				.costing(mat(Items.PHANTOM_MEMBRANE, 16), mat(Items.FIREWORK_ROCKET, 32))
				.enchantFor(35),
			SkillNode.chained("clear_sight_2", 2, 8, "clear_sight_1", SkillType.PASSIVE).icon(Items.HEART_OF_THE_SEA)
				.costing(mat(Items.PRISMARINE_SHARD, 32), mat(Items.HEART_OF_THE_SEA, 1)),
			SkillNode.of("trailblazer", 2, 7, SkillType.PASSIVE).icon(Items.GRAVEL)
				.costing(mat(Items.GRAVEL, 64), mat(Items.IRON_INGOT, 8)),
			SkillNode.chained("biome_chart_2", 2, 8, "biome_chart_1", SkillType.ITEM).icon(Items.CARTOGRAPHY_TABLE)
				.costing(mat(Items.PAPER, 32), mat(Items.COMPASS, 1), mat(Items.ENDER_PEARL, 2)),
			// Tier 4
			SkillNode.chained("tireless_3", 3, 8, "tireless_2", SkillType.PASSIVE).icon(Items.GOLDEN_CARROT)
				.costing(mat(Items.LEATHER, 64), mat(Items.GOLDEN_CARROT, 8), mat(Items.DIAMOND, 4)),
			SkillNode.chained("slipstream_3", 3, 14, "slipstream_2", SkillType.ENCHANTMENT).icon(Items.ELYTRA)
				.costing(mat(Items.NETHERITE_INGOT, 1), mat(Items.FIREWORK_ROCKET, 64))
				.enchantFor(50),
			SkillNode.of("soft_landing", 3, 10, SkillType.PASSIVE).icon(Items.SLIME_BALL)
				.costing(mat(Items.PHANTOM_MEMBRANE, 16), mat(Items.SLIME_BALL, 16)),
			SkillNode.of("waypoint", 3, 10, SkillType.ACTIVE).icon(Items.AMETHYST_SHARD)
				.costing(mat(Items.AMETHYST_SHARD, 8), mat(Items.ECHO_SHARD, 1), mat(Items.GOLD_INGOT, 4)),
			// Tier 5 — capstone (pick one)
			SkillNode.of("endless_horizon", 4, 20, SkillType.PASSIVE).icon(Items.FIREWORK_ROCKET)
				.costing(mat(Items.NETHERITE_INGOT, 2), mat(Items.FIREWORK_ROCKET, 64), mat(Items.PHANTOM_MEMBRANE, 16)),
			SkillNode.of("pufferfish_lungs", 4, 20, SkillType.PASSIVE).icon(Items.PUFFERFISH)
				.costing(mat(Items.PUFFERFISH, 16), mat(Items.HEART_OF_THE_SEA, 1), mat(Items.PRISMARINE_CRYSTALS, 32))
		)
	);

	// ---------- Artisan — Path of Order ----------

	/**
	 * Where Pickaxe and Axe are about <em>getting</em> resources, this class is
	 * about <em>keeping them in order</em>: every perk is inventory and storage
	 * quality of life, earned instead of installed, and the capstone is
	 * Terraria's Quick Stack to Nearby Chests.
	 *
	 * <p>Named Artisan rather than Crafter on purpose — the class barely crafts,
	 * and Minecraft already ships a block called the Crafter.
	 */
	public static final SkillTree ARTISAN = new SkillTree(
		"artisan",
		Items.CHEST,
		List.of(
			// Tier 1 — Apprentice
			new SkillTier(5, List.of(
				new GateRequirement("craft_total", 500),
				new GateRequirement("craft_crafting_table", 1),
				new GateRequirement("place_containers", 8)
			)),
			// Tier 2 — Organiser
			new SkillTier(10, List.of(
				new GateRequirement("craft_total", 2000),
				new GateRequirement("workstation_checklist", 8),
				new GateRequirement("deposit_items", 2000),
				new GateRequirement("craft_chests", 32)
			)),
			// Tier 3 — Quartermaster
			new SkillTier(15, List.of(
				new GateRequirement("craft_total", 6000),
				new GateRequirement("craft_tools", 128),
				new GateRequirement("craft_shulker_boxes", 4),
				new GateRequirement("sort_actions", 100)
			)),
			// Tier 4 — Master Artisan
			new SkillTier(20, List.of(
				new GateRequirement("craft_total", 15000),
				new GateRequirement("deposit_items", 15000),
				new GateRequirement("recipe_checklist", 12),
				new GateRequirement("anvil_combines", 25)
			)),
			// Tier 5 — Hand of Order
			new SkillTier(30, List.of(
				new GateRequirement("craft_total", 30000),
				new GateRequirement("containers_sorted", 500),
				new GateRequirement("deposit_items", 40000)
			))
		),
		List.of(
			// Tier 1
			SkillNode.of("sorters_hand_1", 0, 3, SkillType.PASSIVE).icon(Items.OAK_PLANKS)
				.costing(mat(Items.OAK_PLANKS, 16), mat(Items.IRON_INGOT, 4)),
			SkillNode.of("chest_search_1", 0, 4, SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.OAK_PLANKS, 32), mat(Items.CHEST, 1)),
			SkillNode.of("steady_grid", 0, 3, SkillType.PASSIVE).icon(Items.CRAFTING_TABLE)
				.costing(mat(Items.CRAFTING_TABLE, 1), mat(Items.OAK_PLANKS, 32)),
			// Tier 2
			SkillNode.chained("sorters_hand_2", 1, 5, "sorters_hand_1", SkillType.PASSIVE).icon(Items.CHEST)
				.costing(mat(Items.CHEST, 1), mat(Items.IRON_INGOT, 8)),
			SkillNode.chained("chest_search_2", 1, 6, "chest_search_1", SkillType.PASSIVE).icon(Items.ENDER_PEARL)
				.costing(mat(Items.IRON_INGOT, 16), mat(Items.ENDER_PEARL, 1)),
			SkillNode.of("deft_hands", 1, 5, SkillType.PASSIVE).icon(Items.IRON_INGOT)
				.costing(mat(Items.IRON_INGOT, 16), mat(Items.OAK_PLANKS, 16)),
			// Tier 3
			SkillNode.of("slot_lock", 2, 7, SkillType.PASSIVE).icon(Items.OBSIDIAN)
				.costing(mat(Items.IRON_INGOT, 8), mat(Items.OBSIDIAN, 4)),
			SkillNode.chained("tidy_chests", 2, 8, "sorters_hand_2", SkillType.PASSIVE).icon(Items.BARREL)
				.costing(mat(Items.CHEST, 16), mat(Items.GOLD_INGOT, 8)),
			SkillNode.of("sort_profiles", 2, 6, SkillType.PASSIVE).icon(Items.PAPER)
				.costing(mat(Items.GOLD_INGOT, 8), mat(Items.PAPER, 16)),
			// Tier 4
			SkillNode.of("restock_nearby", 3, 10, SkillType.PASSIVE).icon(Items.HOPPER)
				.costing(mat(Items.GOLD_INGOT, 32), mat(Items.DIAMOND, 8)),
			// The two pieces of the quartermaster's kit still to be built.
			SkillNode.of("shulker_sight", 3, 10, SkillType.PASSIVE).icon(Items.SHULKER_BOX)
				.costing(mat(Items.SHULKER_SHELL, 4), mat(Items.ENDER_PEARL, 8)).future(),
			SkillNode.of("auto_block", 3, 9, SkillType.PASSIVE).icon(Items.IRON_BLOCK)
				.costing(mat(Items.IRON_BLOCK, 8), mat(Items.GOLD_BLOCK, 4)),
			// Tier 5 — capstone
			SkillNode.of("hand_of_order", 4, 20, SkillType.PASSIVE).icon(Items.ENDER_CHEST)
				.costing(mat(Items.EMERALD_BLOCK, 4), mat(Items.DIAMOND, 8), mat(Items.CHEST, 64))
		)
	);

	// ---------- Sword — Path of the Blade ----------
	/**
	 * The combat class, and the only tree that is seven tiers rather than five.
	 * It covers every weapon that hits — sword, trident, mace, axe-as-weapon and
	 * the 26.2 spear — because most of what makes a combat node interesting is
	 * weapon-agnostic, and four more tabs would not fit the strip.
	 *
	 * <p>Two rules are specific to this tree. Nodes marked {@code .pve()} never
	 * fire against another player: armour penetration, execute damage and
	 * stacking damage-per-kill are all fine against a zombie and would rewrite
	 * PvP. <b>Nostalgy</b> is the deliberate exception — a 1.8 attack cooldown
	 * is the one node here meant to be felt in a duel. And {@code death_eyes} is
	 * the mod's first {@code .endOfTree()} node: it opens only once the rest
	 * of the tree is bought.
	 */
	public static final SkillTree SWORD = new SkillTree(
		"sword",
		Items.DIAMOND_SWORD,
		List.of(
			// Tier 1 — Duelist
			new SkillTier(5, List.of(
				new GateRequirement("kill_hostiles", 100),
				new GateRequirement("craft_iron_sword", 1),
				new GateRequirement("crit_kills", 50)
			)),
			// Tier 2 — Skirmisher
			new SkillTier(10, List.of(
				new GateRequirement("kill_hostiles", 250),
				new GateRequirement("melee_damage", 5000),
				new GateRequirement("mob_checklist", 5)
			)),
			// Tier 3 — Warblade
			new SkillTier(15, List.of(
				new GateRequirement("kill_hostiles", 500),
				new GateRequirement("own_trident", 1),
				new GateRequirement("sweep_hits", 100)
			)),
			// Tier 4 — Slayer
			new SkillTier(20, List.of(
				new GateRequirement("kill_hostiles", 800),
				new GateRequirement("survive_raid", 1),
				new GateRequirement("nether_kills", 100)
			)),
			// Tier 5 — Champion
			new SkillTier(25, List.of(
				new GateRequirement("kill_hostiles", 1200),
				new GateRequirement("mace_slams", 50),
				new GateRequirement("melee_damage", 25000),
				new GateRequirement("slay_dragon", 1)
			)),
			// Tier 6 — Warlord
			new SkillTier(30, List.of(
				new GateRequirement("kill_hostiles", 1750),
				new GateRequirement("slay_boss", 1),
				new GateRequirement("desperate_kills", 100)
			)),
			// Tier 7 — Legend
			new SkillTier(40, List.of(
				new GateRequirement("kill_hostiles", 2500),
				new GateRequirement("mob_checklist", 30),
				new GateRequirement("boss_checklist", 4)
			))
		),
		List.of(
			// Tier 1 — the basics of holding a weapon
			SkillNode.of("keen_edge_1", 0, 4, SkillType.ENCHANTMENT).icon(Items.IRON_SWORD)
				.costing(mat(Items.IRON_INGOT, 8), mat(Items.FLINT, 8))
				.enchantFor(20),
			SkillNode.of("combat_magnet", 0, 5, SkillType.PASSIVE).icon(Items.HOPPER)
				.costing(mat(Items.IRON_INGOT, 8), mat(Items.HOPPER, 1)),
			SkillNode.of("butchers_cut", 0, 4, SkillType.PASSIVE).icon(Items.COOKED_BEEF)
				.costing(mat(Items.LEATHER, 16), mat(Items.COOKED_BEEF, 8)),
			// Tier 2 — reach and information
			SkillNode.chained("keen_edge_2", 1, 6, "keen_edge_1", SkillType.ENCHANTMENT).icon(Items.GOLDEN_SWORD)
				.costing(mat(Items.IRON_INGOT, 16), mat(Items.BONE, 32))
				.enchantFor(35),
			SkillNode.of("sweeping_arc_1", 1, 6, SkillType.ENCHANTMENT).icon(Items.STRING)
				.costing(mat(Items.STRING, 32), mat(Items.IRON_INGOT, 8))
				.enchantFor(20),
			SkillNode.of("broad_swing", 1, 7, SkillType.PASSIVE).icon(Items.IRON_AXE)
				.costing(mat(Items.IRON_AXE, 1), mat(Items.FLINT, 16)),
			SkillNode.of("second_wind", 1, 5, SkillType.PASSIVE).icon(Items.BREAD)
				.costing(mat(Items.COOKED_BEEF, 16), mat(Items.BREAD, 16)),
			SkillNode.of("hunters_mark", 1, 5, SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.SPIDER_EYE, 8), mat(Items.GLOWSTONE_DUST, 16)),
			// Tier 3 — finishing a fight
			SkillNode.chained("keen_edge_3", 2, 9, "keen_edge_2", SkillType.ENCHANTMENT).icon(Items.DIAMOND_SWORD)
				.costing(mat(Items.DIAMOND, 6), mat(Items.QUARTZ, 32))
				.enchantFor(50),
			SkillNode.chained("sweeping_arc_2", 2, 8, "sweeping_arc_1", SkillType.ENCHANTMENT).icon(Items.PHANTOM_MEMBRANE)
				.costing(mat(Items.STRING, 64), mat(Items.GOLD_INGOT, 16))
				.enchantFor(35),
			SkillNode.of("executioner_1", 2, 8, SkillType.ENCHANTMENT).icon(Items.SKELETON_SKULL)
				.costing(mat(Items.BONE, 32), mat(Items.ROTTEN_FLESH, 32))
				.enchantFor(20).pve(),
			SkillNode.of("tidecaller_1", 2, 9, SkillType.ENCHANTMENT).icon(Items.NAUTILUS_SHELL)
				.costing(mat(Items.PRISMARINE_SHARD, 16), mat(Items.NAUTILUS_SHELL, 1))
				.enchantFor(20),
			SkillNode.of("riposte", 2, 8, SkillType.PASSIVE).icon(Items.SHIELD)
				.costing(mat(Items.SHIELD, 2), mat(Items.IRON_INGOT, 16)),
			// Tier 4 — weapon identity
			SkillNode.chained("executioner_2", 3, 10, "executioner_1", SkillType.ENCHANTMENT).icon(Items.BONE_BLOCK)
				.costing(mat(Items.BONE_BLOCK, 8), mat(Items.GOLD_INGOT, 16))
				.enchantFor(35).pve(),
			SkillNode.of("gravity_well_1", 3, 10, SkillType.ENCHANTMENT).icon(Items.MACE)
				.costing(mat(Items.BREEZE_ROD, 2), mat(Items.IRON_INGOT, 32))
				.enchantFor(20).pve(),
			SkillNode.of("phalanx_1", 3, 10, SkillType.ENCHANTMENT).icon(Items.IRON_SPEAR)
				.costing(mat(Items.IRON_SPEAR, 1), mat(Items.COPPER_INGOT, 32))
				.enchantFor(20).pve(),
			SkillNode.of("cleave", 3, 9, SkillType.PASSIVE).icon(Items.DIAMOND_AXE)
				.costing(mat(Items.DIAMOND_AXE, 1), mat(Items.DIAMOND, 4)).pve(),
			SkillNode.of("nostalgy_1", 3, 12, SkillType.ENCHANTMENT).icon(Items.CLOCK)
				.costing(mat(Items.IRON_INGOT, 32), mat(Items.REDSTONE, 32))
				.enchantFor(20),
			// Tier 5 — the fight, not the hit
			SkillNode.chained("executioner_3", 4, 14, "executioner_2", SkillType.ENCHANTMENT).icon(Items.WITHER_SKELETON_SKULL)
				.costing(mat(Items.NETHERITE_SCRAP, 2), mat(Items.BONE_BLOCK, 16))
				.enchantFor(50).pve(),
			SkillNode.chained("gravity_well_2", 4, 12, "gravity_well_1", SkillType.ENCHANTMENT).icon(Items.HEAVY_CORE)
				.costing(mat(Items.BREEZE_ROD, 4), mat(Items.OBSIDIAN, 16))
				.enchantFor(35).pve(),
			SkillNode.chained("phalanx_2", 4, 12, "phalanx_1", SkillType.ENCHANTMENT).icon(Items.DIAMOND_SPEAR)
				.costing(mat(Items.DIAMOND, 4), mat(Items.COPPER_INGOT, 64))
				.enchantFor(35).pve(),
			SkillNode.chained("tidecaller_2", 4, 12, "tidecaller_1", SkillType.ENCHANTMENT).icon(Items.PRISMARINE_CRYSTALS)
				.costing(mat(Items.PRISMARINE_CRYSTALS, 16), mat(Items.NAUTILUS_SHELL, 3))
				.enchantFor(35),
			SkillNode.of("adrenaline", 4, 12, SkillType.PASSIVE).icon(Items.BLAZE_POWDER)
				.costing(mat(Items.BLAZE_POWDER, 16), mat(Items.SUGAR, 32)).pve(),
			SkillNode.of("storm_bearer", 4, 12, SkillType.PASSIVE).icon(Items.TRIDENT)
				.costing(mat(Items.COPPER_INGOT, 64), mat(Items.PRISMARINE_SHARD, 32)),
			// Migrated out of the Axe tree, where a pure PvP node had no business
			// sitting in a woodcutting class. The old id is honoured on load.
			SkillNode.of("shield_breaker", 4, 8, SkillType.PASSIVE).icon(Items.SHIELD)
				.costing(mat(Items.SHIELD, 2), mat(Items.IRON_INGOT, 16)),
			SkillNode.chained("nostalgy_2", 4, 14, "nostalgy_1", SkillType.ENCHANTMENT).icon(Items.CLOCK)
				.costing(mat(Items.GOLD_BLOCK, 2), mat(Items.REDSTONE_BLOCK, 4))
				.enchantFor(35),
			// Tier 6 — the arguable ones
			SkillNode.of("sundering_blow_1", 5, 14, SkillType.ENCHANTMENT).icon(Items.NETHERITE_SCRAP)
				.costing(mat(Items.NETHERITE_SCRAP, 1), mat(Items.DIAMOND, 8))
				.enchantFor(20).pve(),
			SkillNode.chained("sundering_blow_2", 5, 16, "sundering_blow_1", SkillType.ENCHANTMENT).icon(Items.NETHERITE_INGOT)
				.costing(mat(Items.NETHERITE_INGOT, 1), mat(Items.OBSIDIAN, 16))
				.enchantFor(35).pve(),
			SkillNode.of("bloodthirst", 5, 16, SkillType.PASSIVE).icon(Items.GHAST_TEAR)
				.costing(mat(Items.GHAST_TEAR, 4), mat(Items.GOLDEN_APPLE, 2)).pve(),
			SkillNode.of("headhunter", 5, 14, SkillType.PASSIVE).icon(Items.ZOMBIE_HEAD)
				.costing(mat(Items.BONE_BLOCK, 16), mat(Items.ROTTEN_FLESH, 64)).pve(),
			SkillNode.chained("nostalgy_3", 5, 16, "nostalgy_2", SkillType.ENCHANTMENT).icon(Items.CLOCK)
				.costing(mat(Items.DIAMOND_BLOCK, 1), mat(Items.AMETHYST_SHARD, 16))
				.enchantFor(50),
			// Tier 7 — the end of the class
			SkillNode.chained("nostalgy_4", 6, 20, "nostalgy_3", SkillType.ENCHANTMENT).icon(Items.CLOCK)
				.costing(mat(Items.NETHERITE_INGOT, 2), mat(Items.ECHO_SHARD, 8))
				.enchantFor(70),
			SkillNode.capstone("spoils_of_war", 6, 20, SkillType.PASSIVE, "warlords_wake").icon(Items.EMERALD_BLOCK)
				.costing(mat(Items.EMERALD_BLOCK, 4), mat(Items.DIAMOND, 16)),
			SkillNode.capstone("warlords_wake", 6, 20, SkillType.PASSIVE, "spoils_of_war").icon(Items.WIND_CHARGE)
				.costing(mat(Items.BREEZE_ROD, 8), mat(Items.NETHERITE_INGOT, 1)).pve(),
			SkillNode.of("death_eyes", 6, 30, SkillType.PASSIVE).icon(Items.WITHER_SKELETON_SKULL)
				.costing(mat(Items.NETHER_STAR, 1), mat(Items.WITHER_SKELETON_SKULL, 3), mat(Items.ECHO_SHARD, 8))
				.pve().endOfTree()
		)
	);

	// ---------- Armor — Path of the Bulwark ----------

	/**
	 * The one class that cannot level from <em>doing</em> something: wearing
	 * armour is not an action. It levels from <b>damage survived</b> — what the
	 * set absorbed, what the shield stopped, what the player walked away from —
	 * which is also what makes it the only tree that advances while you play
	 * every other one.
	 *
	 * <p>Seven tiers, like the Sword, and for the same reason: the defensive
	 * kit splits into more distinct steps than five columns can hold without
	 * two unrelated ideas sharing a tier. Tiers 4 and 6 — Bastion and Aegis
	 * Bearer — are the two the five-tier sketch in issue #28 did not have.
	 *
	 * <p>The tier 7 capstones are a genuine pick-one of three, which is what
	 * made {@link SkillNode#exclusiveWith()} a list rather than a single id.
	 */
	public static final SkillTree ARMOR = new SkillTree(
		"armor",
		Items.DIAMOND_CHESTPLATE,
		List.of(
			// Tier 1 — Padded
			new SkillTier(5, List.of(
				new GateRequirement("absorb_damage", 250),
				new GateRequirement("craft_iron_armor", 4),
				new GateRequirement("block_damage", 100)
			)),
			// Tier 2 — Ironclad
			new SkillTier(10, List.of(
				new GateRequirement("absorb_damage", 1000),
				new GateRequirement("survive_falls", 50),
				new GateRequirement("wear_diamond_minutes", 20)
			)),
			// Tier 3 — Warden of the Gate
			new SkillTier(15, List.of(
				new GateRequirement("survive_fire_seconds", 30),
				new GateRequirement("block_hits", 100),
				new GateRequirement("low_health_kills", 50)
			)),
			// Tier 4 — Bastion
			new SkillTier(20, List.of(
				new GateRequirement("absorb_damage", 3000),
				new GateRequirement("block_damage", 1000),
				new GateRequirement("survive_explosions", 25)
			)),
			// Tier 5 — Netherite Wall
			new SkillTier(25, List.of(
				new GateRequirement("own_netherite_set", 1),
				new GateRequirement("absorb_damage", 5000),
				new GateRequirement("survive_wither_skull", 1)
			)),
			// Tier 6 — Aegis Bearer
			new SkillTier(30, List.of(
				new GateRequirement("absorb_damage", 9000),
				new GateRequirement("block_hits", 500),
				new GateRequirement("wear_netherite_minutes", 60),
				new GateRequirement("trim_full_set", 1)
			)),
			// Tier 7 — Unbreaking Line
			new SkillTier(40, List.of(
				new GateRequirement("armor_checklist", 6),
				new GateRequirement("absorb_damage", 20000),
				new GateRequirement("block_damage", 5000)
			))
		),
		List.of(
			// Tier 1 — kit management
			SkillNode.of("padded_lining_1", 0, 3, SkillType.PASSIVE).icon(Items.LEATHER)
				.costing(mat(Items.LEATHER, 16), mat(Items.IRON_INGOT, 4)),
			SkillNode.of("set_sense", 0, 3, SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.IRON_INGOT, 8), mat(Items.GLASS, 4)),
			SkillNode.of("shield_wall_1", 0, 4, SkillType.PASSIVE).icon(Items.SHIELD)
				.costing(mat(Items.SHIELD, 1), mat(Items.IRON_INGOT, 8)),
			SkillNode.of("steady_stance", 0, 4, SkillType.PASSIVE).icon(Items.IRON_BOOTS)
				.costing(mat(Items.IRON_INGOT, 12), mat(Items.LEATHER, 16)),
			// Tier 2 — the environment
			SkillNode.of("flashpoint", 1, 6, SkillType.PASSIVE).icon(Items.LAVA_BUCKET)
				.costing(mat(Items.MAGMA_CREAM, 8), mat(Items.OBSIDIAN, 4)),
			SkillNode.of("thermal_weave_1", 1, 6, SkillType.ENCHANTMENT).icon(Items.MAGMA_CREAM)
				.costing(mat(Items.MAGMA_CREAM, 8), mat(Items.BLAZE_POWDER, 4))
				.enchantFor(20),
			SkillNode.of("sure_footing", 1, 5, SkillType.PASSIVE).icon(Items.SOUL_SAND)
				.costing(mat(Items.SOUL_SAND, 16), mat(Items.HONEYCOMB, 8)),
			SkillNode.chained("padded_lining_2", 1, 5, "padded_lining_1", SkillType.PASSIVE).icon(Items.RABBIT_HIDE)
				.costing(mat(Items.LEATHER, 32), mat(Items.IRON_INGOT, 16)),
			// Tier 3 — the gear itself
			SkillNode.of("second_skin", 2, 7, SkillType.PASSIVE).icon(Items.PHANTOM_MEMBRANE)
				.costing(mat(Items.PHANTOM_MEMBRANE, 8), mat(Items.GOLD_INGOT, 8)),
			SkillNode.of("ablative_plating_1", 2, 7, SkillType.ENCHANTMENT).icon(Items.TNT)
				.costing(mat(Items.GUNPOWDER, 16), mat(Items.IRON_INGOT, 16))
				.enchantFor(20),
			SkillNode.chained("shield_wall_2", 2, 7, "shield_wall_1", SkillType.PASSIVE).icon(Items.IRON_BLOCK)
				.costing(mat(Items.IRON_BLOCK, 2), mat(Items.SHIELD, 1)),
			SkillNode.chained("thermal_weave_2", 2, 8, "thermal_weave_1", SkillType.ENCHANTMENT).icon(Items.MAGMA_BLOCK)
				.costing(mat(Items.MAGMA_CREAM, 16), mat(Items.BLAZE_ROD, 8))
				.enchantFor(35),
			// Tier 4 — standing your ground
			SkillNode.of("bulwark_1", 3, 9, SkillType.ENCHANTMENT).icon(Items.SHIELD)
				.costing(mat(Items.IRON_INGOT, 16), mat(Items.OAK_PLANKS, 32))
				.enchantFor(20),
			SkillNode.of("thorned_plate_1", 3, 9, SkillType.ENCHANTMENT).icon(Items.CACTUS)
				.costing(mat(Items.CACTUS, 16), mat(Items.IRON_INGOT, 16))
				.enchantFor(20),
			SkillNode.of("last_stand", 3, 10, SkillType.PASSIVE).icon(Items.GOLDEN_APPLE)
				.costing(mat(Items.GOLD_INGOT, 16), mat(Items.GHAST_TEAR, 2)),
			SkillNode.chained("padded_lining_3", 3, 9, "padded_lining_2", SkillType.PASSIVE).icon(Items.DIAMOND_CHESTPLATE)
				.costing(mat(Items.LEATHER, 64), mat(Items.DIAMOND, 4)),
			// Tier 5 — the set as a whole
			SkillNode.of("repair_rites", 4, 12, SkillType.PASSIVE).icon(Items.ANVIL)
				.costing(mat(Items.IRON_BLOCK, 2), mat(Items.AMETHYST_SHARD, 8)),
			SkillNode.of("kinetic_plating", 4, 12, SkillType.ENCHANTMENT).icon(Items.SLIME_BLOCK)
				.costing(mat(Items.SLIME_BLOCK, 4), mat(Items.PHANTOM_MEMBRANE, 16))
				.enchantFor(35),
			SkillNode.chained("ablative_plating_2", 4, 12, "ablative_plating_1", SkillType.ENCHANTMENT).icon(Items.GUNPOWDER)
				.costing(mat(Items.GUNPOWDER, 32), mat(Items.DIAMOND, 4))
				.enchantFor(35),
			SkillNode.chained("bulwark_2", 4, 12, "bulwark_1", SkillType.ENCHANTMENT).icon(Items.NETHERITE_SCRAP)
				.costing(mat(Items.DIAMOND, 8), mat(Items.IRON_BLOCK, 2))
				.enchantFor(35),
			// Tier 6 — the set as a whole, and the people beside you
			SkillNode.of("guardians_aura", 5, 16, SkillType.PASSIVE).icon(Items.BEACON)
				.costing(mat(Items.DIAMOND, 8), mat(Items.GOLD_BLOCK, 1)),
			SkillNode.of("wardens_weight", 5, 14, SkillType.PASSIVE).icon(Items.NETHERITE_INGOT)
				.costing(mat(Items.NETHERITE_INGOT, 1), mat(Items.OBSIDIAN, 16)),
			SkillNode.of("nightplate", 5, 14, SkillType.PASSIVE).icon(Items.GOLDEN_CHESTPLATE)
				.costing(mat(Items.GOLD_INGOT, 32), mat(Items.LEATHER, 32)),
			SkillNode.chained("thorned_plate_2", 5, 14, "thorned_plate_1", SkillType.ENCHANTMENT).icon(Items.SWEET_BERRIES)
				.costing(mat(Items.CACTUS, 32), mat(Items.DIAMOND, 4))
				.enchantFor(35),
			SkillNode.chained("bulwark_3", 5, 16, "bulwark_2", SkillType.ENCHANTMENT).icon(Items.NETHERITE_INGOT)
				.costing(mat(Items.NETHERITE_INGOT, 1), mat(Items.DIAMOND, 8))
				.enchantFor(50),
			// Tier 7 — capstones (pick one of three)
			SkillNode.capstone("aegis", 6, 20, SkillType.PASSIVE, "immortal_line", "living_armor")
				.icon(Items.NETHERITE_CHESTPLATE)
				.costing(mat(Items.NETHERITE_INGOT, 2), mat(Items.DIAMOND_BLOCK, 1), mat(Items.EMERALD, 32)),
			SkillNode.capstone("immortal_line", 6, 20, SkillType.PASSIVE, "aegis", "living_armor")
				.icon(Items.TOTEM_OF_UNDYING)
				.costing(mat(Items.TOTEM_OF_UNDYING, 1), mat(Items.NETHERITE_INGOT, 1), mat(Items.GHAST_TEAR, 8)),
			SkillNode.capstone("living_armor", 6, 20, SkillType.PASSIVE, "aegis", "immortal_line")
				.icon(Items.EXPERIENCE_BOTTLE)
				.costing(mat(Items.EXPERIENCE_BOTTLE, 32), mat(Items.NETHERITE_INGOT, 1), mat(Items.AMETHYST_SHARD, 16))
		)
	);

	// ---------- Bow — Path of the Arrow ----------

	/**
	 * The ranged class: bow, crossbow and everything that leaves your hand and
	 * travels. Its whole identity is <b>distance</b> — nodes get better the
	 * further the shot, and none of them makes close-range archery the answer,
	 * because that fight already belongs to the Sword tree.
	 *
	 * <p>Seven tiers, like Sword and Armor, and for the same structural reason
	 * plus a new one: this tree carries <b>eleven</b> {@code .pve()} nodes —
	 * distance-scaled damage, a root, a kill that bounces, an outline through
	 * the dark — every one of them reasonable against a skeleton and a rewrite
	 * of PvP against a player. Spreading them across seven tiers keeps each
	 * tier's power step honest instead of stacking three sharp knives per
	 * column. What stays live in a duel is deliberate: Swift Draw and Rapid
	 * Reload (you move while you aim — visible, symmetric, the class's feel),
	 * Fletcher's Hands, Steady Aim, Gale, and the capstones that are economy or
	 * mechanics rather than burst.
	 */
	public static final SkillTree BOW = new SkillTree(
		"bow",
		Items.BOW,
		List.of(
			// Tier 1 — Fletcher
			new SkillTier(5, List.of(
				new GateRequirement("arrows_fired", 150),
				new GateRequirement("arrows_hit", 100),
				new GateRequirement("craft_bow", 1)
			)),
			// Tier 2 — Marksman
			new SkillTier(10, List.of(
				new GateRequirement("arrows_fired", 400),
				new GateRequirement("arrows_hit", 300),
				new GateRequirement("bow_kills", 50)
			)),
			// Tier 3 — Sharpshooter
			new SkillTier(15, List.of(
				new GateRequirement("bow_kills", 150),
				new GateRequirement("kills_30", 25),
				new GateRequirement("craft_crossbow", 1)
			)),
			// Tier 4 — Hawkeye
			new SkillTier(20, List.of(
				new GateRequirement("crossbow_kills", 50),
				new GateRequirement("arrows_hit", 800),
				new GateRequirement("multishot_kills", 10)
			)),
			// Tier 5 — Deadeye
			new SkillTier(25, List.of(
				new GateRequirement("kills_60", 25),
				new GateRequirement("arrows_hit", 1500),
				new GateRequirement("phantom_air_kills", 10)
			)),
			// Tier 6 — Windrunner
			new SkillTier(30, List.of(
				new GateRequirement("ranged_kills", 300),
				new GateRequirement("tipped_checklist", 3),
				new GateRequirement("arrows_hit", 2500)
			)),
			// Tier 7 — Eye of the Storm
			new SkillTier(40, List.of(
				new GateRequirement("ranged_kills", 500),
				new GateRequirement("kills_60", 75),
				new GateRequirement("tipped_checklist", 8)
			))
		),
		List.of(
			// Tier 1 — the draw
			SkillNode.of("fletchers_hands_1", 0, 4, SkillType.PASSIVE).icon(Items.BOW)
				.costing(mat(Items.STRING, 16), mat(Items.STICK, 32)),
			SkillNode.of("swift_draw_1", 0, 4, SkillType.PASSIVE).icon(Items.FEATHER)
				.costing(mat(Items.FEATHER, 16), mat(Items.LEATHER, 8)),
			SkillNode.of("quiver_sense", 0, 3, SkillType.PASSIVE).icon(Items.ARROW)
				.costing(mat(Items.ARROW, 32), mat(Items.GLASS, 4)),
			SkillNode.of("arrow_recovery_1", 0, 5, SkillType.PASSIVE).icon(Items.FLINT)
				.costing(mat(Items.FLINT, 32), mat(Items.FEATHER, 16)),
			// Tier 2 — the shot
			SkillNode.chained("swift_draw_2", 1, 6, "swift_draw_1", SkillType.PASSIVE).icon(Items.RABBIT_FOOT)
				.costing(mat(Items.FEATHER, 32), mat(Items.SUGAR, 16)),
			SkillNode.of("long_shot_1", 1, 6, SkillType.ENCHANTMENT).icon(Items.SPYGLASS)
				.costing(mat(Items.STRING, 32), mat(Items.IRON_INGOT, 8))
				.enchantFor(20).pve(),
			SkillNode.of("steady_aim", 1, 5, SkillType.PASSIVE).icon(Items.TARGET)
				.costing(mat(Items.TARGET, 1), mat(Items.STRING, 16)),
			SkillNode.of("fletchers_bench", 1, 5, SkillType.PASSIVE).icon(Items.FLETCHING_TABLE)
				.costing(mat(Items.FLETCHING_TABLE, 1), mat(Items.FEATHER, 32)),
			// Tier 3 — the quiver
			SkillNode.chained("fletchers_hands_2", 2, 7, "fletchers_hands_1", SkillType.PASSIVE).icon(Items.CROSSBOW)
				.costing(mat(Items.STRING, 64), mat(Items.IRON_INGOT, 16)),
			SkillNode.of("gale_1", 2, 8, SkillType.ENCHANTMENT).icon(Items.WIND_CHARGE)
				.costing(mat(Items.PHANTOM_MEMBRANE, 8), mat(Items.FEATHER, 32))
				.enchantFor(20),
			SkillNode.chained("long_shot_2", 2, 9, "long_shot_1", SkillType.ENCHANTMENT).icon(Items.SPYGLASS)
				.costing(mat(Items.GOLD_INGOT, 16), mat(Items.STRING, 64))
				.enchantFor(35).pve(),
			SkillNode.of("ricochet_1", 2, 8, SkillType.ENCHANTMENT).icon(Items.SLIME_BALL)
				.costing(mat(Items.SLIME_BALL, 16), mat(Items.STRING, 32))
				.enchantFor(20).pve(),
			SkillNode.of("piercing_sight", 2, 7, SkillType.PASSIVE).icon(Items.GLOW_INK_SAC)
				.costing(mat(Items.SPIDER_EYE, 8), mat(Items.GLOWSTONE_DUST, 16)).pve(),
			// Tier 4 — the crossbow's column
			SkillNode.of("rapid_reload_1", 3, 9, SkillType.PASSIVE).icon(Items.REDSTONE)
				.costing(mat(Items.CROSSBOW, 1), mat(Items.REDSTONE, 32)),
			SkillNode.chained("arrow_recovery_2", 3, 9, "arrow_recovery_1", SkillType.PASSIVE).icon(Items.BUNDLE)
				.costing(mat(Items.FLINT, 64), mat(Items.EMERALD, 8)),
			SkillNode.chained("gale_2", 3, 10, "gale_1", SkillType.ENCHANTMENT).icon(Items.BREEZE_ROD)
				.costing(mat(Items.BREEZE_ROD, 2), mat(Items.PHANTOM_MEMBRANE, 16))
				.enchantFor(35),
			SkillNode.of("multishot_focus", 3, 10, SkillType.PASSIVE).icon(Items.FIREWORK_STAR)
				.costing(mat(Items.QUARTZ, 16), mat(Items.CROSSBOW, 1)).pve(),
			SkillNode.of("alchemists_quiver", 3, 9, SkillType.PASSIVE).icon(Items.TIPPED_ARROW)
				.costing(mat(Items.GLASS_BOTTLE, 16), mat(Items.GLOWSTONE_DUST, 16)).pve(),
			// Tier 5 — the hard shots
			SkillNode.chained("fletchers_hands_3", 4, 12, "fletchers_hands_2", SkillType.PASSIVE).icon(Items.DIAMOND)
				.costing(mat(Items.DIAMOND, 6), mat(Items.STRING, 64)),
			SkillNode.chained("long_shot_3", 4, 14, "long_shot_2", SkillType.ENCHANTMENT).icon(Items.SPYGLASS)
				.costing(mat(Items.DIAMOND, 6), mat(Items.AMETHYST_SHARD, 16))
				.enchantFor(50).pve(),
			SkillNode.chained("rapid_reload_2", 4, 12, "rapid_reload_1", SkillType.PASSIVE).icon(Items.DISPENSER)
				.costing(mat(Items.REDSTONE, 64), mat(Items.DIAMOND, 4)),
			SkillNode.of("pinning_shot", 4, 12, SkillType.ENCHANTMENT).icon(Items.COBWEB)
				.costing(mat(Items.COBWEB, 8), mat(Items.IRON_INGOT, 16))
				.enchantFor(20).pve(),
			// Tier 6 — the sky
			SkillNode.chained("swift_draw_3", 5, 14, "swift_draw_2", SkillType.PASSIVE).icon(Items.GOLDEN_BOOTS)
				.costing(mat(Items.GOLD_INGOT, 16), mat(Items.SUGAR, 32)),
			SkillNode.chained("ricochet_2", 5, 14, "ricochet_1", SkillType.ENCHANTMENT).icon(Items.SLIME_BLOCK)
				.costing(mat(Items.SLIME_BLOCK, 4), mat(Items.GOLD_INGOT, 16))
				.enchantFor(35).pve(),
			SkillNode.of("aerial_hunter", 5, 16, SkillType.PASSIVE).icon(Items.PHANTOM_MEMBRANE)
				.costing(mat(Items.PHANTOM_MEMBRANE, 16), mat(Items.FIREWORK_ROCKET, 32)).pve(),
			SkillNode.of("endless_quiver", 5, 14, SkillType.PASSIVE).icon(Items.SPECTRAL_ARROW)
				.costing(mat(Items.SPECTRAL_ARROW, 32), mat(Items.GLOWSTONE_DUST, 32)),
			// Tier 7 — capstones (pick one of three)
			SkillNode.capstone("deadeye", 6, 20, SkillType.PASSIVE, "storm_of_arrows", "hunters_bounty")
				.icon(Items.ENDER_EYE)
				.costing(mat(Items.NETHERITE_INGOT, 1), mat(Items.AMETHYST_SHARD, 16), mat(Items.EMERALD, 32))
				.pve(),
			SkillNode.capstone("storm_of_arrows", 6, 20, SkillType.PASSIVE, "deadeye", "hunters_bounty")
				.icon(Items.FIREWORK_ROCKET)
				.costing(mat(Items.ARROW, 64), mat(Items.BREEZE_ROD, 4), mat(Items.NETHERITE_SCRAP, 2)),
			SkillNode.capstone("hunters_bounty", 6, 20, SkillType.PASSIVE, "deadeye", "storm_of_arrows")
				.icon(Items.ENCHANTED_BOOK)
				.costing(mat(Items.EMERALD_BLOCK, 4), mat(Items.LAPIS_LAZULI, 64), mat(Items.NETHERITE_INGOT, 1))
		)
	);

	/**
	 * Every playable tree, in the order the skill screen tabs them. Adding a
	 * class is one entry here plus its {@code SkillTree} above — the GUI, the
	 * commands, the advancements and the state packet all read this list.
	 */
	public static final List<SkillTree> ORDER =
		List.of(PICKAXE, AXE, ENCHANTER, EXPLORER, ARTISAN, SWORD, ARMOR, BOW);

	/**
	 * Trees whose balance is still being play-tested. Fully playable — the
	 * skill screen just stamps an "in testing" badge on their tabs and nodes
	 * so nobody mistakes their numbers for final.
	 */
	public static final Set<String> IN_TESTING = Set.of("sword", "armor", "bow");

	/**
	 * Classes the design calls for but that have no tree yet. They show up as
	 * greyed "coming soon" tabs so the shape of the mod is visible from day one.
	 */
	public record PlannedTree(String name, Item icon) {
	}

	public static final List<PlannedTree> PLANNED = List.of(
		new PlannedTree("Rod", Items.FISHING_ROD)
	);

	public static final Map<String, SkillTree> ALL = ORDER.stream()
		.collect(Collectors.toMap(SkillTree::id, tree -> tree, (a, b) -> a, LinkedHashMap::new));

	@Nullable
	public static SkillTree byId(String id) {
		return ALL.get(id);
	}
}
