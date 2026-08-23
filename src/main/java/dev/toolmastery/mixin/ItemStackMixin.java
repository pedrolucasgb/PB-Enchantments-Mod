package dev.toolmastery.mixin;

import dev.toolmastery.perk.Indestructible;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * The three halves of Indestructible, all on {@link ItemStack} because that is
 * where durability and tool behaviour live:
 *
 * <ol>
 *   <li>damage is clamped one point short of breaking, so vanilla's
 *       {@code applyDamage} never sees {@code isBroken()} and never shrinks the
 *       stack away;</li>
 *   <li>a spent item digs at bare-hand speed;</li>
 *   <li>and it stops counting as the right tool, so blocks that need one drop
 *       nothing — the item is carried, not usable.</li>
 * </ol>
 *
 * <p>Both read-side hooks run on either side: the client animates the break and
 * the server validates it, so they have to agree.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {
	@ModifyVariable(
		method = "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
		at = @At("HEAD"), argsOnly = true)
	private int toolmastery$indestructibleClamp(int damage, int ignored, ServerPlayer player, Consumer<?> onBreak) {
		return Indestructible.clampDamage((ItemStack) (Object) this, damage);
	}

	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void toolmastery$spentDigsLikeAHand(BlockState state, CallbackInfoReturnable<Float> cir) {
		if (Indestructible.isSpent((ItemStack) (Object) this)) {
			cir.setReturnValue(1.0F); // the bare-hand rate
		}
	}

	@Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void toolmastery$spentHarvestsNothing(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (Indestructible.isSpent((ItemStack) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
