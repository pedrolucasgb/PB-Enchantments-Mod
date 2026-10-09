package dev.pbenchants.client.mixin;

import dev.pbenchants.client.ArtisanScreenHooks;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Seeker's Eye: the recipe book must not hear the keyboard while the magnifier
 * holds the caret.
 *
 * <p>{@link ContainerScreenSearchMixin} stops the <em>screen's</em> shortcuts,
 * but the inventory and crafting screens are {@link AbstractRecipeBookScreen}s,
 * and their {@code keyPressed}/{@code charTyped} offer every key to
 * {@code recipeBookComponent} <b>before</b> the chain that mixin guards ever
 * runs. Worse, the recipe book's own search box keeps {@code isFocused() ==
 * true} on its own — it is a plain field, not a focus-managed child — so a T
 * typed into the Seeker's Eye landed in the crafting search too, and with the
 * book open the first keystroke yanked the caret there outright.
 *
 * <p>So: while the Seeker's Eye field has focus, the recipe book is told the
 * key did nothing. The event then falls through to the container screen, whose
 * focused-widget pass delivers it to the magnifier's field — and only there.
 */
@Mixin(RecipeBookComponent.class)
public class RecipeBookScreenSearchMixin {
	// 1.21.1: InventoryScreen, CraftingScreen and AbstractFurnaceScreen each
	// feed the recipe book first in their own keyPressed/charTyped, so the
	// guard sits on the recipe book itself instead of on three call sites.
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void pbenchants$seekersEyeOwnsKeys(int keyCode, int scanCode, int modifiers,
			CallbackInfoReturnable<Boolean> cir) {
		if (ArtisanScreenHooks.searchHasFocus()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void pbenchants$seekersEyeOwnsChars(char character, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (ArtisanScreenHooks.searchHasFocus()) {
			cir.setReturnValue(false);
		}
	}
}
