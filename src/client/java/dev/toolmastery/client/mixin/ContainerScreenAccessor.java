package dev.toolmastery.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The two numbers every container screen keeps to itself: where its window sits
 * on screen. The Artisan buttons hang off the left edge of that window and the
 * pinned-slot markers are drawn relative to it, so both need the origin.
 *
 * <p>An accessor rather than an injector on purpose — nothing here changes
 * behaviour, it only reads two protected ints.
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int toolmastery$leftPos();

	@Accessor("topPos")
	int toolmastery$topPos();

	/** Width of the window itself, which is where the search panel starts. */
	@Accessor("imageWidth")
	int toolmastery$imageWidth();
}
