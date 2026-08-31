package dev.pbenchants.mixin;

import dev.pbenchants.perk.ExplorerPerks;
import dev.pbenchants.track.StorageTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two server-side features that hang off {@code ServerPlayer}: the Explorer's
 * Tireless line, and the Artisan's container bookkeeping.
 *
 * <p><b>Tireless</b> — covering ground costs less hunger.
 *
 * <p>The scaling is deliberately not applied to {@code causeFoodExhaustion}
 * itself. That method is the funnel for every exhaustion source in the game —
 * mining, taking damage, regenerating — and a blanket discount would turn a
 * movement perk into "you never eat again". Only the two methods that
 * <em>are</em> movement are redirected: the per-metre cost inside
 * {@code checkMovementStatistics}, which covers walking, sprinting and
 * swimming alike, and the flat cost of a jump. That is wider than "sprinting
 * and jumping", and deliberately so: this class levels from swimming and
 * rowing too, and a discount that stopped at the waterline would read as a bug.
 *
 * <p>Both live on {@code ServerPlayer}, so this is a server-side mixin with no
 * client parity to worry about: the hunger bar is synced, not computed twice.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Redirect(method = "checkMovementStatistics", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
	private void pbenchants$tirelessWhileMoving(ServerPlayer player, float exhaustion) {
		player.causeFoodExhaustion(exhaustion * ExplorerPerks.exhaustionFactor(player));
	}

	@Redirect(method = "jumpFromGround", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
	private void pbenchants$tirelessWhileJumping(ServerPlayer player, float exhaustion) {
		player.causeFoodExhaustion(exhaustion * ExplorerPerks.exhaustionFactor(player));
	}

	// ---------- Artisan: deposits and Tidy Storage ----------

	/**
	 * Every menu this player opens is snapshotted here and settled in
	 * {@code doCloseContainer} — see {@link StorageTracker} for why the
	 * bookkeeping lives at the menu rather than at the slot.
	 */
	@Inject(method = "initMenu", at = @At("TAIL"))
	private void pbenchants$snapshotContainer(AbstractContainerMenu menu, CallbackInfo ci) {
		StorageTracker.onMenuOpened((ServerPlayer) (Object) this, menu);
	}

	@Inject(method = "doCloseContainer", at = @At("HEAD"))
	private void pbenchants$settleContainer(CallbackInfo ci) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		StorageTracker.onMenuClosed(self, self.containerMenu);
	}
}
