package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Slipstream — the same rocket, more distance.
 *
 * <p>Of the two readings the design left open, this is the momentum one rather
 * than the refund one: a boost that keeps pushing past the point where it would
 * normally have died, so 10 / 25 / 50% of the rocket's push carries over
 * instead of decaying. It is implemented by lengthening the rocket's own life
 * rather than by re-applying a decaying slice of its velocity vector, which
 * means the acceleration curve, the collision handling and the client-side
 * prediction all stay vanilla's. The extra distance is real; the physics is
 * not reimplemented.
 *
 * <p>(The refund reading — a chance the rocket is not consumed — is the
 * Endless Horizon capstone, so both ideas ship, priced apart.)
 *
 * <p>The level is read off the Elytra in the chest slot through
 * {@link ItemAuthority#effectiveLevel}, so a borrowed Slipstream III wing works
 * at whatever rank its wearer has actually unlocked.
 */
public final class Slipstream {
	/** Extra life as a fraction of the rocket's own, per rank. */
	private static final double[] CARRYOVER = {0.0, 0.10, 0.25, 0.50};

	/** Endless Horizon doubles the carryover, and refunds this share of rockets. */
	private static final double CAPSTONE_REFUND_CHANCE = 0.25;

	private Slipstream() {
	}

	/** Effective Slipstream rank of the wings this player is wearing, 0 when none. */
	public static int rank(Player player) {
		ItemStack wings = player.getItemBySlot(EquipmentSlot.CHEST);
		return ItemAuthority.effectiveLevel(player, wings, ModEnchantments.SLIPSTREAM);
	}

	/**
	 * Extra ticks to add to a firework's lifetime for the player it is pushing.
	 * Zero for anyone without the enchantment, which is every rocket fired at
	 * the sky.
	 */
	public static int bonusLifetime(Player player, int baseLifetime) {
		int rank = rank(player);
		if (rank <= 0) {
			return 0;
		}
		double carryover = CARRYOVER[Math.min(rank, CARRYOVER.length - 1)];
		if (ExplorerPerks.owns(player, ExplorerPerks.ENDLESS_HORIZON)) {
			carryover *= 2.0;
		}
		return (int) Math.round(baseLifetime * carryover);
	}

	/** Endless Horizon: does this launch give the rocket back? */
	public static boolean refunds(Player player) {
		return ExplorerPerks.owns(player, ExplorerPerks.ENDLESS_HORIZON)
			&& player.getRandom().nextDouble() < CAPSTONE_REFUND_CHANCE;
	}
}
