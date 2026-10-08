package dev.pbenchants.client;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.client.gui.SkillTreeStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Says so, on screen, while Diggy Diggy Hole is running.
 *
 * <p>A held ability that keeps breaking blocks on its own has to be visible or it
 * reads as a bug the first time it eats a wall behind you. The chat line only
 * shows the moment it flips; this stays for as long as the aura does.
 *
 * <p>Server-driven: the client is told on the transition and nowhere else, so what
 * is drawn is what the server thinks is true, including every way the ability
 * turns itself off.
 */
public final class DiggyHud {
	private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(PBEnchants.MOD_ID, "diggy_diggy_hole");

	/**
	 * Built on demand, never in a static field: client init runs before item
	 * components are bound, and an ItemStack made that early throws
	 * "Components not bound yet" and takes the whole entrypoint with it.
	 */
	private static ItemStack icon;

	private static boolean active;

	private DiggyHud() {
	}

	public static void register() {
		net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, delta) -> {
			if (!net.minecraft.client.Minecraft.getInstance().options.hideGui) {
				draw(graphics);
			}
		});
	}

	public static void setActive(boolean value) {
		active = value;
	}

	/** Leaving a world must not leave the badge on for the next one. */
	public static void clear() {
		active = false;
	}

	private static void draw(net.minecraft.client.gui.GuiGraphics graphics) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (!active || player == null || player.isSpectator()) {
			return;
		}
		Component label = Component.translatable("hud.pbenchants.diggy_diggy_hole");
		int width = client.font.width(label);

		// Centred above the hotbar, high enough to clear the armour and hunger
		// rows on both sides of it.
		int x = graphics.guiWidth() / 2 - (width + 20) / 2;
		int y = graphics.guiHeight() - 72;

		if (icon == null) {
			icon = new ItemStack(Items.NETHERITE_SHOVEL);
		}
		graphics.renderItem(icon, x, y - 4);
		graphics.drawString(client.font, label, x + 20, y, SkillTreeStyle.GOLD);
	}
}
