package dev.toolmastery.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * The panel Seeker's Eye answers into: a framed column of result lines beside
 * the container window.
 *
 * <p>It reads its lines through a supplier rather than holding them, so the
 * next answer from the server simply appears — no widget rebuild, no flicker
 * while you type.
 */
public class StorageResultWidget extends AbstractWidget {
	private static final int LINE_HEIGHT = 10;

	private final Supplier<List<String>> lines;
	private final Font font = Minecraft.getInstance().font;

	public StorageResultWidget(int x, int y, int width, int height, Supplier<List<String>> lines) {
		super(x, y, width, height, Component.translatable("screen.toolmastery.search.results"));
		this.lines = lines;
		this.active = false; // a read-out, not a control: never takes focus or clicks
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		List<String> content = lines.get();
		if (content.isEmpty()) {
			return;
		}
		int rows = Math.min(content.size(), height / LINE_HEIGHT);
		int boxHeight = rows * LINE_HEIGHT + 6;
		graphics.fill(getX(), getY(), getX() + width, getY() + boxHeight, SkillTreeStyle.PANEL);
		graphics.outline(getX(), getY(), width, boxHeight, SkillTreeStyle.BORDER);

		for (int row = 0; row < rows; row++) {
			String line = content.get(row);
			boolean nested = line.startsWith("  ");
			graphics.text(font, SkillTreeStyle.trim(font, line, width - 8),
				getX() + 4, getY() + 4 + row * LINE_HEIGHT,
				nested ? SkillTreeStyle.DIM : SkillTreeStyle.TEXT);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, getMessage());
	}
}
