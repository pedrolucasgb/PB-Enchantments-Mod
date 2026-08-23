package dev.toolmastery.client.mixin;

import dev.toolmastery.client.EnchantPreviewState;
import dev.toolmastery.enchant.EnchanterPerks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Enchanter-class additions to the enchanting table screen:
 *
 * - Arcane Insight: a side panel listing the true enchantments behind each
 *   offer the player can read (data arrives via EnchantPreviewPayload).
 * - Inner Focus: the lapis count is treated as full so the offers render (and
 *   click) enabled without lapis — the server validates the real perk.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
	// palette mirrors SkillTreeScreen
	private static final int COLOR_PANEL = 0xE0191C22;
	private static final int COLOR_PANEL_BORDER = 0xFF2C313B;
	private static final int COLOR_GOLD = 0xFFF2C94C;
	private static final int COLOR_TEXT = 0xFFE8E6DF;

	protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void toolmastery$resetInsightPanel(CallbackInfo ci) {
		EnchantPreviewState.clear();
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void toolmastery$drawInsightPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
	                                            float delta, CallbackInfo ci) {
		if (EnchantPreviewState.isEmpty()) {
			return;
		}
		int x = leftPos + imageWidth + 4;
		int y = topPos + 20;

		// measure first so the backdrop hugs the content
		int lineCount = 0;
		int widest = font.width(Component.translatable("screen.toolmastery.insight_title").getString());
		for (int slot = 0; slot < 3; slot++) {
			List<Component> lines = EnchantPreviewState.slot(slot);
			if (lines.isEmpty()) {
				continue;
			}
			lineCount += 1 + lines.size();
			widest = Math.max(widest,
				font.width(Component.translatable("screen.toolmastery.insight_slot", slot + 1).getString()));
			for (Component line : lines) {
				widest = Math.max(widest, font.width(line.getString()) + 6);
			}
		}
		if (lineCount == 0) {
			return;
		}
		int panelWidth = widest + 8;
		int panelHeight = 14 + lineCount * 10 + 4;
		graphics.fill(x, y, x + panelWidth, y + panelHeight, COLOR_PANEL);
		graphics.outline(x, y, panelWidth, panelHeight, COLOR_PANEL_BORDER);

		int textY = y + 4;
		graphics.text(font, Component.translatable("screen.toolmastery.insight_title"), x + 4, textY, COLOR_GOLD);
		textY += 12;
		for (int slot = 0; slot < 3; slot++) {
			List<Component> lines = EnchantPreviewState.slot(slot);
			if (lines.isEmpty()) {
				continue;
			}
			graphics.text(font, Component.translatable("screen.toolmastery.insight_slot", slot + 1),
				x + 4, textY, COLOR_GOLD);
			textY += 10;
			for (Component line : lines) {
				graphics.text(font, line, x + 10, textY, COLOR_TEXT);
				textY += 10;
			}
		}
	}

	/**
	 * Inner Focus visual: the screen gates the offer sprites, cost colors and
	 * the pre-click check on the synced lapis count; owners see them enabled.
	 */
	@Redirect(method = {"extractBackground", "extractRenderState"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/inventory/EnchantmentMenu;getGoldCount()I"))
	private int toolmastery$innerFocusGoldCount(EnchantmentMenu menu) {
		if (minecraft != null && minecraft.player != null
				&& EnchanterPerks.owns(minecraft.player, EnchanterPerks.INNER_FOCUS)) {
			return 64;
		}
		return menu.getGoldCount();
	}
}
