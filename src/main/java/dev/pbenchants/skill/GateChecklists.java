package dev.pbenchants.skill;

import dev.pbenchants.progress.TreeProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * <p>Only closed lists keep a bitmask. "Visit 45 biomes" or "craft 100 distinct
 * items" have no finite roster to tick off — any forty-five will do — so they
 * stay plain counters (backed by {@link TreeProgress#seen}) and get a
 * description in the tooltip instead of a list. The hostile-mob list is the
 * one in between: it is also "any thirty", but the game <em>does</em> know
 * every monster it can spawn, and a player staring at 12/30 wants to know which
 * eighteen are left. So that gate gets a <b>name roster</b> — built from the
 * entity registry on first use, ticked against the seen-set the tracker keeps —
 * and its entries ride the state packet to the client for the tooltip.
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

	/**
	 * One line of a name roster.
	 *
	 * @param id   what the tracker records, e.g. {@code "minecraft:zombie"}
	 * @param name what the line reads on screen
	 */
	public record SeenEntry(String id, Component name) {
	}

	private static final Map<String, List<Entry>> ENTRIES = new LinkedHashMap<>();

	/**
	 * The gates that are a name roster, and the kind {@link TreeProgress#see}
	 * records their entries under.
	 */
	private static final Map<String, String> SEEN_KINDS = Map.of("mob_checklist", "mob");

	/**
	 * Monsters the registry lists but survival never meets. Left off the
	 * roster so nobody goes hunting for a giant.
	 */
	private static final Set<String> UNREACHABLE_HOSTILES = Set.of("minecraft:giant", "minecraft:illusioner");

	/** Rosters built on first use — the registry is frozen by then, static init is too early. */
	private static final Map<String, List<SeenEntry>> SEEN_ROSTERS = new HashMap<>();

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
		put("sapling_checklist",
			block(0, Blocks.OAK_SAPLING),
			block(1, Blocks.SPRUCE_SAPLING),
			block(2, Blocks.BIRCH_SAPLING),
			block(3, Blocks.JUNGLE_SAPLING),
			block(4, Blocks.ACACIA_SAPLING),
			block(5, Blocks.DARK_OAK_SAPLING),
			block(6, Blocks.MANGROVE_PROPAGULE),
			block(7, Blocks.CHERRY_SAPLING),
			block(8, Blocks.PALE_OAK_SAPLING),
			block(9, Blocks.AZALEA),
			block(10, Blocks.FLOWERING_AZALEA),
			block(11, Blocks.CRIMSON_FUNGUS),
			block(12, Blocks.WARPED_FUNGUS));

		// Enchanter — one of every kind of gear taken off the table.
		// Ground — every plant a hoe harvests, in the order BlockBreakTracker
		// assigns them. Read at three targets: any five at tier 3, any eight at
		// tier 4, all twelve at tier 5. The last two are named by what you
		// harvest — the torchflower bloom and the grown pitcher plant — since
		// the seedling blocks are not the harvest.
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
			block(10, Blocks.TORCHFLOWER),
			block(11, Blocks.PITCHER_PLANT));
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

	/** The seen-kind a name-roster gate ticks against, or null for every other gate. */
	@Nullable
	public static String seenKind(String gateId) {
		return SEEN_KINDS.get(gateId);
	}

	/**
	 * The slice of a seen-set the client needs: only the kinds a roster exists
	 * for. The rest stays on the server, where the counter it feeds is enough.
	 */
	public static Set<String> synced(Set<String> seen) {
		Set<String> out = new HashSet<>();
		for (String entry : seen) {
			int slash = entry.indexOf('/');
			if (slash > 0 && SEEN_KINDS.containsValue(entry.substring(0, slash))) {
				out.add(entry);
			}
		}
		return out;
	}

	/** Whether one roster line is ticked, read off a synced seen-set. */
	public static boolean seen(Set<String> seen, String gateId, String id) {
		String kind = SEEN_KINDS.get(gateId);
		return kind != null && seen.contains(kind + "/" + id);
	}

	/**
	 * Every line a name-roster gate can tick, or an empty list for any other
	 * gate. The hostile roster is every entity type the game files under the
	 * monster category — so a mob added by a data pack or another mod lists
	 * itself — sorted by the name the player will read.
	 */
	public static List<SeenEntry> seenRoster(String gateId) {
		String kind = SEEN_KINDS.get(gateId);
		if (kind == null) {
			return List.of();
		}
		return SEEN_ROSTERS.computeIfAbsent(kind, GateChecklists::buildRoster);
	}

	private static List<SeenEntry> buildRoster(String kind) {
		if (!kind.equals("mob")) {
			return List.of();
		}
		List<SeenEntry> roster = new ArrayList<>();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (type.getCategory() != MobCategory.MONSTER) {
				continue;
			}
			String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
			if (UNREACHABLE_HOSTILES.contains(id)) {
				continue;
			}
			roster.add(new SeenEntry(id, type.getDescription()));
		}
		roster.sort(Comparator.comparing(entry -> entry.name().getString()));
		return List.copyOf(roster);
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
