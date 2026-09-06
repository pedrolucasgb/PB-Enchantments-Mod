package dev.pbenchants.mixin;

import dev.pbenchants.perk.XpOrbs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every experience the game hands out on the ground goes through
 * {@code awardWithDirection}: a mob's death, an ore, a furnace, a bottle, a
 * bred animal. Vanilla splits the amount into as many orbs as its value ladder
 * needs and the player then collects them one every two ticks — which is why
 * a Rich Vein swing or a night at a mob farm leaves someone standing in a
 * cloud of orbs for a minute. {@link XpOrbs} hands out the same experience in
 * fewer, larger orbs instead.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
	@Inject(method = "awardWithDirection", at = @At("HEAD"), cancellable = true)
	private static void pbenchants$fewerLargerOrbs(ServerLevel level, Vec3 pos, Vec3 direction, int amount,
			CallbackInfo ci) {
		if (amount <= 0) {
			return;
		}
		ci.cancel();
		XpOrbs.award(level, pos, direction, amount);
	}
}
