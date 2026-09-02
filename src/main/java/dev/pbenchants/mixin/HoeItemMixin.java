package dev.pbenchants.mixin;

import dev.pbenchants.perk.FurrowHand;
import dev.pbenchants.track.FarmingTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public class HoeItemMixin {
	/**
	 * A till is a successful useOn that left farmland at the clicked position.
	 * A hoe also succeeds at turning coarse dirt into dirt and rooted dirt into
	 * dirt, so the result block is checked rather than the return value alone —
	 * the same filter {@link AxeItemMixin} uses against copper scraping.
	 */
	@Inject(method = "useOn", at = @At("RETURN"))
	private void pbenchants$trackTill(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction()) {
			return;
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.isClientSide() || !(context.getPlayer() instanceof ServerPlayer player)
			|| !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!level.getBlockState(pos).is(Blocks.FARMLAND)) {
			return;
		}
		FarmingTracker.onTilled(player);
		FurrowHand.tillAround(player, serverLevel, pos);
	}
}
