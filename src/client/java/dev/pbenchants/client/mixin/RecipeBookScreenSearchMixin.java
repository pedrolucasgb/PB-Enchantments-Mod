package dev.pbenchants.client.mixin;

import dev.pbenchants.client.ArtisanScreenHooks;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
@Mixin(AbstractRecipeBookScreen.class)
public class RecipeBookScreenSearchMixin {
	@Redirect(method = "keyPressed", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
	private boolean pbenchants$seekersEyeOwnsKeys(RecipeBookComponent<?> component, KeyEvent event) {
		if (ArtisanScreenHooks.searchHasFocus()) {
			return false;
		}
		return component.keyPressed(event);
	}

	@Redirect(method = "charTyped", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"))
	private boolean pbenchants$seekersEyeOwnsChars(RecipeBookComponent<?> component, CharacterEvent event) {
		if (ArtisanScreenHooks.searchHasFocus()) {
			return false;
		}
		return component.charTyped(event);
	}
}
