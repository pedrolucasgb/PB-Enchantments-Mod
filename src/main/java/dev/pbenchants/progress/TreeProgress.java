package dev.pbenchants.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A player's progress inside one skill tree. Mutable; owned by {@link PlayerProgress}. */
public final class TreeProgress {
	public static final Codec<TreeProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("unlocked_tiers").forGetter(t -> t.unlockedTiers),
		Codec.STRING.listOf().fieldOf("purchased").forGetter(t -> List.copyOf(t.purchased)),
		Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("counters").forGetter(t -> Map.copyOf(t.counters)),
		Codec.STRING.listOf().optionalFieldOf("seen", List.of()).forGetter(t -> List.copyOf(t.seen)),
		Codec.LONG.optionalFieldOf("locked_slots", 0L).forGetter(t -> t.lockedSlots)
	).apply(instance, TreeProgress::new));

	/**
	 * Node ids that were renamed after saves already existed, remapped on load
	 * so nobody loses a node to a rename. Melt became Smelt in 0.2.0.
	 */
	private static final Map<String, String> RENAMED = Map.of(
		"melt_1", "smelt_1",
		"melt_2", "smelt_2",
		"melt_3", "smelt_3"
	);

	private static final String WOOD_MASK = "overworld_wood_checklist_mask";
	private static final String RARE_MASK = "rare_wood_checklist_mask";
	private static final int OVERWORLD_WOOD_BITS = 0b1_1111_1111;

	/** Conversion rate for the pre-0.7.1 "levels spent" counter — see {@link #convertSpentLevels}. */
	private static final int POINTS_PER_SPENT_LEVEL = 100;

	/** How many tiers are unlocked (0 = none; 1 = tier 1 open, ...). */
	public int unlockedTiers;
	public final Set<String> purchased = new HashSet<>();
	public final Map<String, Integer> counters = new HashMap<>();

	/**
	 * Distinct things this player has been to, as {@code "<kind>/<id>"} strings
	 * — {@code "biome/minecraft:plains"}, {@code "struct/minecraft:village_plains"}.
	 *
	 * <p>The bitmask trick the ore and wood checklists use needs a fixed ordered
	 * list that fits in one {@code int}; the Overworld alone has more biomes than
	 * that, and the structure list grows with every data pack. So the wide
	 * checklists keep names instead, and {@link #recountSeen} keeps the visible
	 * gate counter in step with the set.
	 */
	public final Set<String> seen = new HashSet<>();

	/**
	 * Inventory slots the player pinned: sorting, auto-refill and Quick Stack
	 * step around them. One bit per slot, which covers the 41 an inventory has
	 * with room to spare. Player-wide rather than per-tree; it lives on the
	 * Artisan tree because that is the class that grants the ability to set it.
	 */
	public long lockedSlots;

	public TreeProgress() {
	}

	private TreeProgress(int unlockedTiers, List<String> purchased, Map<String, Integer> counters,
			List<String> seen, long lockedSlots) {
		this.unlockedTiers = unlockedTiers;
		for (String nodeId : purchased) {
			this.purchased.add(RENAMED.getOrDefault(nodeId, nodeId));
		}
		this.counters.putAll(counters);
		this.seen.addAll(seen);
		this.lockedSlots = lockedSlots;
		mergeWoodChecklist();
		convertSpentLevels();
	}

	/**
	 * A development build briefly cut the Overworld wood checklist in two — six
	 * common woods, three biome-hunt ones. It is one nine-bit line again, read at
	 * two targets, so any save written under the split has its second mask folded
	 * back into the first and the popcount recomputed. A save that never saw the
	 * split has no second mask and falls straight through.
	 */
	private void mergeWoodChecklist() {
		Integer rare = counters.remove(RARE_MASK);
		counters.remove("rare_wood_checklist");
		if (rare == null) {
			return;
		}
		int merged = (counters.getOrDefault(WOOD_MASK, 0) | (rare << 6)) & OVERWORLD_WOOD_BITS;
		counters.put(WOOD_MASK, merged);
		counters.put("overworld_wood_checklist", Integer.bitCount(merged));
	}

	/**
	 * The Enchanter's spending gate counted XP <em>levels</em> until 0.7.1 and
	 * counts XP <em>points</em> now — a level is not a fixed price, so the old
	 * number asked a different question of every player. There is no exact
	 * conversion (the answer depends on which levels were spent), so a saved
	 * total is carried over at {@value #POINTS_PER_SPENT_LEVEL} points a level,
	 * roughly what one costs in the band players enchant in. Nobody's grind is
	 * reset to zero, and the estimate errs in the player's favour.
	 */
	private void convertSpentLevels() {
		Integer levels = counters.remove("spend_levels");
		if (levels != null && levels > 0) {
			counters.merge("spend_points", levels * POINTS_PER_SPENT_LEVEL, Integer::sum);
		}
	}

	public int count(String counterId) {
		return counters.getOrDefault(counterId, 0);
	}

	public void addCount(String counterId, int amount) {
		counters.merge(counterId, amount, Integer::sum);
	}

	public boolean owns(String nodeId) {
		return purchased.contains(nodeId);
	}

	/**
	 * Records one entry of a name-based checklist and refreshes its counter.
	 * Returns true when this is the first time — the caller uses that to decide
	 * whether the player is told about it.
	 *
	 * @param kind      checklist prefix, e.g. {@code "biome"}
	 * @param id        the thing seen, e.g. {@code "minecraft:plains"}
	 * @param counterId gate counter that holds how many are on the list
	 */
	public boolean see(String kind, String id, String counterId) {
		if (!seen.add(kind + "/" + id)) {
			return false;
		}
		recountSeen(kind, counterId);
		return true;
	}

	/** Recomputes a name-based checklist counter from the set behind it. */
	public void recountSeen(String kind, String counterId) {
		String prefix = kind + "/";
		int total = 0;
		for (String entry : seen) {
			if (entry.startsWith(prefix)) {
				total++;
			}
		}
		counters.put(counterId, total);
	}

	public boolean slotLocked(int slot) {
		return slot >= 0 && slot < Long.SIZE && (lockedSlots & (1L << slot)) != 0L;
	}

	public void setSlotLocked(int slot, boolean locked) {
		if (slot < 0 || slot >= Long.SIZE) {
			return;
		}
		lockedSlots = locked ? lockedSlots | (1L << slot) : lockedSlots & ~(1L << slot);
	}
}
