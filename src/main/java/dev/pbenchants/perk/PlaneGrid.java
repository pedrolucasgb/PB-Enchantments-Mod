package dev.pbenchants.perk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * The 3x3 plane around a block, in a stable (horizontal, vertical) basis.
 *
 * <p>Same gesture Dig Range uses — a steep pitch means you are working a floor,
 * so the plane is horizontal; otherwise it is the wall you are facing — but with
 * the two axes named instead of implied. {@code AreaBreak.targets} folds the
 * basis into the offset call and quietly swaps which of its two loop variables
 * is Y between the along-X and along-Z cases. That is invisible for the
 * symmetric cross and 3x3 masks it uses, and wrong for any asymmetric mask, such
 * as Flat Earth II's "the three below".
 */
public record PlaneGrid(Direction hAxis, Direction vAxis) {
	/**
	 * Looking mostly up or down gives the horizontal plane, with "up" on the
	 * grid meaning away from the player; otherwise the vertical plane facing
	 * them, with "up" meaning up.
	 */
	public static PlaneGrid facing(Player player) {
		Direction facing = player.getDirection(); // always horizontal
		if (Math.abs(player.getXRot()) > 45.0F) {
			return new PlaneGrid(facing.getClockWise(), facing);
		}
		return new PlaneGrid(facing.getClockWise(), Direction.UP);
	}

	public BlockPos at(BlockPos origin, int h, int v) {
		return origin.relative(hAxis, h).relative(vAxis, v);
	}
}
