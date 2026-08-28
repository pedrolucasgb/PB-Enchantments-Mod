package dev.toolmastery.mixin;

import dev.toolmastery.perk.ArmorPerks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sure Footing, the ground half: soul sand and honey stop slowing you down.
 *
 * <p>{@code getBlockSpeedFactor} is the single place vanilla asks how much the
 * block underfoot costs you, so returning 1 there covers every block that has
 * ever slowed anyone — including the ones a future version adds — rather than
 * naming soul sand and honey and going stale. The other half of the node, the
 * half-strength Depth Strider and Soul Speed, is two attribute modifiers in
 * {@code ArmorUpkeep}.
 *
 * <p>Runs on both sides: the client predicts movement and the server checks it,
 * so a one-sided answer here would read as rubber-banding.
 */
@Mixin(Entity.class)
public class EntityMixin {
	@Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
	private void toolmastery$sureFooting(CallbackInfoReturnable<Float> cir) {
		if (cir.getReturnValueF() < 1.0F && (Object) this instanceof Player player
			&& ArmorPerks.owns(player, ArmorPerks.SURE_FOOTING)) {
			cir.setReturnValue(1.0F);
		}
	}
}
