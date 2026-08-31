package dev.pbenchants.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A class tab: the class icon and its name, lit up when it is the tree on
 * screen. Planned classes use the same tab greyed out and inert, so the shape
 * of the mod is visible before the trees behind them exist.
 */
public class ClassTabWidget extends AbstractWidget {
	public static final int HEIGHT = 20;

	/** Width of a tab that shows only its class icon. */
	public static final int ICON_ONLY_WIDTH = 22;

	private final ItemStack icon;
	private final boolean current;
	private final Runnable onPress;
	private final Font font = Minecraft.getInstance().font;

	public ClassTabWidget(int x, int y, int width, ItemStack icon, Component label, boolean current,
			Runnable onPress) {
		super(x, y, width, HEIGHT, label);
		this.icon = icon;
		this.current = current;
		this.onPress = onPress;
	}

	/** Width a tab needs for this label: icon, text and padding. */
	public static int widthFor(Font font, Component label) {
		return 22 + font.width(label) + 6;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		onPress.run();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!active || !visible || !event.isSelection()) {
			return false;
		}
		playDownSound(Minecraft.getInstance().getSoundManager());
		onPress.run();
		return true;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = getX();
		int y = getY();
		int background = current ? 0xFF262D38 : 0xFF171A20;
		if (active && (isHovered() || isFocused())) {
			background = SkillTreeStyle.blend(background, 0xFF3A4250, 0.5f);
		}
		graphics.fill(x, y, x + width, y + HEIGHT, background);
		graphics.outline(x, y, width, HEIGHT, current ? SkillTreeStyle.GOLD : SkillTreeStyle.BORDER);
		if (current) {
			// The current tab bleeds into the tree below it.
			graphics.fill(x + 1, y + HEIGHT - 2, x + width - 1, y + HEIGHT, 0xFF262D38);
		}

		graphics.item(icon, x + 3, y + 2);
		if (!active) {
			graphics.fill(x + 3, y + 2, x + 19, y + 18, 0xAA101318);
		}

		int labelX = x + 22;
		int room = x + width - 3 - labelX;
		if (room >= 12) {
			String label = SkillTreeStyle.trim(font, getMessage().getString(), room);
			graphics.text(font, label, labelX, y + 6,
				!active ? SkillTreeStyle.DIM : current ? SkillTreeStyle.GOLD : SkillTreeStyle.TEXT);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", getMessage()));
	}
}
