package dev.pbenchants.client;

import dev.pbenchants.client.gui.SkillTreeStyle;
import dev.pbenchants.client.mixin.ContainerScreenAccessor;
import dev.pbenchants.network.AttunePayload;
import dev.pbenchants.network.SkillStatePayload;
import dev.pbenchants.perk.BeaconPerks;
import dev.pbenchants.skill.SkillNode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * Prism, where a beacon's choices are made: the beacon screen.
 *
 * <p>The beacon window offers the pyramid's powers; this adds one more row,
 * just above the window's top-right corner, for the power the <em>player</em>
 * brings to any beacon. One button per choice — nothing, Night Vision, Fire
 * Resistance, and with Prism II Slow Falling and Saturation — drawn with the
 * same effect sprites vanilla's own buttons use, the current choice framed in
 * gold. A choice rank II has not opened yet is drawn dimmed and says so on
 * hover. Nothing appears at all for a player without the node.
 *
 * <p>Pressing a button sends the index; the server validates the rank, stores
 * it in the tree's counters and pushes the snapshot back, which is what the
 * gold frame reads. The command {@code /pbenchants attune} does the same
 * thing from chat.
 */
public final class PrismScreenHooks {
	private static final int SIZE = 22;
	private static final int ICON = 18;
	private static final int GAP = 2;
	private static final int MARGIN = 3;

	/** Where the label sits, kept for the foreground pass. */
	private static int labelX;
	private static int labelY;
	private static boolean attached;

	private PrismScreenHooks() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			attached = false;
			if (!(screen instanceof BeaconScreen beacon) || !ClientSkillState.owns("beacon", BeaconPerks.PRISM[0])) {
				return;
			}
			attach(beacon);
			ScreenEvents.afterForeground(screen).register(PrismScreenHooks::drawLabel);
			ScreenEvents.remove(screen).register(self -> attached = false);
		});
	}

	private static void attach(BeaconScreen screen) {
		ContainerScreenAccessor geometry = (ContainerScreenAccessor) screen;
		int right = geometry.pbenchants$leftPos() + geometry.pbenchants$imageWidth();
		int above = geometry.pbenchants$topPos() - SIZE - MARGIN;
		int y = above >= MARGIN
			? above
			: geometry.pbenchants$topPos() + geometry.pbenchants$imageHeight() + MARGIN;
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		int count = BeaconPerks.ATTUNEMENTS.size();
		int x = right - count * SIZE - (count - 1) * GAP;
		int rank = BeaconPerks.prismRank(Minecraft.getInstance().player);
		for (int index = 0; index < count; index++) {
			widgets.add(new PrismButton(x, y, index, BeaconPerks.ATTUNEMENTS.get(index),
				rank >= BeaconPerks.rankFor(index)));
			x += SIZE + GAP;
		}
		labelX = right - count * SIZE - (count - 1) * GAP - GAP * 2;
		labelY = y + (SIZE - 8) / 2;
		attached = true;
	}

	/** "Prism" to the left of the row, so the row is not five unexplained icons. */
	private static void drawLabel(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (!attached) {
			return;
		}
		var font = Minecraft.getInstance().font;
		Component label = Component.translatable("screen.pbenchants.prism.label");
		graphics.text(font, label, labelX - font.width(label), labelY, SkillTreeStyle.GOLD);
	}

	/** What the server last said the player chose. */
	private static int current() {
		SkillStatePayload.TreeState state = ClientSkillState.tree("beacon");
		return state == null ? 0 : state.counters().getOrDefault(BeaconPerks.ATTUNE_COUNTER, 0);
	}

	private static final class PrismButton extends AbstractWidget {
		private final int index;
		@Nullable
		private final Holder<MobEffect> effect;
		private final boolean unlocked;

		PrismButton(int x, int y, int index, @Nullable Holder<MobEffect> effect, boolean unlocked) {
			super(x, y, SIZE, SIZE, name(index, effect, unlocked));
			this.index = index;
			this.effect = effect;
			this.unlocked = unlocked;
			setTooltip(Tooltip.create(getMessage()));
			setTooltipDelay(Duration.ZERO);
		}

		private static Component name(int index, @Nullable Holder<MobEffect> effect, boolean unlocked) {
			if (effect == null) {
				return Component.translatable("screen.pbenchants.prism.none");
			}
			Component power = effect.value().getDisplayName();
			return unlocked
				? Component.translatable("screen.pbenchants.prism.power", power)
				: Component.translatable("screen.pbenchants.prism.locked", power,
					SkillNode.roman(BeaconPerks.rankFor(index)));
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubled) {
			if (unlocked) {
				ClientPlayNetworking.send(new AttunePayload(index));
			}
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			boolean chosen = current() == index;
			boolean lit = isHoveredOrFocused() && unlocked;
			graphics.fill(getX(), getY(), getX() + width, getY() + height,
				chosen || lit ? SkillTreeStyle.COLUMN_OPEN : SkillTreeStyle.PANEL_DEEP);
			graphics.outline(getX(), getY(), width, height,
				chosen ? SkillTreeStyle.GOLD : lit ? SkillTreeStyle.TEXT : SkillTreeStyle.BORDER_LIT);
			if (effect == null) {
				var font = Minecraft.getInstance().font;
				graphics.text(font, "✕", getX() + (width - font.width("✕")) / 2, getY() + (height - 8) / 2,
					chosen ? SkillTreeStyle.GOLD : unlocked ? SkillTreeStyle.TEXT : SkillTreeStyle.DIM);
				return;
			}
			int x = getX() + (width - ICON) / 2;
			int y = getY() + (height - ICON) / 2;
			if (unlocked) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect), x, y, ICON, ICON);
			} else {
				// A choice the next rank opens: there, but faded.
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect), x, y, ICON, ICON,
					0x60FFFFFF);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, getMessage());
		}
	}
}
