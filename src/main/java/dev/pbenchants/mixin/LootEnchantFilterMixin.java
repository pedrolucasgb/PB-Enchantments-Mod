package dev.pbenchants.mixin;

import dev.pbenchants.skill.TreeSwitch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Loot never hands out an enchantment from a switched-off tree.
 *
 * <p>Both loot functions that enchant at random end here — chests, fishing,
 * mob equipment rolled through {@code enchant_with_levels}, and the like. The
 * finished stack is cleaned rather than the pool filtered, so the two
 * functions share one rule and vanilla's own selection is not touched. A book
 * left with no enchantment at all goes back to being a plain book.
 *
 * <p>On the 1.21.1 build this mostly guards config-disabled trees: the
 * always-off Bow/Armor/Sword enchantments are already out of every loot tag.
 */
@Mixin({EnchantRandomlyFunction.class, EnchantWithLevelsFunction.class})
public abstract class LootEnchantFilterMixin {
	@Inject(method = "run", at = @At("RETURN"), cancellable = true)
	private void pbenchants$dropDisabledTrees(ItemStack input, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack stack = cir.getReturnValue();
		if (stack == null || stack.isEmpty() || TreeSwitch.disabledIds().isEmpty()) {
			return;
		}
		boolean[] changed = {false};
		EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(holder -> {
			boolean drop = !TreeSwitch.enchantmentAllowed(holder);
			changed[0] |= drop;
			return drop;
		}));
		if (changed[0] && stack.is(Items.ENCHANTED_BOOK)
			&& EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty()) {
			cir.setReturnValue(new ItemStack(Items.BOOK, stack.getCount()));
		}
	}
}
