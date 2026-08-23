package dev.toolmastery.mixin;

import dev.toolmastery.perk.MiningSpeed;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the pickaxe speed passives (Mason's Grip, Obsidian Breaker) on top of
 * the vanilla destroy speed.
 *
 * <p>Deliberately a common mixin, not a client one: the client drives the
 * breaking animation and the server validates the break, so both sides have to
 * compute the same speed or the block "heals" mid-swing.
 */
@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void toolmastery$applySpeedPassives(BlockState state, CallbackInfoReturnable<Float> cir) {
		float speed = cir.getReturnValue();
		if (speed <= 0.0F) {
			return; // unbreakable, or the wrong tool — nothing to speed up
		}
		float multiplier = MiningSpeed.multiplier((Player) (Object) this, state);
		if (multiplier != 1.0F) {
			cir.setReturnValue(speed * multiplier);
		}
	}
}
