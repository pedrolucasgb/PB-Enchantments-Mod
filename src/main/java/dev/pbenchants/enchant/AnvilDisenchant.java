package dev.pbenchants.enchant;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

/**
 * The anvil's disenchant mode (0.10.0).
 *
 * <p>The anvil always enchants — that stays the default and nothing about it
 * changes. But with a book in the sacrifice slot that carries <em>exactly</em>
 * one enchantment, at <em>exactly</em> the rank the tool in the base slot
 * carries, a second reading of the same two items becomes possible: strike the
 * enchantment OFF the tool instead of merging it on. Which of the two happens
 * is a toggle on the screen; the toggle only shows itself when this match
 * exists, so nobody meets a mode they cannot use.
 *
 * <p>The price of a removal is the book: it is consumed exactly as it would be
 * in a merge, plus a levels bill equal to the rank removed. The enchantment is
 * not refunded onto anything — the book bought the removal, not a transfer.
 * Repair-cost bookkeeping is deliberately left alone.
 *
 * <p>The mode itself lives on the menu instance (see {@link Mode}, implemented
 * by the anvil mixin), so every fresh anvil screen opens in enchant mode.
 */
public final class AnvilDisenchant {
	/** Implemented on {@code AnvilMenu} by the mixin; the payload receiver flips it. */
	public interface Mode {
		boolean pbenchants$disenchanting();

		void pbenchants$setDisenchanting(boolean value);
	}

	private AnvilDisenchant() {
	}

	/**
	 * The one enchantment the book offers that the input carries at the very
	 * same rank — or null, which means the disenchant reading does not exist
	 * for these two items. A book with two enchantments never matches: "the
	 * exact enchantment to remove" is the contract, and a bundle is not exact.
	 */
	@Nullable
	public static Holder<Enchantment> match(ItemStack input, ItemStack book) {
		if (input.isEmpty() || !book.is(Items.ENCHANTED_BOOK)) {
			return null;
		}
		ItemEnchantments offered = EnchantmentHelper.getEnchantmentsForCrafting(book);
		if (offered.keySet().size() != 1) {
			return null;
		}
		Holder<Enchantment> enchantment = offered.keySet().iterator().next();
		int level = offered.getLevel(enchantment);
		if (level <= 0) {
			return null;
		}
		ItemEnchantments carried = EnchantmentHelper.getEnchantmentsForCrafting(input);
		return carried.getLevel(enchantment) == level ? enchantment : null;
	}

	/** The input with that one enchantment struck off — everything else untouched. */
	public static ItemStack strip(ItemStack input, Holder<Enchantment> enchantment) {
		ItemStack result = input.copy();
		EnchantmentHelper.updateEnchantments(result, mutable ->
			mutable.removeIf(present -> present.equals(enchantment)));
		return result;
	}
}
