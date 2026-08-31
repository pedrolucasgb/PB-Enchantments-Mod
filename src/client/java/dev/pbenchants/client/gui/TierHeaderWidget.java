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

import java.util.function.IntConsumer;

/**
 * The head of a tier column: "TIER 3" over the tier's own name, so a column is
 * "Mine Master" rather than a number. Clicking it puts the tier's gate
 * checklist in the details panel.
 */
public class TierHeaderWidget extends AbstractWidget {
	/** Tall enough for two lines of text plus the accent bar. */
	public static final int HEIGHT = 24;

	/** Where a tier stands relative to the player's progress. */
	public enum State {
		OPEN,
		NEXT,
		LOCKED
	}

	private final int tierIndex;
	private final Component tierName;
	private final State state;
	private final IntConsumer onPress;
	private final Font font = Minecraft.getInstance().font;
	private boolean selected;

	public TierHeaderWidget(int x, int y, int width, int tierIndex, Component tierName, State state,
			IntConsumer onPress) {
		super(x, y, width, HEIGHT, tierName);
		this.tierIndex = tierIndex;
		this.tierName = tierName;
		this.state = state;
		this.onPress = onPress;
	}

	public TierHeaderWidget selected(boolean selected) {
		this.selected = selected;
		return this;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		onPress.accept(tierIndex);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!active || !visible || !event.isSelection()) {
			return false;
		}
		playDownSound(Minecraft.getInstance().getSoundManager());
		onPress.accept(tierIndex);
		return true;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = getX();
		int y = getY();
		int accent = switch (state) {
			case OPEN -> SkillTreeStyle.GREEN;
			case NEXT -> SkillTreeStyle.pulsingGold();
			case LOCKED -> SkillTreeStyle.BORDER;
		};

		int background = state == State.LOCKED ? 0xFF14171C : 0xFF1D222B;
		if (isHovered() || isFocused() || selected) {
			background = SkillTreeStyle.blend(background, 0xFF39414F, 0.5f);
		}
		graphics.fill(x, y, x + width, y + HEIGHT, background);
		graphics.outline(x, y, width, HEIGHT, selected ? SkillTreeStyle.BORDER_LIT : SkillTreeStyle.BORDER);
		// Accent bar on top: the column's status in one stroke.
		graphics.fill(x, y, x + width, y + 2, accent);

		String badge = "TIER " + (tierIndex + 1) + (state == State.OPEN ? " ✓" : " □");
		graphics.text(font, SkillTreeStyle.trim(font, badge, width - 6), x + 3, y + 5, accent);
		graphics.text(font, SkillTreeStyle.trim(font, tierName.getString(), width - 6), x + 3, y + 14,
			state == State.LOCKED ? SkillTreeStyle.DIM : SkillTreeStyle.TEXT);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", tierName));
	}
}
