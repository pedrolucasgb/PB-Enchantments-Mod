package dev.pbenchants.client.mixin;

import dev.pbenchants.client.ArtisanScreenHooks;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Seeker's Eye: while you are typing in the search field, the container screen
 * stops listening for its own shortcuts.
 *
 * <p>Searching for anything with an <b>E</b> in it shut the inventory. The
 * reason is the order in {@code AbstractContainerScreen.keyPressed}: the focused
 * widget is offered the key first, but an {@code EditBox} only claims keys it
 * <em>acts</em> on — backspace, the arrows — and lets plain letters fall through
 * to arrive later as {@code charTyped}. So the letter reached the field
 * <em>and</em> the screen's own handling of it, which for E is "close the
 * inventory". Q dropped the hovered stack, 1-9 swapped it into the hotbar and
 * the middle-click bind cloned it, all for the same reason.
 *
 * <p>The fix is to end {@code keyPressed} the moment vanilla is about to start
 * reading the key as a shortcut — the first {@code KeyMapping.matches} call,
 * which is the inventory key — and only while the field actually holds the
 * caret. Everything before that point still runs, so the field keeps its
 * editing keys; everything after it is what the field is protected from. Click
 * a slot and focus leaves the field, at which point E closes the screen again,
 * as it should.
 *
 * <p>Escape is untouched: {@code Screen.keyPressed} handles it before any of
 * this, so it still closes the screen from inside the field.
 */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenSearchMixin {
	@Inject(method = "keyPressed", cancellable = true, at = @At(value = "INVOKE", ordinal = 0,
		target = "Lnet/minecraft/client/KeyMapping;matches(Lnet/minecraft/client/input/KeyEvent;)Z"))
	private void pbenchants$searchFieldEatsShortcuts(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (ArtisanScreenHooks.searchHasFocus()) {
			cir.setReturnValue(true);
		}
	}
}
