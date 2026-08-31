package dev.pbenchants.mixin;

import dev.pbenchants.perk.ExplorerPerks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sea Legs — boats you steer move about 15% faster.
 *
 * <p>The push is applied after vanilla has finished steering, and it is capped:
 * multiplying velocity every tick without a ceiling compounds against the
 * boat's own drag and would end in a boat travelling at the speed of sound.
 * With the cap it simply settles at a higher cruising speed, which is what the
 * perk promises.
 *
 * <p>Only the player actually holding the tiller benefits — a passenger in
 * someone else's boat gets whatever the driver has earned, which is the same
 * rule the rest of the mod applies to borrowed gear.
 */
@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {
	@Unique
	private static final double PBENCHANTS$SEA_LEGS_FACTOR = 1.15;

	/** Vanilla boats top out around 0.4 blocks/tick on water; this is that plus the bonus. */
	@Unique
	private static final double PBENCHANTS$SPEED_CAP = 0.46;

	@Inject(method = "controlBoat", at = @At("TAIL"))
	private void pbenchants$seaLegs(CallbackInfo ci) {
		AbstractBoat boat = (AbstractBoat) (Object) this;
		LivingEntity pilot = boat.getControllingPassenger();
		if (!(pilot instanceof Player player) || !ExplorerPerks.owns(player, ExplorerPerks.SEA_LEGS)) {
			return;
		}
		Vec3 movement = boat.getDeltaMovement();
		double speed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
		if (speed <= 1.0E-4) {
			return;
		}
		double factor = Math.min(PBENCHANTS$SEA_LEGS_FACTOR, PBENCHANTS$SPEED_CAP / speed);
		if (factor > 1.0) {
			boat.setDeltaMovement(movement.x * factor, movement.y, movement.z * factor);
		}
	}
}
