package dev.pbenchants.mixin;

import dev.pbenchants.storage.SteadyGrid;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Steady Grid — the crafting table remembers what you were building.
 *
 * <p>Both halves are one hook each, and neither has to fight vanilla for it.
 * On close, the grid is taken aside <em>before</em> vanilla empties it: what
 * vanilla then finds is an empty container, so its own "give it back" pass
 * quietly does nothing. On open, the stash goes back into the grid.
 *
 * <p>Anyone without the node falls straight through and gets vanilla's
 * behaviour untouched.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
	@Inject(method = "removed", at = @At("HEAD"))
	private void pbenchants$steadyGridKeepsTheLayout(Player player, CallbackInfo ci) {
		if (SteadyGrid.owns(player)) {
			SteadyGrid.stash(player, ((CraftingMenu) (Object) this).getInputGridSlots().getFirst().container);
		}
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;"
		+ "Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void pbenchants$steadyGridRestores(int syncId, Inventory inventory,
			ContainerLevelAccess access, CallbackInfo ci) {
		if (SteadyGrid.owns(inventory.player)) {
			SteadyGrid.restore(inventory.player,
				((CraftingMenu) (Object) this).getInputGridSlots().getFirst().container);
		}
	}
}
