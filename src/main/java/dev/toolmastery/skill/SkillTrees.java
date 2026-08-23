package dev.toolmastery.skill;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Static definitions of every skill tree — the design document in code.
 * Sprint 1 ships Pickaxe and Axe; the other five classes are coming soon.
 */
public final class SkillTrees {
	private SkillTrees() {
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
			SkillNode.of("masons_grip_1", 0, 3, SkillType.PASSIVE).future(),
			SkillNode.of("miners_magnet", 0, 5, SkillType.PASSIVE).future(),
			SkillNode.of("melt_1", 0, 4, SkillType.ENCHANTMENT),
			// Tier 2
			SkillNode.chained("masons_grip_2", 1, 5, "masons_grip_1", SkillType.PASSIVE).future(),
			SkillNode.of("dig_range_1", 1, 6, SkillType.ENCHANTMENT),
			SkillNode.chained("melt_2", 1, 6, "melt_1", SkillType.ENCHANTMENT),
			SkillNode.of("prospectors_sense", 1, 8, SkillType.ACTIVE).future(),
			// Tier 3
			SkillNode.of("miners_helm", 2, 7, SkillType.ITEM).future(),
			SkillNode.chained("dig_range_2", 2, 9, "dig_range_1", SkillType.ENCHANTMENT),
			SkillNode.chained("melt_3", 2, 9, "melt_2", SkillType.ENCHANTMENT),
			SkillNode.of("rich_vein_1", 2, 10, SkillType.ENCHANTMENT),
			// Tier 4
			SkillNode.of("deep_haste", 3, 8, SkillType.PASSIVE).future(),
			SkillNode.chained("dig_range_3", 3, 14, "dig_range_2", SkillType.ENCHANTMENT),
			SkillNode.of("obsidian_breaker", 3, 6, SkillType.PASSIVE).future(),
			SkillNode.chained("rich_vein_2", 3, 10, "rich_vein_1", SkillType.ENCHANTMENT),
			// Tier 5 — capstones (mutually exclusive)
			SkillNode.capstone("magma_touch", 4, 20, "ancient_fortune", SkillType.ENCHANTMENT),
			SkillNode.capstone("ancient_fortune", 4, 20, "magma_touch", SkillType.PASSIVE).future()
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
			SkillNode.of("lumberjacks_arms_1", 0, 3, SkillType.PASSIVE).future(),
			SkillNode.of("loggers_magnet", 0, 5, SkillType.PASSIVE).future(),
			SkillNode.of("fair_harvest", 0, 4, SkillType.PASSIVE).future(),
			// Tier 2
			SkillNode.chained("lumberjacks_arms_2", 1, 5, "lumberjacks_arms_1", SkillType.PASSIVE).future(),
			SkillNode.of("logic_1", 1, 8, SkillType.ENCHANTMENT),
			SkillNode.of("rich_bark", 1, 4, SkillType.ITEM).future(),
			// Tier 3
			SkillNode.of("pruner", 2, 6, SkillType.PASSIVE).future(),
			SkillNode.chained("logic_2", 2, 9, "logic_1", SkillType.ENCHANTMENT),
			SkillNode.of("double_axe_1", 2, 7, SkillType.PASSIVE).future(),
			SkillNode.of("shield_breaker", 2, 8, SkillType.PASSIVE).future(),
			// Tier 4
			SkillNode.of("call_of_the_forest", 3, 10, SkillType.ACTIVE).future(),
			SkillNode.chained("logic_3", 3, 12, "logic_2", SkillType.ENCHANTMENT),
			SkillNode.chained("double_axe_2", 3, 8, "double_axe_1", SkillType.PASSIVE).future(),
			SkillNode.chained("environment", 3, 8, "logic_3", SkillType.ENCHANTMENT),
			// Tier 5 — capstones (mutually exclusive)
			SkillNode.capstone("lumberjacks_fury", 4, 20, "green_heart", SkillType.ACTIVE).future(),
			SkillNode.capstone("green_heart", 4, 20, "lumberjacks_fury", SkillType.PASSIVE).future()
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
			SkillNode.of("arcane_insight_1", 0, 4, SkillType.PASSIVE),
			SkillNode.of("scholar_1", 0, 3, SkillType.PASSIVE),
			SkillNode.of("book_binder", 0, 4, SkillType.PASSIVE).future(),
			// Tier 2
			SkillNode.chained("arcane_insight_2", 1, 6, "arcane_insight_1", SkillType.PASSIVE),
			SkillNode.chained("scholar_2", 1, 5, "scholar_1", SkillType.PASSIVE),
			SkillNode.of("tome_keeper", 1, 5, SkillType.ITEM).future(),
			// Tier 3
			SkillNode.chained("arcane_insight_3", 2, 9, "arcane_insight_2", SkillType.PASSIVE),
			SkillNode.of("inner_focus", 2, 8, SkillType.PASSIVE),
			SkillNode.of("anvil_adept", 2, 7, SkillType.PASSIVE).future(),
			// Tier 4
			SkillNode.chained("scholar_3", 3, 8, "scholar_2", SkillType.PASSIVE),
			SkillNode.of("arcane_conduit", 3, 10, SkillType.PASSIVE).future(),
			SkillNode.of("curse_breaker", 3, 12, SkillType.ACTIVE).future(),
			// Tier 5 — capstones (mutually exclusive)
			SkillNode.capstone("rewrite_fate", 4, 20, "ancient_knowledge", SkillType.ACTIVE),
			SkillNode.capstone("ancient_knowledge", 4, 20, "rewrite_fate", SkillType.PASSIVE).future()
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
