package dev.pbenchants.perk;

import dev.pbenchants.enchant.EnchanterPerks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Prospector's Wisdom (Enchanter tier 5) — the mining half of
 * {@code Reaper's Wisdom}: a block broken with a Fortune tool gives more
 * experience, +25% per level, so Fortune III is +75%.
 *
 * <p>Looting already decides how much of a mob you take away, and Reaper's
 * Wisdom makes it decide how much you learn too. Fortune is the same bargain on
 * the other side of the game — it decides how much of a vein you take away —
 * and this is the node that makes it pay the Enchanter as well. The two are
 * priced and worded as a pair on purpose.
 *
 * <p>The hook cannot read the player: {@code Block.popExperience} is handed a
 * level, a position and a number. So the tool and its owner are parked here for
 * the length of one {@code Block.playerDestroy} — the call that drops the block
 * and its experience together — and the scaling reads them back. Every cascade
 * in the mod breaks its extra blocks through {@code gameMode.destroyBlock},
 * which runs {@code playerDestroy} per block, so Rich Vein and Dig Range scale
 * for free.
 */
public final class ProspectorsWisdom {
	/** The break currently running on this thread, if it is a player's. */
	private record Swing(int fortuneLevel) {
	}

	private static final ThreadLocal<Swing> CURRENT = new ThreadLocal<>();

	private ProspectorsWisdom() {
	}

	/**
	 * Opens the window for one player break. Cheap on the common path: the
	 * enchantment lookup only happens for someone who owns the node.
	 */
	public static void enter(Player player, ItemStack tool) {
		if (!(player instanceof ServerPlayer serverPlayer)
			|| !EnchanterPerks.owns(serverPlayer, EnchanterPerks.PROSPECTORS_WISDOM)) {
			return;
		}
		int fortune = fortuneLevel(serverPlayer, tool);
		if (fortune > 0) {
			CURRENT.set(new Swing(fortune));
		}
	}

	/** Always in a finally: the window must not outlive the break that opened it. */
	public static void exit() {
		CURRENT.remove();
	}

	/**
	 * The experience this break should actually drop. Rounded up, so a 1-point
	 * block still benefits, and Scholar applies afterwards on the way into the
	 * bar — the two multiply, exactly as they do for Reaper's Wisdom.
	 */
	public static int scale(int amount) {
		Swing swing = CURRENT.get();
		if (swing == null || amount <= 0) {
			return amount;
		}
		return amount + Math.max(1, amount * swing.fortuneLevel() / 4);
	}

	private static int fortuneLevel(ServerPlayer player, ItemStack tool) {
		if (tool.isEmpty()) {
			return 0;
		}
		Holder<Enchantment> fortune = player.level().registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(Enchantments.FORTUNE)
			.orElse(null);
		return fortune == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(fortune, tool);
	}
}
