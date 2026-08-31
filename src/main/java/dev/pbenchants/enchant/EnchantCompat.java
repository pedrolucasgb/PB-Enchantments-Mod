package dev.pbenchants.enchant;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

/**
 * "Can this go on that?" for the skill screen's Enchant action.
 *
 * <p>The server is the authority, but the client runs exactly the same check to
 * grey out the button and explain why before a single level is spent — every
 * input it needs (the held stack, the enchantment registry, the exclusive sets)
 * is present on both sides.
 *
 * <p>Returns the reason the enchant cannot happen, or null when it can.
 */
public final class EnchantCompat {
	private EnchantCompat() {
	}

	@Nullable
	public static Component problem(ItemStack stack, Holder<Enchantment> enchantment, int level) {
		Component name = Enchantment.getFullname(enchantment, level);
		if (stack.isEmpty()) {
			return Component.translatable("enchant.pbenchants.fail.empty_hand", name);
		}
		if (!enchantment.value().canEnchant(stack)) {
			return Component.translatable("enchant.pbenchants.fail.unsupported", stack.getHoverName(), name);
		}
		if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) >= level) {
			return Component.translatable("enchant.pbenchants.fail.already", stack.getHoverName(), name);
		}
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
			Holder<Enchantment> present = entry.getKey();
			if (present.value() == enchantment.value()) {
				continue; // upgrading our own level, handled above
			}
			if (!Enchantment.areCompatible(enchantment, present)) {
				return Component.translatable("enchant.pbenchants.fail.conflict",
					name, Enchantment.getFullname(present, entry.getIntValue()));
			}
		}
		return null;
	}
}
