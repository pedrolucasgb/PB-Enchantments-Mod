package dev.pbenchants.client;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.client.gui.SkillTreeStyle;
import dev.pbenchants.perk.BowPerks;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Quiver Sense — the fact vanilla has genuinely never told you: which arrow
 * the bow will actually fire, and how many of it you carry.
 *
 * <p>Vanilla picks ammunition by a rule nobody can see (offhand first, then
 * the first match in the hotbar order), which is exactly how a tipped arrow of
 * Harming ends up spent on a zombie. This asks the same {@code getProjectile}
 * the shot will ask, so the label is never a guess — it is the arrow that will
 * leave the string, by name, with the count of identical ones behind it.
 *
 * <p>Drawn just right of centre above the hotbar, mirroring Set Sense's spot
 * on the armour side. Pure QoL: nothing here changes a shot.
 */
public final class QuiverSenseHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "quiver_sense");

	private QuiverSenseHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, ID, (graphics, delta) -> draw(graphics));
	}

	private static void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null || player.isSpectator() || !BowPerks.owns(player, BowPerks.QUIVER_SENSE)) {
			return;
		}
		ItemStack weapon = heldRangedWeapon(player);
		if (weapon.isEmpty()) {
			return;
		}
		ItemStack ammo = player.getProjectile(weapon);
		String text = ammo.isEmpty()
			? Component.translatable("perk.pbenchants.quiver_sense.empty").getString()
			: ammo.getHoverName().getString() + " ×" + countMatching(player, ammo);

		// Right of centre, one line above where the food bar starts — the
		// mirror of Set Sense's seat on the armour side.
		int x = graphics.guiWidth() / 2 + 91 - client.font.width(text);
		int y = graphics.guiHeight() - 49 - 10;
		graphics.text(client.font, text, x, y, SkillTreeStyle.MUTED);
	}

	private static ItemStack heldRangedWeapon(LocalPlayer player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem) {
			return main;
		}
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof BowItem || off.getItem() instanceof CrossbowItem) {
			return off;
		}
		return ItemStack.EMPTY;
	}

	/** How many arrows identical to the chosen one the whole inventory holds. */
	private static int countMatching(LocalPlayer player, ItemStack ammo) {
		int total = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, ammo)) {
				total += stack.getCount();
			}
		}
		return total;
	}
}
