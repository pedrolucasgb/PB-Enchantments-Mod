package dev.pbenchants.mixin;

import dev.pbenchants.perk.BeamReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The one beacon mixin. {@code applyEffects} is the method that runs every 80
 * ticks, collects the players in range and hands them the pyramid's powers;
 * it is replaced wholesale by {@link BeamReceiver#apply}, which does the same
 * thing per player. Everything the Beacon tree changes about what a player
 * receives, and everything it counts about time in a beam, hangs off this
 * one point so that nothing can disagree with anything.
 *
 * <p>The block's own state — level, powers, beam — is never written here.
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
	@Inject(method = "applyEffects", at = @At("HEAD"), cancellable = true)
	private static void pbenchants$applyPerPlayer(Level level, BlockPos pos, int levels,
			Holder<MobEffect> primary, Holder<MobEffect> secondary, CallbackInfo ci) {
		if (!(level instanceof ServerLevel serverLevel) || primary == null) {
			return;
		}
		ci.cancel();
		BeamReceiver.apply(serverLevel, pos, levels, primary, secondary);
	}
}
