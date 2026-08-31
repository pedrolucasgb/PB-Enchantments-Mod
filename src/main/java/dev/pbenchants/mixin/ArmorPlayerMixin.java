package dev.pbenchants.mixin;

import dev.pbenchants.track.ArmorTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Armor tree's absorb gate, measured where the absorbing happens.
 *
 * <p>The gate used to be computed from Fabric's {@code AFTER_DAMAGE} event as
 * {@code base - taken} — but that event fires at the tail of
 * {@code hurtServer}, where the amount has only been reduced by the
 * <em>shield</em>: armour, Protection and Resistance are all applied inside
 * {@code actuallyHurt}, on a local the event never sees. So {@code base -
 * taken} was zero on every unblocked hit and the counter never moved.
 *
 * <p>Here the number is read off vanilla's own reduction pair instead:
 * {@code amount} on the way into {@code getDamageAfterArmorAbsorb} minus
 * {@code amount} on the way out of {@code getDamageAfterMagicAbsorb} is
 * exactly what the set and its enchantments soaked out of this hit. Shield
 * blocking was already subtracted upstream, so a blocked hit no longer needs
 * excluding — what reaches this method is what the armour was left to deal
 * with. {@code Player} overrides {@code actuallyHurt} (for food exhaustion),
 * which is why this mixin targets {@code Player} and not {@code LivingEntity}.
 */
@Mixin(Player.class)
public abstract class ArmorPlayerMixin {
	/** The damage walking into the armour, remembered across the two calls below. */
	@Unique
	private float pbenchants$incomingDamage;

	@Inject(method = "actuallyHurt", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
	private void pbenchants$noteIncoming(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
		pbenchants$incomingDamage = amount;
	}

	@ModifyVariable(method = "actuallyHurt", argsOnly = true, at = @At(value = "INVOKE_ASSIGN",
		target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
	private float pbenchants$countAbsorbed(float reduced) {
		if ((Player) (Object) this instanceof ServerPlayer player) {
			ArmorTracker.onArmorAbsorb(player, pbenchants$incomingDamage - reduced);
		}
		return reduced;
	}
}
