package dev.pbenchants.skill;

import dev.pbenchants.progress.TreeProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The contents of every gate that is a <b>closed list</b> rather than a running
 * total — "mine one of each ore", "kill all four bosses".
 *
 * <p>A player looking at "Ore checklist 7/11" cannot act on it: the number says
 * how far they are, never what is left. So each of these gates keeps a bitmask
 * beside its visible counter — one bit per entry, in the order declared here —
 * and this class is the one place that says which bit means what. The trackers
 * set the bits, the skill screen reads them back into a tick-list, and both
 * sides agree because they read the same table.
 *
 * <p>Only closed lists live here. "Kill 30 distinct hostile mobs" or "visit 45
 * biomes" have no finite roster to tick off — any thirty will do — so they stay
 * plain counters and get a description in the tooltip instead of a list.
 *
 * <p>Entry names borrow vanilla's own translations wherever the entry is a
 * block or a mob, so a new language costs nothing here; only the entries with
 * no vanilla name of their own (armour materials, gear categories) carry a
 * key of the mod's own.
 */
public final class GateChecklists {
	/**
	 * One line of a checklist.
	 *
	 * @param bit  index into the gate's mask counter
	 * @param name what the line reads on screen
	 */
	public record Entry(int bit, Component name) {
	}

	private static final Map<String, List<Entry>> ENTRIES = new LinkedHashMap<>();

	static {
		// Pickaxe — every ore in the game, in the order BlockBreakTracker
		// assigns them. Bit 9 is the Nether's gold, which is a different block
		// from the Overworld's and counts as its own line.
		put("ore_checklist",
			block(0, Blocks.COAL_ORE),
			block(1, Blocks.COPPER_ORE),
			block(2, Blocks.IRON_ORE),
			block(3, Blocks.GOLD_ORE),
			block(4, Blocks.REDSTONE_ORE),
			block(5, Blocks.LAPIS_ORE),
			block(6, Blocks.DIAMOND_ORE),
			block(7, Blocks.EMERALD_ORE),
			block(8, Blocks.NETHER_QUARTZ_ORE),
			block(9, Blocks.NETHER_GOLD_ORE),
			block(10, Blocks.ANCIENT_DEBRIS));

		// Axe — the nine Overworld woods, read at two targets (any six for
		// tier 2, all nine for tier 4), and the Nether's two stems.
		put("overworld_wood_checklist",
			block(0, Blocks.OAK_LOG),
			block(1, Blocks.SPRUCE_LOG),
			block(2, Blocks.BIRCH_LOG),
			block(3, Blocks.JUNGLE_LOG),
			block(4, Blocks.ACACIA_LOG),
			block(5, Blocks.DARK_OAK_LOG),
			block(6, Blocks.MANGROVE_LOG),
			block(7, Blocks.CHERRY_LOG),
			block(8, Blocks.PALE_OAK_LOG));
		put("nether_wood_checklist",
			block(0, Blocks.CRIMSON_STEM),
			block(1, Blocks.WARPED_STEM));

		// Enchanter — one of every kind of gear taken off the table.
		// Ground — every plant a hoe harvests, in the order BlockBreakTracker
		// assigns them. Read at three targets: any five at tier 3, any eight at
		// tier 4, all eleven at tier 5.
		put("crop_checklist",
			block(0, Blocks.WHEAT),
			block(1, Blocks.CARROTS),
			block(2, Blocks.POTATOES),
			block(3, Blocks.BEETROOTS),
			block(4, Blocks.MELON),
			block(5, Blocks.PUMPKIN),
			block(6, Blocks.NETHER_WART),
			block(7, Blocks.COCOA),
			block(8, Blocks.SWEET_BERRY_BUSH),
			block(9, Blocks.SUGAR_CANE),
			block(10, Blocks.TORCHFLOWER_CROP));
		put("enchant_type_checklist",
			entry(0, "sword"),
			entry(1, "pickaxe"),
			entry(2, "axe"),
			entry(3, "shovel"),
			entry(4, "hoe"),
			entry(5, "armor"),
			entry(6, "bow"),
			entry(7, "book"),
			entry(8, "other"));

		// Sword — the four the game calls bosses. The dragon has a gate of its
		// own at tier 5; this is the collection.
		put("boss_checklist",
			mob(0, EntityTypes.ELDER_GUARDIAN),
			mob(1, EntityTypes.WITHER),
			mob(2, EntityTypes.WARDEN),
			mob(3, EntityTypes.ENDER_DRAGON));

		// Armor — the four pieces of the first set, and every material a full
		// set can be made of.
		put("craft_iron_armor",
			entry(0, "helmet"),
			entry(1, "chestplate"),
			entry(2, "leggings"),
			entry(3, "boots"));
		put("armor_checklist",
			entry(0, "leather"),
			entry(1, "chainmail"),
			entry(2, "iron"),
			entry(3, "gold"),
			entry(4, "diamond"),
			entry(5, "netherite"));
	}

	private GateChecklists() {
	}

	/** The lines of a checklist gate, or an empty list for a plain counter. */
	public static List<Entry> of(String gateId) {
		return ENTRIES.getOrDefault(gateId, List.of());
	}

	/** The counter a checklist gate keeps its bits in, beside the visible count. */
	public static String maskId(String gateId) {
		return gateId + "_mask";
	}

	/** Whether one line is ticked, read off a progress snapshot's counters. */
	public static boolean ticked(Map<String, Integer> counters, String gateId, int bit) {
		return (counters.getOrDefault(maskId(gateId), 0) & (1 << bit)) != 0;
	}

	/**
	 * Ticks one line of a checklist and refreshes the visible counter from the
	 * mask. Bits outside the list are dropped, so a mask saved under an older
	 * roster cannot inflate the count past the gate's target.
	 */
	public static void tick(TreeProgress progress, String gateId, int bit) {
		String maskId = maskId(gateId);
		int mask = (progress.count(maskId) | (1 << bit)) & width(gateId);
		progress.counters.put(maskId, mask);
		progress.counters.put(gateId, Integer.bitCount(mask));
	}

	/** Every bit of a checklist set — the width to clamp a saved mask to. */
	public static int width(String gateId) {
		int width = 0;
		for (Entry entry : of(gateId)) {
			width |= 1 << entry.bit();
		}
		return width;
	}

	private static void put(String gateId, Entry... entries) {
		ENTRIES.put(gateId, List.of(entries));
	}

	private static Entry block(int bit, Block block) {
		return new Entry(bit, block.getName());
	}

	private static Entry mob(int bit, EntityType<?> type) {
		return new Entry(bit, type.getDescription());
	}

	private static Entry entry(int bit, String name) {
		return new Entry(bit, Component.translatable("gate.pbenchants.entry." + name));
	}
}
