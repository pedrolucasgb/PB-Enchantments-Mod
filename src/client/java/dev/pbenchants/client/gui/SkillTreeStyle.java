package dev.pbenchants.client.gui;

import dev.pbenchants.skill.SkillType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The look of the skill screen in one place: the palette, and the handful of
 * shapes everything else is built out of (panels, badges, bars, connectors).
 *
 * <p>All of it is drawn from solid rectangles rather than a texture sheet, so
 * the screen scales to any window size and a new class or node needs no art —
 * it inherits the frame, the badge colour and the connector routing for free.
 */
public final class SkillTreeStyle {
	private SkillTreeStyle() {
	}

	// --- surfaces ---
	public static final int BACKDROP = 0xE60B0D11;
	public static final int PANEL = 0xF01A1E25;
	public static final int PANEL_DEEP = 0xF012151B;
	public static final int COLUMN_OPEN = 0x30202634;
	public static final int COLUMN_LOCKED = 0x30000000;
	public static final int BORDER = 0xFF2E3542;
	public static final int BORDER_LIT = 0xFF4A5666;

	// --- text ---
	public static final int TEXT = 0xFFE8E6DF;
	public static final int MUTED = 0xFF9AA1AD;
	public static final int DIM = 0xFF6C7280;

	// --- states ---
	public static final int GREEN = 0xFF5FBF4F;
	public static final int GOLD = 0xFFF2C94C;
	public static final int GOLD_DEEP = 0xFF8A6A1E;
	public static final int BAD = 0xFFE86B6B;
	public static final int SOON = 0xFFFFA94D;
	public static final int XP_GREEN = 0xFF80FF20;

	/** Height of a node tile, and the pitch between two stacked nodes. */
	public static final int NODE_HEIGHT = 22;
	public static final int NODE_PITCH = 26;

	/** Badge colour of a node type — the same hue the node tile is striped with. */
	public static int typeColor(SkillType type) {
		return switch (type) {
			case ENCHANTMENT -> 0xFFB07CE8;
			case PASSIVE -> 0xFF5FBF8A;
			case ACTIVE -> 0xFF6FA8E8;
			case ITEM -> 0xFFE8A45F;
		};
	}

	public static MutableComponent typeName(SkillType type) {
		return Component.translatable("screen.pbenchants.type." + type.id());
	}

	/** Blends two ARGB colours; {@code t} of 0 gives {@code a}, 1 gives {@code b}. */
	public static int blend(int a, int b, float t) {
		int out = 0;
		for (int shift = 0; shift < 32; shift += 8) {
			int ca = (a >> shift) & 0xFF;
			int cb = (b >> shift) & 0xFF;
			out |= (int) (ca + (cb - ca) * t) << shift;
		}
		return out;
	}

	/**
	 * A slow breath between two colours, shared by every pulsing element on the
	 * screen so they beat together instead of drifting apart.
	 */
	public static float pulse() {
		double phase = (System.currentTimeMillis() % 1600L) / 1600.0 * Math.PI * 2;
		return (float) ((Math.sin(phase) + 1) / 2);
	}

	/** The gold of a node you can afford to reach right now, breathing. */
	public static int pulsingGold() {
		return blend(GOLD_DEEP, GOLD, pulse());
	}

	/** Filled panel with a 1px border. */
	public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill, int border) {
		graphics.fill(x, y, x + width, y + height, fill);
		graphics.outline(x, y, width, height, border);
	}

	/** Cuts a string down to {@code maxWidth} pixels, ellipsing it if it does not fit. */
	public static String trim(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		StringBuilder sb = new StringBuilder(text);
		while (sb.length() > 1 && font.width(sb + "...") > maxWidth) {
			sb.setLength(sb.length() - 1);
		}
		return sb + "...";
	}

	/** The coloured pill behind a type name in the details panel. Returns its width. */
	public static int badge(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int color) {
		int textWidth = font.width(label);
		int width = textWidth + 8;
		graphics.fill(x, y, x + width, y + 11, (color & 0x00FFFFFF) | 0x33000000);
		graphics.outline(x, y, width, 11, color);
		graphics.text(font, label, x + 4, y + 2, color);
		return width;
	}

	/** A thin have/need bar — used for gate lines and for the player's XP. */
	public static void progressBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
			float progress, int fill) {
		graphics.fill(x, y, x + width, y + height, 0xFF0A0C10);
		graphics.outline(x, y, width, height, 0xFF2A303A);
		int filled = (int) ((width - 2) * Math.clamp(progress, 0f, 1f));
		if (filled > 0) {
			graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, fill);
		}
	}

	/** Text with the HUD's black outline, for anything drawn over busy pixels. */
	public static void outlinedText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.text(font, text, x + 1, y, 0xFF000000);
		graphics.text(font, text, x - 1, y, 0xFF000000);
		graphics.text(font, text, x, y + 1, 0xFF000000);
		graphics.text(font, text, x, y - 1, 0xFF000000);
		graphics.text(font, text, x, y, color);
	}
}
