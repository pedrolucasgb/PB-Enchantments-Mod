package dev.toolmastery.mixin;

import dev.toolmastery.enchant.ModEnchantments;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/**
	 * Logic level 1 trades chop speed for the instant fell: breaking a log
	 * with a Logic I axe is noticeably slower. Levels 2+ chop at normal
	 * speed. Runs on both sides so the client's break progress matches the
	 * server. Sneaking (which disables timber) also disables the slowdown.
	 */
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void toolmastery$logicLevel1Slowdown(BlockState state, CallbackInfoReturnable<Float> cir) {
		Player self = (Player) (Object) this;
		if (!state.is(BlockTags.LOGS) || self.isShiftKeyDown()) {
			return;
		}
		ItemStack held = self.getMainHandItem();
		if (!held.is(ItemTags.AXES)) {
			return;
		}
		if (ModEnchantments.level(self, held, ModEnchantments.LOGIC) == 1) {
			cir.setReturnValue(cir.getReturnValueF() * 0.3f);
		}
	}
}
