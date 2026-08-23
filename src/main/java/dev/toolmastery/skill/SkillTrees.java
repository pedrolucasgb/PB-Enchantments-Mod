package dev.toolmastery.skill;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Static definitions of every skill tree — the design document in code.
 * Sprint 1 ships Pickaxe, Axe and Enchanter; the other four classes are coming soon.
 *
 * <p>Every node carries two prices. {@code of/chained/capstone} sets the XP half
 * of the <b>unlock</b>, {@code .costing(...)} its materials, and
 * {@code .enchantFor(n)} the repeatable <b>enchant</b> price. Enchant prices sit
 * far above unlock prices on purpose: 20 levels for a rank I, 35 for a rank II,
 * 50 for a rank III and 60 for a capstone, so a table at 30 levels — which also
 * throws in vanilla enchantments — stays the better deal for anyone willing to
 * grind for it.
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
			// (a Prospector's Sense usage gate returns here once the skill ships)
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
			// (a Prospector's Sense diamond-reveal gate returns here once the skill ships)
			new SkillTier(30, List.of(
				new GateRequirement("ore_checklist", 11),
				new GateRequirement("break_total", 10000)
			))
		),
		List.of(
			// Tier 1
			SkillNode.of("masons_grip_1", 0, 3, SkillType.PASSIVE)
				.costing(mat(Items.COBBLESTONE, 64), mat(Items.COAL, 8)),
			SkillNode.of("miners_magnet", 0, 5, SkillType.PASSIVE)
				.costing(mat(Items.IRON_INGOT, 8), mat(Items.HOPPER, 1)),
			SkillNode.of("smelt_1", 0, 4, SkillType.ENCHANTMENT)
				.costing(mat(Items.COAL, 16), mat(Items.IRON_INGOT, 8))
				.enchantFor(20),
			// Tier 2
			SkillNode.chained("masons_grip_2", 1, 5, "masons_grip_1", SkillType.PASSIVE)
				.costing(mat(Items.COBBLESTONE, 128), mat(Items.IRON_INGOT, 16)),
			SkillNode.of("dig_range_1", 1, 6, SkillType.ENCHANTMENT)
				.costing(mat(Items.IRON_INGOT, 12), mat(Items.FLINT, 8))
				.enchantFor(20),
			SkillNode.chained("smelt_2", 1, 6, "smelt_1", SkillType.ENCHANTMENT)
				.costing(mat(Items.COAL, 32), mat(Items.BLAST_FURNACE, 1))
				.enchantFor(35),
			SkillNode.of("prospectors_sense", 1, 8, SkillType.ACTIVE)
				.costing(mat(Items.GOLD_INGOT, 8), mat(Items.LAPIS_LAZULI, 16)).future(),
			// Tier 3
			SkillNode.of("miners_helm", 2, 7, SkillType.ITEM)
				.costing(mat(Items.IRON_INGOT, 16), mat(Items.GLOWSTONE_DUST, 8)).future(),
			SkillNode.chained("masons_grip_3", 2, 8, "masons_grip_2", SkillType.PASSIVE)
				.costing(mat(Items.COBBLED_DEEPSLATE, 128), mat(Items.GOLD_INGOT, 8)),
			SkillNode.chained("dig_range_2", 2, 9, "dig_range_1", SkillType.ENCHANTMENT)
				.costing(mat(Items.DIAMOND, 6), mat(Items.REDSTONE, 32))
				.enchantFor(35),
			SkillNode.chained("smelt_3", 2, 9, "smelt_2", SkillType.ENCHANTMENT)
				.costing(mat(Items.BLAZE_POWDER, 8), mat(Items.COAL, 32))
				.enchantFor(50),
			SkillNode.of("rich_vein_1", 2, 10, SkillType.ENCHANTMENT)
				.costing(mat(Items.DIAMOND, 4), mat(Items.LAPIS_LAZULI, 32))
				.enchantFor(20),
			// Tier 4
			SkillNode.of("deep_haste", 3, 8, SkillType.PASSIVE)
				.costing(mat(Items.DIAMOND_BLOCK, 1), mat(Items.GLOWSTONE_DUST, 16)),
			SkillNode.chained("dig_range_3", 3, 14, "dig_range_2", SkillType.ENCHANTMENT)
				.costing(mat(Items.NETHERITE_INGOT, 1), mat(Items.DIAMOND, 8))
				.enchantFor(50),
			SkillNode.of("obsidian_breaker", 3, 6, SkillType.PASSIVE)
				.costing(mat(Items.OBSIDIAN, 16), mat(Items.DIAMOND, 4)),
			SkillNode.chained("rich_vein_2", 3, 10, "rich_vein_1", SkillType.ENCHANTMENT)
				.costing(mat(Items.DIAMOND, 8), mat(Items.EMERALD_BLOCK, 1))
				.enchantFor(35),
			// Tier 5 — capstones (mutually exclusive)
			SkillNode.capstone("magma_touch", 4, 20, "ancient_fortune", SkillType.ENCHANTMENT)
				.costing(mat(Items.NETHERITE_INGOT, 2), mat(Items.MAGMA_BLOCK, 16), mat(Items.BLAZE_POWDER, 16))
				.enchantFor(60),
			SkillNode.capstone("ancient_fortune", 4, 20, "magma_touch", SkillType.ENCHANTMENT)
				.costing(mat(Items.NETHERITE_INGOT, 2), mat(Items.EMERALD, 32), mat(Items.AMETHYST_SHARD, 16))
				.enchantFor(60)
		)
	);

	// ---------- Axe — Path of the Grove ----------
	public static final SkillTree AXE = new SkillTree(
		"axe",
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
			SkillNode.of("lumberjacks_arms_1", 0, 3, SkillType.PASSIVE)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.IRON_INGOT, 4)),
			SkillNode.of("loggers_magnet", 0, 5, SkillType.PASSIVE)
				.costing(mat(Items.IRON_INGOT, 8), mat(Items.HOPPER, 1)),
			SkillNode.of("fair_harvest", 0, 4, SkillType.PASSIVE)
				.costing(mat(ItemTags.SAPLINGS, 16), mat(Items.BONE_MEAL, 16)),
			// Tier 2
			SkillNode.chained("lumberjacks_arms_2", 1, 5, "lumberjacks_arms_1", SkillType.PASSIVE)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.IRON_INGOT, 16)),
			SkillNode.of("logic_1", 1, 8, SkillType.ENCHANTMENT)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.IRON_INGOT, 8))
				.enchantFor(20),
			SkillNode.of("rich_bark", 1, 4, SkillType.ITEM)
				.costing(mat(ItemTags.LOGS, 32), mat(Items.SHEARS, 1)).future(),
			// Tier 3
			SkillNode.chained("lumberjacks_arms_3", 2, 8, "lumberjacks_arms_2", SkillType.PASSIVE)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.GOLD_INGOT, 8)),
			SkillNode.of("pruner", 2, 6, SkillType.PASSIVE)
				.costing(mat(ItemTags.LEAVES, 64), mat(Items.SHEARS, 2)),
			SkillNode.chained("logic_2", 2, 9, "logic_1", SkillType.ENCHANTMENT)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.DIAMOND, 4))
				.enchantFor(35),
			SkillNode.of("double_axe_1", 2, 7, SkillType.PASSIVE)
				.costing(mat(ItemTags.LOGS, 32), mat(Items.DIAMOND, 4)),
			SkillNode.of("shield_breaker", 2, 8, SkillType.PASSIVE)
				.costing(mat(Items.SHIELD, 2), mat(Items.IRON_INGOT, 16)),
			// Tier 4
			SkillNode.of("call_of_the_forest", 3, 10, SkillType.ACTIVE)
				.costing(mat(Items.BONE_MEAL, 64), mat(Items.EMERALD, 8)).future(),
			SkillNode.chained("logic_3", 3, 12, "logic_2", SkillType.ENCHANTMENT)
				.costing(mat(ItemTags.LOGS, 64), mat(Items.DIAMOND, 8))
				.enchantFor(50),
			SkillNode.chained("double_axe_2", 3, 8, "double_axe_1", SkillType.PASSIVE)
				.costing(mat(Items.DIAMOND, 8), mat(Items.EMERALD_BLOCK, 1)),
			SkillNode.chained("environment", 3, 8, "logic_3", SkillType.ENCHANTMENT)
				.costing(mat(ItemTags.SAPLINGS, 64), mat(Items.BONE_MEAL, 32))
				.enchantFor(25),
			// Tier 5 — capstones (mutually exclusive)
			SkillNode.capstone("lumberjacks_fury", 4, 20, "green_heart", SkillType.ACTIVE)
				.costing(mat(Items.NETHERITE_INGOT, 1), mat(ItemTags.LOGS, 64)).future(),
			SkillNode.capstone("green_heart", 4, 20, "lumberjacks_fury", SkillType.PASSIVE)
				.costing(mat(Items.EMERALD_BLOCK, 4), mat(ItemTags.SAPLINGS, 64)).future()
		)
	);

	// ---------- Enchanter — Path of the Arcane ----------
	public static final SkillTree ENCHANTER = new SkillTree(
		"enchanter",
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
			SkillNode.of("arcane_insight_1", 0, 4, SkillType.PASSIVE)
				.costing(mat(Items.LAPIS_LAZULI, 16), mat(Items.BOOK, 4)),
			SkillNode.of("scholar_1", 0, 3, SkillType.PASSIVE)
				.costing(mat(Items.BOOK, 8), mat(Items.BOOKSHELF, 4)),
			SkillNode.of("book_binder", 0, 4, SkillType.PASSIVE)
				.costing(mat(Items.BOOK, 16), mat(Items.PAPER, 32)).future(),
			// Tier 2
			SkillNode.chained("arcane_insight_2", 1, 6, "arcane_insight_1", SkillType.PASSIVE)
				.costing(mat(Items.LAPIS_LAZULI, 32), mat(Items.BOOKSHELF, 8)),
			SkillNode.chained("scholar_2", 1, 5, "scholar_1", SkillType.PASSIVE)
				.costing(mat(Items.BOOK, 16), mat(Items.BOOKSHELF, 8)),
			SkillNode.of("tome_keeper", 1, 5, SkillType.ITEM)
				.costing(mat(Items.BOOK, 32), mat(Items.LAPIS_LAZULI, 32)).future(),
			// Tier 3
			SkillNode.chained("arcane_insight_3", 2, 9, "arcane_insight_2", SkillType.PASSIVE)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.BOOKSHELF, 16)),
			SkillNode.of("inner_focus", 2, 8, SkillType.PASSIVE)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.ENCHANTING_TABLE, 1)),
			SkillNode.of("anvil_adept", 2, 7, SkillType.PASSIVE)
				.costing(mat(Items.IRON_BLOCK, 4), mat(Items.EMERALD, 8)).future(),
			// Tier 4
			SkillNode.chained("scholar_3", 3, 8, "scholar_2", SkillType.PASSIVE)
				.costing(mat(Items.BOOK, 32), mat(Items.BOOKSHELF, 16), mat(Items.EMERALD, 8)),
			SkillNode.of("arcane_conduit", 3, 10, SkillType.PASSIVE)
				.costing(mat(Items.BOOKSHELF, 32), mat(Items.DIAMOND, 8)).future(),
			SkillNode.of("curse_breaker", 3, 12, SkillType.ACTIVE)
				.costing(mat(Items.EMERALD_BLOCK, 2), mat(Items.LAPIS_LAZULI, 64)).future(),
			// Tier 5 — capstones (mutually exclusive)
			SkillNode.capstone("rewrite_fate", 4, 20, "ancient_knowledge", SkillType.ACTIVE)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.BOOKSHELF, 16), mat(Items.DIAMOND_BLOCK, 4)),
			SkillNode.capstone("ancient_knowledge", 4, 20, "rewrite_fate", SkillType.PASSIVE)
				.costing(mat(Items.LAPIS_LAZULI, 64), mat(Items.BOOKSHELF, 16), mat(Items.EMERALD_BLOCK, 4)).future()
		)
	);

	public static final Map<String, SkillTree> ALL = Map.of(
		PICKAXE.id(), PICKAXE,
		AXE.id(), AXE,
		ENCHANTER.id(), ENCHANTER
	);

	@Nullable
	public static SkillTree byId(String id) {
		return ALL.get(id);
	}
}
