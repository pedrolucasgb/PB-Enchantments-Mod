package dev.pbenchants.mixin;

import dev.pbenchants.track.BeaconTracker;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The beacon's payment, seen from the only place that knows who paid it.
 *
 * <p>{@code BeaconMenu.updateEffects} consumes the payment slot and returns
 * true, but the menu holds no player; the packet handler that calls it does.
 * So the payment is photographed here before the handler runs and, once it
 * has run, the slot being empty is the proof that the beacon took it. The
 * handler is entered twice per packet — once on the network thread, which
 * only reschedules it, and once on the server thread — and only the second
 * pass is allowed to look at the menu.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
	@Shadow
	public ServerPlayer player;

	@Unique
	private ItemStack pbenchants$beaconPayment = ItemStack.EMPTY;

	@Inject(method = "handleSetBeaconPacket", at = @At("HEAD"))
	private void pbenchants$rememberPayment(ServerboundSetBeaconPacket packet, CallbackInfo ci) {
		pbenchants$beaconPayment = ItemStack.EMPTY;
		if (player.level().getServer().isSameThread() && player.containerMenu instanceof BeaconMenu menu) {
			pbenchants$beaconPayment = menu.getSlot(0).getItem().copy();
		}
	}

	@Inject(method = "handleSetBeaconPacket", at = @At("RETURN"))
	private void pbenchants$paymentTaken(ServerboundSetBeaconPacket packet, CallbackInfo ci) {
		ItemStack paid = pbenchants$beaconPayment;
		pbenchants$beaconPayment = ItemStack.EMPTY;
		if (paid.isEmpty() || !(player.containerMenu instanceof BeaconMenu menu) || menu.hasPayment()) {
			return;
		}
		BeaconTracker.onBeaconPaid(player, menu, paid);
	}
}
