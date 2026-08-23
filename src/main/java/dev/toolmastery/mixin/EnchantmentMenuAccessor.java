package dev.toolmastery.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the menu internals the Rewrite Fate reroll handler needs. */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {
	@Accessor("enchantSlots")
	Container toolmastery$enchantSlots();

	@Accessor("enchantmentSeed")
	DataSlot toolmastery$enchantmentSeedSlot();
}
