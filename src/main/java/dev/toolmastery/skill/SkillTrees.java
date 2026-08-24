package dev.toolmastery.skill;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Static definitions of every skill tree — the design document in code.
 * Sprint 1 ships Pickaxe, Axe and Enchanter; the other four classes are coming soon.
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
			SkillNode.of("miners_helm", 2, 7, SkillType.ITEM).icon(Items.IRON_HELMET)
				.costing(mat(Items.IRON_INGOT, 16), mat(Items.GLOWSTONE_DUST, 8)).future(),
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
			// Tier 5 — capstone
			SkillNode.of("ancient_fortune", 4, 20, SkillType.PASSIVE).icon(Items.ANCIENT_DEBRIS)
				.costing(mat(Items.NETHERITE_INGOT, 2), mat(Items.EMERALD, 32), mat(Items.AMETHYST_SHARD, 16))
		)
	);

	// ---------- Axe — Path of the Grove ----------
	public static final SkillTree AXE = new SkillTree(
		"axe",
		Items.DIAMOND_AXE,
		List.of(
			// Tier 1 — Apprentice Lumberjack
			new SkillTier(5, List.of(
				new GateRequirement("chop_logs", 256),
				new GateRequirement("craft_iron_axe", 1),
				new GateRequirement("make_charcoal", 64)
			)),
			// Tier 2 — Feller
			new SkillTier(10, List.of(
				new GateRequirement("overworld_wood_checklist", 9),
				new GateRequirement("chop_logs_total", 512),
				new GateRequirement("strip_logs", 32)
			)),
			// Tier 3 — Master Lumberjack
			new SkillTier(15, List.of(
				new GateRequirement("fell_with_logic", 100),
				new GateRequirement("nether_wood_checklist", 2),
				new GateRequirement("break_leaves", 1000)
			)),
			// Tier 4 — Grove Warden
			new SkillTier(20, List.of(
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
			SkillNode.of("rich_bark", 1, 4, SkillType.ITEM).icon(Items.STRIPPED_OAK_LOG)
				.costing(mat(ItemTags.LOGS, 32), mat(Items.SHEARS, 1)).future(),
			// Tier 3
			SkillNode.chained("lumberjacks_arms_3", 2, 8, "lumberjacks_arms_2", SkillType.PASSIVE).icon(Items.DIAMOND_AXE)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.GOLD_INGOT, 8)),
			SkillNode.of("pruner", 2, 6, SkillType.PASSIVE).icon(Items.SHEARS)
				.costing(mat(ItemTags.LEAVES, 64), mat(Items.SHEARS, 2)),
			SkillNode.chained("logic_2", 2, 9, "logic_1", SkillType.ENCHANTMENT).icon(Items.OAK_WOOD)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.DIAMOND, 4))
				.enchantFor(35),
			SkillNode.of("double_axe_1", 2, 7, SkillType.PASSIVE).icon(Items.GOLDEN_AXE)
				.costing(mat(ItemTags.LOGS, 32), mat(Items.DIAMOND, 4)),
			SkillNode.of("shield_breaker", 2, 8, SkillType.PASSIVE).icon(Items.SHIELD)
				.costing(mat(Items.SHIELD, 2), mat(Items.IRON_INGOT, 16)),
			// Tier 4
			SkillNode.chained("logic_3", 3, 12, "logic_2", SkillType.ENCHANTMENT).icon(Items.OAK_LEAVES)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.DIAMOND, 8))
				.enchantFor(50),
			SkillNode.chained("double_axe_2", 3, 8, "double_axe_1", SkillType.PASSIVE).icon(Items.GOLD_BLOCK)
				.costing(mat(Items.DIAMOND, 8), mat(Items.EMERALD_BLOCK, 1)),
			SkillNode.chained("environment", 3, 8, "logic_3", SkillType.ENCHANTMENT).icon(Items.BONE_MEAL)
				.costing(mat(ItemTags.SAPLINGS, 64), mat(Items.BONE_MEAL, 32))
				.enchantFor(25),
			// Tier 5 — capstone
			SkillNode.of("green_heart", 4, 20, SkillType.PASSIVE).icon(Items.FLOWERING_AZALEA)
				.costing(mat(Items.EMERALD_BLOCK, 4), mat(ItemTags.SAPLINGS, 64)).future()
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
				new GateRequirement("spend_levels", 30),
				new GateRequirement("mine_lapis", 64),
				new GateRequirement("craft_books", 32)
			)),
			// Tier 3 — Arcanist
			new SkillTier(15, List.of(
				new GateRequirement("enchant_items", 50),
				new GateRequirement("max_slot_enchants", 10),
				new GateRequirement("reach_level_30", 1)
			)),
			// Tier 4 — Master Enchanter
			new SkillTier(20, List.of(
				new GateRequirement("enchant_items", 100),
				new GateRequirement("spend_levels", 150),
				new GateRequirement("collect_xp", 10000)
			)),
			// Tier 5 — Archmage
			new SkillTier(30, List.of(
				new GateRequirement("enchant_items", 200),
				new GateRequirement("max_slot_enchants", 50),
				new GateRequirement("spend_levels", 400)
			))
		),
		List.of(
			// Tier 1
			SkillNode.of("arcane_insight_1", 0, 4, SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.LAPIS_LAZULI, 16), mat(Items.BOOK, 4)),
			SkillNode.of("scholar_1", 0, 3, SkillType.PASSIVE).icon(Items.EXPERIENCE_BOTTLE)
				.costing(mat(Items.BOOK, 8), mat(Items.BOOKSHELF, 4)),
			SkillNode.of("book_binder", 0, 4, SkillType.PASSIVE).icon(Items.BOOK)
				.costing(mat(Items.BOOK, 16), mat(Items.PAPER, 32)).future(),
			// Tier 2
			SkillNode.chained("arcane_insight_2", 1, 6, "arcane_insight_1", SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.LAPIS_LAZULI, 32), mat(Items.BOOKSHELF, 8)),
			SkillNode.chained("scholar_2", 1, 5, "scholar_1", SkillType.PASSIVE).icon(Items.EXPERIENCE_BOTTLE)
				.costing(mat(Items.BOOK, 16), mat(Items.BOOKSHELF, 8)),
			SkillNode.of("tome_keeper", 1, 5, SkillType.ITEM).icon(Items.WRITABLE_BOOK)
				.costing(mat(Items.BOOK, 32), mat(Items.LAPIS_LAZULI, 32)).future(),
			// Tier 3
			SkillNode.chained("arcane_insight_3", 2, 9, "arcane_insight_2", SkillType.PASSIVE).icon(Items.SPYGLASS)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.BOOKSHELF, 16)),
			SkillNode.of("inner_focus", 2, 8, SkillType.PASSIVE).icon(Items.LAPIS_LAZULI)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.ENCHANTING_TABLE, 1)),
			SkillNode.of("indestructible", 2, 10, SkillType.ENCHANTMENT).icon(Items.PHANTOM_MEMBRANE)
				.costing(mat(Items.PHANTOM_MEMBRANE, 16), mat(Items.LAPIS_LAZULI, 32))
				.enchantFor(40),
			SkillNode.of("anvil_adept", 2, 7, SkillType.PASSIVE).icon(Items.ANVIL)
				.costing(mat(Items.IRON_BLOCK, 4), mat(Items.EMERALD, 8)).future(),
			// Tier 4
			SkillNode.chained("scholar_3", 3, 8, "scholar_2", SkillType.PASSIVE).icon(Items.EXPERIENCE_BOTTLE)
				.costing(mat(Items.BOOK, 32), mat(Items.BOOKSHELF, 16), mat(Items.EMERALD, 8)),
			SkillNode.of("arcane_conduit", 3, 10, SkillType.PASSIVE).icon(Items.BOOKSHELF)
				.costing(mat(Items.BOOKSHELF, 32), mat(Items.DIAMOND, 8)).future(),
			// Tier 5 — capstone
			SkillNode.of("ancient_knowledge", 4, 20, SkillType.PASSIVE).icon(Items.ENCHANTED_BOOK)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.BOOKSHELF, 16), mat(Items.EMERALD_BLOCK, 4)).future()
		)
	);

	/**
	 * Every playable tree, in the order the skill screen tabs them. Adding a
	 * class is one entry here plus its {@code SkillTree} above — the GUI, the
	 * commands, the advancements and the state packet all read this list.
	 */
	public static final List<SkillTree> ORDER = List.of(PICKAXE, AXE, ENCHANTER);

	/**
	 * Classes the design calls for but that have no tree yet. They show up as
	 * greyed "coming soon" tabs so the shape of the mod is visible from day one.
	 */
	public record PlannedTree(String name, Item icon) {
	}

	public static final List<PlannedTree> PLANNED = List.of(
		new PlannedTree("Sword", Items.DIAMOND_SWORD),
		new PlannedTree("Bow", Items.BOW),
		new PlannedTree("Rod", Items.FISHING_ROD),
		new PlannedTree("Armor", Items.DIAMOND_CHESTPLATE)
	);

	public static final Map<String, SkillTree> ALL = ORDER.stream()
		.collect(Collectors.toMap(SkillTree::id, tree -> tree, (a, b) -> a, LinkedHashMap::new));

	@Nullable
	public static SkillTree byId(String id) {
		return ALL.get(id);
	}
}
