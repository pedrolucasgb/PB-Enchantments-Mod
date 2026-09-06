package dev.pbenchants.perk;

import dev.pbenchants.mixin.ExperienceOrbAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Experience in fewer, larger orbs.
 *
 * <p>Vanilla breaks every award into orbs off a fixed ladder (1, 3, 7, 17, 37,
 * 73 ...), so a 5-point zombie is three orbs and a 100-point blaze farm kill
 * is four; a player picks up <em>one orb per two ticks</em>, and an orb that
 * merged with its neighbours still costs one pickup per orb it swallowed. At
 * the top of the tree — Rich Vein opening a whole vein, Fortune IV on top,
 * Prospector's Wisdom doubling the experience — that is a cloud the player
 * stands in for a minute, and a level-30 player who does not need the
 * experience at all still has to wait for it.
 *
 * <p>Two changes, both on the server's side only, both invisible to the total:
 * an award is <b>one orb carrying the whole amount</b>, and an award that
 * lands within {@value #MERGE_RADIUS} blocks of an orb this class made earlier
 * is <b>folded into that orb</b> rather than spawning beside it. A vein of
 * twelve ore blocks becomes one or two orbs; a mob farm's drop pile stays a
 * handful of orbs instead of a hundred. Mending sees the same points, because
 * an orb repairs as far as its value goes and then keeps repairing the next
 * piece with what is left.
 *
 * <p>Only orbs that have never merged the vanilla way ({@code count == 1})
 * are folded into, so an orb another mod stacked stays exactly as that mod
 * left it; and an orb is never grown past {@value #MAX_ORB_VALUE}, the point
 * where the save format would overflow it.
 */
public final class XpOrbs {
	/** How far an award looks for an orb to join. Vanilla merges at half a block. */
	private static final double MERGE_RADIUS = 1.5;

	/** Saved as a short: leave headroom rather than wrap. */
	private static final int MAX_ORB_VALUE = 30_000;

	private XpOrbs() {
	}

	/** Replaces {@code ExperienceOrb.awardWithDirection}; the mixin has already refused amounts of zero. */
	public static void award(ServerLevel level, Vec3 pos, Vec3 direction, int amount) {
		int remaining = amount;
		AABB box = AABB.ofSize(pos, MERGE_RADIUS * 2, MERGE_RADIUS * 2, MERGE_RADIUS * 2);
		List<ExperienceOrb> nearby = level.getEntitiesOfClass(ExperienceOrb.class, box,
			orb -> !orb.isRemoved() && ((ExperienceOrbAccessor) orb).pbenchants$count() == 1
				&& orb.getValue() < MAX_ORB_VALUE);
		for (ExperienceOrb orb : nearby) {
			int room = MAX_ORB_VALUE - orb.getValue();
			int folded = Math.min(room, remaining);
			ExperienceOrbAccessor access = (ExperienceOrbAccessor) orb;
			access.pbenchants$setValue(orb.getValue() + folded);
			access.pbenchants$setAge(0);
			remaining -= folded;
			if (remaining <= 0) {
				return;
			}
		}
		while (remaining > 0) {
			int value = Math.min(remaining, MAX_ORB_VALUE);
			level.addFreshEntity(new ExperienceOrb(level, pos, direction, value));
			remaining -= value;
		}
	}
}
