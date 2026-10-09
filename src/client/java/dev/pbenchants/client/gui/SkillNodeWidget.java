package dev.pbenchants.client.gui;

import dev.pbenchants.skill.SkillNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * One node of the tree: an item icon in a coloured frame, with its name beside
 * it when the column is wide enough for one.
 *
 * <p>The frame carries the whole state of the node — owned, reachable, locked,
 * locked out by a capstone, or not built yet — and a stripe of the type colour
 * runs along the bottom edge, so a glance across a column tells you what is
 * passive and what is an enchantment without reading a word.
 */
public class SkillNodeWidget extends AbstractWidget {
	/** Icon box; leaves 3px of padding around a 16x16 item. */
	private static final int ICON = 16;

	/** Corner glyphs: owned, not-built-yet, and one-of-a-pick-one-group. */
	private static final String CHECK = "✓";
	private static final String STAR = "★";
	private static final String SCALES = "⚖";

	private final SkillNode node;
	private final NodeState state;
	private final ItemStack icon;
	private final Consumer<SkillNode> onPress;
	private final Font font = Minecraft.getInstance().font;
	private boolean selected;

	public SkillNodeWidget(int x, int y, int width, int height, SkillNode node, NodeState state,
			Consumer<SkillNode> onPress) {
		super(x, y, width, height, node.displayName());
		this.node = node;
		this.state = state;
		this.icon = node.iconStack();
		this.onPress = onPress;
	}

	public SkillNode node() {
		return node;
	}

	public SkillNodeWidget selected(boolean selected) {
		this.selected = selected;
		return this;
	}

	/** Where a connector line should meet this tile, in screen coordinates. */
	public int connectorY() {
		return getY() + height / 2;
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		onPress.accept(node);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!active || !visible || !net.minecraft.client.gui.navigation.CommonInputs.selected(keyCode)) {
			return false;
		}
		playDownSound(Minecraft.getInstance().getSoundManager());
		onPress.accept(node);
		return true;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		int x = getX();
		int y = getY();
		boolean highlight = isHovered() || isFocused() || selected;

		int background = state.background();
		if (highlight) {
			background = SkillTreeStyle.blend(background, 0xFF3A4250, 0.45f);
		}
		graphics.fill(x, y, x + width, y + height, background);

		// The frame reads the state; a selected node gets a second ring so it
		// stays obvious which one the details panel is talking about.
		int frame = state.frame();
		graphics.renderOutline(x, y, width, height, frame);
		if (selected) {
			graphics.renderOutline(x - 1, y - 1, width + 2, height + 2, SkillTreeStyle.TEXT);
		}

		int iconX = x + 3;
		int iconY = y + (height - ICON) / 2;
		graphics.renderItem(icon, iconX, iconY);
		if (state.dimmed()) {
			graphics.fill(iconX, iconY, iconX + ICON, iconY + ICON, 0x99101318);
		}

		// Type stripe along the bottom edge.
		graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1,
			SkillTreeStyle.typeColor(node.type()));

		int labelX = iconX + ICON + 3;
		// A node that shuts its siblings when bought wears the scales while the
		// choice is still open; once it is made, the ✓ and the red frame say it.
		boolean choice = !node.exclusiveWith().isEmpty()
			&& (state == NodeState.AVAILABLE || state == NodeState.LOCKED);
		boolean corner = state == NodeState.OWNED || state == NodeState.FUTURE || choice;
		// Leave the corner glyph its own space instead of letting the name run under it.
		int room = x + width - 3 - labelX - (corner ? 9 : 0);
		if (room >= 20) {
			String label = SkillTreeStyle.trim(font, getMessage().getString(), room);
			graphics.drawString(font, label, labelX, y + (height - 8) / 2, state.labelColor());
		}

		if (state == NodeState.OWNED) {
			graphics.drawString(font, CHECK, x + width - 8, y + 2, SkillTreeStyle.GREEN);
		} else if (state == NodeState.FUTURE) {
			graphics.drawString(font, STAR, x + width - 8, y + 2, SkillTreeStyle.SOON);
		} else if (choice) {
			graphics.drawString(font, SCALES, x + width - 8, y + 2, SkillTreeStyle.CHOICE);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", getMessage()));
	}
}
