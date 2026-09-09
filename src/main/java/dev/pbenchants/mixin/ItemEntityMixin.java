package dev.pbenchants.mixin;

import dev.pbenchants.perk.Indestructible;
import dev.pbenchants.track.FarmingTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Counts apple pickups by comparing the entity's stack before and after
 * vanilla pickup logic — the stack is mutated in place (and reports AIR once
 * emptied), so both the item and the count must be captured up front.
 *
 * <p>Also home to Indestructible's second life (0.10.0): an item that cannot
 * break in your hand should not break on the floor either. Lava, fire,
 * explosions, cactus — every damage source an {@link ItemEntity} can take is
 * refused while the stack carries the enchantment. Only two ends remain: the
 * despawn clock, deliberately untouched, and {@code /kill}-class sources that
 * bypass invulnerability, so an admin can still clean up.
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
		// A full pickup discards the entity and then RESTORES the stack's count
		// (vanilla keeps the stack alive for the pickup animation), so the
		// before/after difference reads zero exactly when everything was taken.
		int taken = self.isRemoved()
			? pbenchants$preTouchCount
			: pbenchants$preTouchCount - self.getItem().getCount();
		if (taken > 0 && pbenchants$preTouchItem == Items.APPLE) {
			FarmingTracker.onApplePickup(serverPlayer, taken);
		}
		// Beacon: the wither skeleton skull, picked up by hand.
		dev.pbenchants.track.BeaconTracker.onPickup(serverPlayer, pbenchants$preTouchItem, taken);
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void pbenchants$indestructibleFloorLife(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (Indestructible.has(self.getItem()) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			cir.setReturnValue(false);
		}
	}

	/** No fire ticks either: an Indestructible drop in lava should not even look like it is burning. */
	@Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
	private void pbenchants$indestructibleFireImmune(CallbackInfoReturnable<Boolean> cir) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (!cir.getReturnValueZ() && Indestructible.has(self.getItem())) {
			cir.setReturnValue(true);
		}
	}
}
