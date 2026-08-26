package dev.toolmastery.mixin;

import dev.toolmastery.enchant.EnchanterPerks;
import dev.toolmastery.track.EnchantTracker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The anvil: the Arcanist gate, and the Anvil Adept perk.
 *
 * <p><b>The gate.</b> Only a real merge counts: renaming or repairing with raw
 * material leaves the sacrifice slot without enchantments, and those takes are
 * ignored. Injected at HEAD because onTake is where vanilla clears the inputs —
 * a tick later they are already gone. The inputs are read off the player's open
 * menu because inputSlots lives on ItemCombinerMenu, and a {@code @Shadow} only
 * reaches fields declared in the target class itself.
 *
 * <p><b>Anvil Adept</b> takes the two teeth out of the anvil. The
 * <em>"Too Expensive!"</em> wall is a hard stop at 40 levels that no amount of
 * XP gets you past — the anvil simply refuses — and it is what makes a
 * well-loved tool eventually unrepairable. And every combine costs 30% fewer
 * levels, applied to the final figure so the discount is the one the player
 * reads on the label and the one {@code onTake} actually charges.
 *
 * <p>Neither is done by rewriting vanilla's arithmetic. The wall is a single
 * {@code hasInfiniteMaterials} branch, so the perk answers that question
 * instead — the same trick Inner Focus uses on the lapis check — and the
 * discount lands at the very end of {@code createResult}, after the last thing
 * vanilla does to the price.
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	/** What a combine costs an Anvil Adept, as a fraction of vanilla. */
	@Unique
	private static final float ADEPT_DISCOUNT = 0.7F;

	@Shadow
	@Final
	private DataSlot cost;

	@Unique
	private Player toolmastery$player;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
		at = @At("RETURN"))
	private void toolmastery$capturePlayer(int containerId, Inventory inventory, ContainerLevelAccess access,
			CallbackInfo ci) {
		this.toolmastery$player = inventory.player;
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void toolmastery$trackCombine(Player player, ItemStack result, CallbackInfo ci) {
		if (!(player.containerMenu instanceof AnvilMenu menu)) {
			return;
		}
		ItemStack sacrifice = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		if (!sacrifice.isEmpty() && !EnchantmentHelper.getEnchantmentsForCrafting(sacrifice).isEmpty()) {
			EnchantTracker.onAnvilCombine(player);
		}
	}

	/**
	 * Anvil Adept: no "Too Expensive!". Ordinal 1 is the branch that throws the
	 * result away once the price reaches 40 — creative players are exempt from
	 * it, and this makes an Adept exempt the same way. The other
	 * {@code hasInfiniteMaterials} call in this method guards material cost and
	 * is left alone.
	 */
	@Redirect(method = "createResult", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z", ordinal = 1))
	private boolean toolmastery$adeptIgnoresTheWall(Player player) {
		return player.hasInfiniteMaterials() || EnchanterPerks.owns(player, EnchanterPerks.ANVIL_ADEPT);
	}

	/**
	 * Anvil Adept: 30% off, applied last so nothing downstream re-inflates it.
	 * A priced job never falls to zero — a free anvil would make the Arcanist
	 * gate that counts combines self-completing.
	 */
	@Inject(method = "createResult", at = @At("TAIL"))
	private void toolmastery$adeptDiscount(CallbackInfo ci) {
		int full = cost.get();
		if (full <= 0 || toolmastery$player == null
			|| !EnchanterPerks.owns(toolmastery$player, EnchanterPerks.ANVIL_ADEPT)) {
			return;
		}
		cost.set(Math.max(1, Math.round(full * ADEPT_DISCOUNT)));
	}
}
