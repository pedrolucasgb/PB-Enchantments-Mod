package dev.pbenchants.client.mixin;

import dev.pbenchants.client.ContainerForeground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link ContainerForeground} once the slots are drawn. Vanilla has the
 * pose translated to the window origin here, so it is undone for the listeners
 * (who work in screen coordinates), and lifted above the item layer so a frame
 * or a highlight reads on top of the stack, while staying under the tooltip.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenForegroundMixin {
	@Shadow
	protected int leftPos;

	@Shadow
	protected int topPos;

	@Inject(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
	private void pbenchants$foreground(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		graphics.pose().pushPose();
		graphics.pose().translate(-leftPos, -topPos, 210.0F);
		ContainerForeground.fire((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY, delta);
		graphics.pose().popPose();
	}
}
