package dev.pbenchants.perk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * The floor rule of the Ground tree: nothing this class breaks automatically
 * takes the block holding the player up, or anything below it.
 *
 * <p>It is the reason Flat Earth and Diggy Diggy Hole can be as wide as they are.
 * The class that moves the most earth per second is also the one that cannot drop
 * you into a cave you did not see — you clear the layer around your feet, step
 * down, and clear the next.
 *
 * <p>The rule has two halves and both matter:
 * <ul>
 *   <li><b>Nothing below the floor.</b> The threshold is {@link Player#getOnPos()},
 *       not {@code blockPosition()}. {@code blockPosition()} is the block the
 *       <em>feet occupy</em>, i.e. floor + 1; using it would put the whole aimed
 *       plane one below the threshold every time you dig the floor under
 *       yourself, and the perk would do nothing in its headline case.
 *       {@code getOnPos()} is the supporting block, and it also gets slabs,
 *       stairs, farmland and standing on a block edge right.
 *   <li><b>Not the supporting block itself.</b> The floor test alone is
 *       inclusive, so the tile directly under the player passed it and every
 *       swing dropped them a block. Everything else in that layer is still fair
 *       game — you clear the ring around your feet and keep standing on the one
 *       tile that holds you.
 * </ul>
 *
 * <p>Callers take the support position once and pass it in rather than asking per
 * block: the aura tests a whole cube of candidates every tick, and that is not
 * the place to re-derive the same answer.
 */
public final class GroundLevel {
	private GroundLevel() {
	}

	/** The block the player is standing on. */
	public static BlockPos support(Player player) {
		return player.getOnPos();
	}

	/** The absolute rule, against a support position taken once by the caller. */
	public static boolean allowed(BlockPos support, BlockPos target) {
		return target.getY() >= support.getY() && !target.equals(support);
	}
}
