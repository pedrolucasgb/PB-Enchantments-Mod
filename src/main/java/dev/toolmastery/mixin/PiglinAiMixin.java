package dev.toolmastery.mixin;

import dev.toolmastery.perk.ArmorPerks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nightplate, the gold set bonus: piglins do not turn on you.
 *
 * <p>Vanilla already lets a single gold piece keep them neutral while you walk
 * past — what it does not forgive is opening a chest or breaking gold in front
 * of them, and that is the whole bonus. A player in a full gold set is one of
 * them, and one of them can open a chest.
 *
 * <p>Everything else Nightplate promises is already vanilla's: netherite grants
 * knockback resistance on its own, and the leather half — freezing — is in
 * {@code ArmorUpkeep}, because vanilla only ever gave that to the boots.
 */
@Mixin(PiglinAi.class)
public class PiglinAiMixin {
	@Inject(method = "angerNearbyPiglins", at = @At("HEAD"), cancellable = true)
	private static void toolmastery$nightplateKeepsThemCalm(ServerLevel level, Player player, boolean requireVisible,
	                                                         CallbackInfo ci) {
		if ("gold".equals(ArmorPerks.nightplateMaterial(player))) {
			ci.cancel();
		}
	}
}
