package dev.pbenchants.track;

import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/**
 * Feeds the Explorer's distance gates off the vanilla movement statistics.
 *
 * <p>Vanilla already counts every centimetre a player walks, sprints, swims,
 * rows, flies and rides, per player and persisted with the save — so this does
 * not integrate positions of its own. Once a second it reads those totals,
 * subtracts the baseline it took the first time it ever saw this player, and
 * writes the difference in whole blocks into the gate counters. Cheap, and it
 * survives teleports, dimension changes and deaths for free.
 *
 * <p>The baseline is what makes the class start at zero for a world that
 * already has a hundred kilometres on the clock: installing the mod does not
 * hand you tier 4.
 */
public final class MovementTracker {
	/** Centimetres per block, the unit vanilla stores movement in. */
	private static final int CM_PER_BLOCK = 100;

	/** Gate counters summed into {@code travel_total}. */
	private static final String[] DISTANCE_LINES = {
		"walk_blocks", "sprint_blocks", "swim_blocks", "boat_blocks", "elytra_blocks", "mount_blocks"
	};

	private MovementTracker() {
	}

	/** Called once a second per online player. */
	public static void tick(ServerPlayer player) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.EXPLORER);

		line(progress, player, "walk_blocks", Stats.WALK_ONE_CM);
		line(progress, player, "sprint_blocks", Stats.SPRINT_ONE_CM);
		line(progress, player, "swim_blocks", Stats.SWIM_ONE_CM, Stats.WALK_UNDER_WATER_ONE_CM);
		line(progress, player, "boat_blocks", Stats.BOAT_ONE_CM);
		line(progress, player, "elytra_blocks", Stats.AVIATE_ONE_CM);
		line(progress, player, "mount_blocks", Stats.HORSE_ONE_CM, Stats.MINECART_ONE_CM,
			Stats.STRIDER_ONE_CM, Stats.PIG_ONE_CM, Stats.HAPPY_GHAST_ONE_CM);

		int total = 0;
		for (String counterId : DISTANCE_LINES) {
			total += progress.count(counterId);
		}
		progress.counters.put("travel_total", total);
	}

	/**
	 * One distance line: the sum of some vanilla statistics, in blocks, counted
	 * from the first time this player was seen.
	 *
	 * <p>A total that has gone <em>down</em> means the statistics were reset
	 * under us (a {@code /stats} wipe, a restored backup). Rebaselining there
	 * freezes the counter rather than letting it go negative.
	 */
	private static void line(TreeProgress progress, ServerPlayer player, String counterId, Identifier... stats) {
		int centimetres = 0;
		for (Identifier stat : stats) {
			centimetres += player.getStats().getValue(Stats.CUSTOM, stat);
		}
		String baselineId = counterId + "_base_cm";
		Integer baseline = progress.counters.get(baselineId);
		if (baseline == null || centimetres < baseline) {
			baseline = centimetres;
			progress.counters.put(baselineId, baseline);
		}
		progress.counters.put(counterId, (centimetres - baseline) / CM_PER_BLOCK);
	}
}
