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
 * <p><b>The whole layer, not just the one tile.</b> The floor the player stands on
 * is protected as a plane: nothing at or below {@code support.getY()} is ever
 * taken. Sparing only the single supporting block was not enough — it left the
 * player on a pillar in the middle of the trench they had just cut around their
 * own feet, which is the same accident by a different route.
 *
 * <p>That makes both perks work <em>downwards from where you stand</em> rather
 * than through it, which is what their names always described: Flat Earth levels
 * a hill by taking everything above the plane you are standing in, and the aura
 * hollows out a room around you. To take the layer you are on, step down onto the
 * next one — vanilla still breaks whatever you actually aim at, so getting there
 * costs one swing.
 *
 * <p>The reference is {@link Player#getOnPos()}, the supporting block, so slabs,
 * stairs, farmland and standing on a block edge all read correctly.
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
		return target.getY() > support.getY();
	}
}
