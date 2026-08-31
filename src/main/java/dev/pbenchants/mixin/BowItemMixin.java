package dev.pbenchants.mixin;

import dev.pbenchants.perk.BowPerks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The bow itself: how fast the draw fills, how straight a braced shot flies,
 * and the over-draw the Storm of Arrows capstone banks.
 *
 * <p>All three hooks run on both sides where vanilla does — the release is
 * simulated on the client too, and the two have to agree on the power the
 * arrow left with or the client draws an arc the server never flew. Ownership
 * answers the same on both sides through the synced snapshot.
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin {
	/**
	 * Fletcher's Hands: the draw counts 20/40/60% more ticks than really
	 * passed, so a full-power arrow leaves the string in 17/13/10 ticks
	 * instead of 20. The power curve itself stays vanilla.
	 */
	@Redirect(method = "releaseUsing", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
	private float pbenchants$fletchersHands(int ticks, ItemStack stack, Level level, LivingEntity shooter,
	                                         int timeLeft) {
		return BowItem.getPowerForTime(BowPerks.scaledDrawTicks(shooter, ticks));
	}

	/** Steady Aim: drawing while sneaking removes the bow's natural spread. */
	@ModifyArg(method = "shootProjectile", index = 5, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
	private float pbenchants$steadyAim(Entity shooter, float xRot, float yRot, float zRot,
	                                    float velocity, float inaccuracy) {
		return shooter instanceof LivingEntity living
			? BowPerks.steadyInaccuracy(living, inaccuracy)
			: inaccuracy;
	}

	/**
	 * Storm of Arrows: after the normal shot has left, the over-draw pays out.
	 * Server-side only — the extra arrows are real entities with real ammo
	 * behind them, and the client learns about them the way it learns about
	 * any arrow.
	 */
	@Inject(method = "releaseUsing", at = @At("RETURN"))
	private void pbenchants$stormOfArrows(ItemStack stack, Level level, LivingEntity shooter, int timeLeft,
	                                       CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() || !(level instanceof ServerLevel serverLevel)
			|| !(shooter instanceof ServerPlayer player)) {
			return;
		}
		int used = ((BowItem) (Object) this).getUseDuration(stack, shooter) - timeLeft;
		BowPerks.stormOfArrows(serverLevel, player, stack, BowPerks.scaledDrawTicks(player, used));
	}
}
