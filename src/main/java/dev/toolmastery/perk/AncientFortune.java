package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Ancient Fortune — the Pickaxe capstone: every block you break is rolled as if
 * your Fortune were one level higher. A bare pickaxe mines at Fortune I, a
 * Fortune III pickaxe at Fortune IV.
 *
 * <p>Vanilla has no "add a fortune level" enchantment effect — Fortune lives
 * entirely inside loot tables, which read the level off the tool in the loot
 * context. So instead of rewriting drops afterwards (which would have to
 * re-derive every block's own drop curve), we hand the loot table a boosted
 * <em>copy</em> of the tool and let vanilla roll it. Exact by construction, and
 * it composes with everything else: Smelt still smelts the extra ore, the
 * magnets still pocket it.
 *
 * <p>The copy never touches the player's real item.
 */
public final class AncientFortune {
	private AncientFortune() {
	}

	/**
	 * The tool the loot table should see for this break: a Fortune-boosted copy
	 * when Ancient Fortune applies, otherwise the tool untouched.
	 */
	public static ItemInstance boosted(ServerLevel level, ItemInstance tool) {
		if (!(tool instanceof ItemStack stack) || stack.isEmpty()) {
			return tool;
		}
		boolean ancient = false;
		for (Holder<Enchantment> present : stack.getEnchantments().keySet()) {
			if (present.is(Enchantments.SILK_TOUCH)) {
				// Silk Touch wins the drop anyway; the exclusive set keeps the two
				// apart in normal play, and a /enchant-forced pair stays sane here.
				return tool;
			}
			ancient |= present.is(ModEnchantments.ANCIENT_FORTUNE);
		}
		if (!ancient) {
			return tool;
		}

		Holder<Enchantment> fortune = level.registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(Enchantments.FORTUNE)
			.orElse(null);
		if (fortune == null) {
			return tool;
		}
		int boosted = EnchantmentHelper.getItemEnchantmentLevel(fortune, stack) + 1;
		ItemStack copy = stack.copy();
		EnchantmentHelper.updateEnchantments(copy, mutable -> mutable.set(fortune, boosted));
		return copy;
	}
}
