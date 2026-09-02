package dev.pbenchants.perk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * The floor rule of the Ground tree: nothing this class breaks automatically is
 * ever below the block the player is standing on.
 *
 * <p>It is the reason Flat Earth and Diggy Diggy Hole can be as wide as they are.
 * The class that moves the most earth per second is also the one that cannot drop
 * you into a cave you did not see — you flatten one layer, step down, flatten the
 * next.
 *
 * <p>The threshold is {@link Player#getOnPos()}, not {@code blockPosition()}.
 * {@code blockPosition()} is the block the <em>feet occupy</em>, i.e. floor + 1;
 * using it would put the whole aimed plane one below the threshold every time you
 * dig the floor under yourself, and the perk would do nothing in its headline
 * case. {@code getOnPos()} is the supporting block, and it also gets slabs,
 * stairs, farmland and standing on a block edge right.
 */
public final class GroundLevel {
	private GroundLevel() {
	}

	/** Y of the block the player is standing on. */
	public static int floorY(Player player) {
		return player.getOnPos().getY();
	}

	/** The absolute rule. */
	public static boolean allowed(Player player, BlockPos target) {
		return target.getY() >= floorY(player);
	}
}
