package dev.pbenchants.mixin;

import dev.pbenchants.track.FarmingTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counts apple pickups by comparing the entity's stack before and after
 * vanilla pickup logic — the stack is mutated in place (and reports AIR once
 * emptied), so both the item and the count must be captured up front.
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {
	@Unique
	private Item pbenchants$preTouchItem;
	@Unique
	private int pbenchants$preTouchCount;

	@Inject(method = "playerTouch", at = @At("HEAD"))
	private void pbenchants$capturePreTouch(Player player, CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		pbenchants$preTouchItem = self.getItem().getItem();
		pbenchants$preTouchCount = self.getItem().getCount();
	}

	@Inject(method = "playerTouch", at = @At("RETURN"))
	private void pbenchants$trackPickup(Player player, CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (self.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		int taken = pbenchants$preTouchCount - self.getItem().getCount();
		if (taken > 0 && pbenchants$preTouchItem == Items.APPLE) {
			FarmingTracker.onApplePickup(serverPlayer, taken);
		}
	}
}
