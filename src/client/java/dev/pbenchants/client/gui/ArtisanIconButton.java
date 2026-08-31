package dev.pbenchants.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.time.Duration;

/**
 * One small square button with a symbol on it, of the sort that sits in the
 * corner of a container screen.
 *
 * <p>Vanilla's {@code Button} is twenty pixels tall and wears a label; a row of
 * five of those would be a second window. This one is the size of an inventory
 * slot, draws its own frame in the mod's palette, and says what it is on hover
 * instead of in writing — which is the only way a symbol is ever learnable.
 *
 * <p>The tooltip has no delay on purpose. A delayed tooltip is fine on a button
 * whose label already says what it does; on a button that is a picture, the
 * delay is the difference between a discoverable control and a guess.
 */
public class ArtisanIconButton extends AbstractWidget {
	/** Side of the button — one inventory slot, so it reads as part of the GUI. */
	public static final int SIZE = 16;

	/** Draws the symbol. Given the inner corner and the colour to use. */
	@FunctionalInterface
	public interface Symbol {
		void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, int color);
	}

	private final Symbol symbol;
	private final Runnable onPress;
	private final Font font = Minecraft.getInstance().font;

	public ArtisanIconButton(int x, int y, Component name, Symbol symbol, Runnable onPress) {
		super(x, y, SIZE, SIZE, name);
		this.symbol = symbol;
		this.onPress = onPress;
		setTooltip(Tooltip.create(name));
		setTooltipDelay(Duration.ZERO);
	}

	/** Retitles the button, tooltip included — the sort-order button changes on every press. */
	public void rename(Component name) {
		setMessage(name);
		setTooltip(Tooltip.create(name));
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubled) {
		onPress.run();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		boolean lit = isHoveredOrFocused();
		graphics.fill(getX(), getY(), getX() + width, getY() + height,
			lit ? SkillTreeStyle.COLUMN_OPEN : SkillTreeStyle.PANEL_DEEP);
		graphics.outline(getX(), getY(), width, height,
			lit ? SkillTreeStyle.GOLD : SkillTreeStyle.BORDER_LIT);
		symbol.draw(graphics, font,
			getX() + (width - ArtisanIcons.SIZE) / 2,
			getY() + (height - ArtisanIcons.SIZE) / 2,
			lit ? SkillTreeStyle.GOLD : SkillTreeStyle.TEXT);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, getMessage());
	}
}
