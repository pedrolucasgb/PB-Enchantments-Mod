package dev.toolmastery.mixin;

import dev.toolmastery.track.FarmingTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
	/**
	 * Identified via the placed block's item form (26.x dropped
	 * BlockTags.SAPLINGS; only the item tag remains). The held stack can't be
	 * used — placing the last sapling leaves it empty, reporting AIR.
	 */
	@Inject(method = "place", at = @At("RETURN"))
	private void toolmastery$trackSaplingPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction()) {
			return;
		}
		Level level = context.getLevel();
		if (level.isClientSide() || !(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
			return;
		}
		ItemStack placed = new ItemStack(level.getBlockState(context.getClickedPos()).getBlock());
		if (placed.is(ItemTags.SAPLINGS)) {
			FarmingTracker.onSaplingPlaced(serverPlayer);
		}
	}
}
