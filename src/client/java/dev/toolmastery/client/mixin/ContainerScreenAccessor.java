package dev.toolmastery.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The numbers every container screen keeps to itself: where its window sits
 * on screen and how big it is. The Artisan button row is anchored to the top-
 * right corner of that window and the slot overlays are drawn relative to its
 * origin, so both need these.
 *
 * <p>An accessor rather than an injector on purpose — nothing here changes
 * behaviour, it only reads a handful of protected ints.
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int toolmastery$leftPos();

	@Accessor("topPos")
	int toolmastery$topPos();

	/** Width of the window itself: the button row is right-aligned to its far edge. */
	@Accessor("imageWidth")
	int toolmastery$imageWidth();

	/** Height of the window, for the rare screen too tall to leave room above it. */
	@Accessor("imageHeight")
	int toolmastery$imageHeight();
}
