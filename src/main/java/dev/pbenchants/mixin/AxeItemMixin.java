package dev.pbenchants.mixin;

import java.util.Map;

import dev.pbenchants.track.BlockBreakTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AxeItem.class)
public class AxeItemMixin {
	@Shadow
	@Final
	protected static Map<Block, Block> STRIPPABLES;

	/**
	 * A strip is a successful useOn that left a stripped variant at the clicked
	 * position. Scraping copper (wax/oxidation) also returns success, but its
	 * result block is never in STRIPPABLES' values, so it is filtered out here.
	 */
	@Inject(method = "useOn", at = @At("RETURN"))
	private void pbenchants$trackStrip(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction()) {
			return;
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (STRIPPABLES.containsValue(level.getBlockState(pos).getBlock())) {
			BlockBreakTracker.onStrip(level, context.getPlayer(), pos);
		}
	}
}
