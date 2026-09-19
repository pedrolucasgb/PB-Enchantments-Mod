package dev.pbenchants.client;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.client.gui.SkillTreeStyle;
import dev.pbenchants.perk.ArmorPerks;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Set Sense — the number vanilla hides behind ten identical icons.
 *
 * <p>The armour bar tells you how many half-shields you have and nothing about
 * what they are worth: two players with ten icons can be taking very different
 * amounts of damage, because toughness changes the curve and the bar does not
 * show toughness at all. This draws the three numbers that actually decide it —
 * armour, toughness, and the share of a hit they take off — just above the bar.
 *
 * <p>The percentage is quoted against a ten-point hit and says so, because
 * there is no single answer: vanilla's own formula makes armour worth less the
 * harder you are hit, which is the fact the node exists to surface.
 */
public final class SetSenseHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "set_sense");

	/** The hit the readout is quoted against. A ten-point blow: a strong mob, not a fall. */
	private static final float REFERENCE_HIT = 10.0F;

	private SetSenseHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, ID, (graphics, delta) -> draw(graphics));
	}

	private static void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		// No hidden-GUI check: Fabric does not run HUD elements at all while the
		// interface is off, so there is nothing here to suppress.
		if (player == null || player.isSpectator()
			|| !ArmorPerks.owns(player, ArmorPerks.SET_SENSE)) {
			return;
		}
		int armour = player.getArmorValue();
		double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		if (armour <= 0 && toughness <= 0) {
			return;
		}
		String text = armour + " / " + trim(toughness) + "  −" + Math.round(reduction(armour, toughness) * 100) + "%";

		// Just left of centre and one line above the armour bar, which is where
		// the bar itself starts — the readout sits with the thing it explains.
		int x = graphics.guiWidth() / 2 - 91;
		int y = armorBarY(graphics, player) - 10;
		graphics.text(client.font, text, x, y, SkillTreeStyle.MUTED);
	}

	/**
	 * Where vanilla draws the armour bar this frame. The health bar grows a row
	 * per twenty points of hearts <em>plus absorption</em>, and the armour bar
	 * rides on top of it — so under Absorption the bar climbs and, until 0.10.1,
	 * this readout stayed put and was drawn straight over it. Same arithmetic
	 * as {@code Hud.extractPlayerHealth}: the health it uses is vanilla's
	 * smoothed display value, which the current health stands in for here
	 * (off by a row only for a heartbeat after a big hit).
	 */
	private static int armorBarY(net.minecraft.client.gui.GuiGraphicsExtractor graphics, LocalPlayer player) {
		float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH),
			Mth.ceil(player.getHealth()));
		int absorption = Mth.ceil(player.getAbsorptionAmount());
		int rows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
		int rowHeight = Math.max(10 - (rows - 2), 3);
		return graphics.guiHeight() - 39 - (rows - 1) * rowHeight - 10;
	}

	/**
	 * Vanilla's own armour formula, asked about a reference hit: points are
	 * worth 4% each until toughness stops the "big hits punch through" term
	 * from biting, and the whole thing caps at 80%.
	 */
	private static float reduction(int armour, double toughness) {
		float divisor = 2.0F + (float) toughness / 4.0F;
		float effective = Math.clamp(armour - REFERENCE_HIT / divisor, armour * 0.2F, 20.0F);
		return effective / 25.0F;
	}

	/** "3.0" reads as noise on a HUD; "3" does not. */
	private static String trim(double value) {
		return value == Math.rint(value) ? String.valueOf((int) value) : String.format("%.1f", value);
	}
}
