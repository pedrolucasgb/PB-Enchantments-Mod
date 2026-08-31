package dev.pbenchants.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The symbols on the Artisan's buttons, drawn from letters and rectangles.
 *
 * <p>No texture sheet for the pictorial ones and no exotic glyphs from the
 * font: a texture would need art and an atlas, and a Unicode glyph is at the
 * mercy of whichever font pack the player is running. Rectangles render
 * identically everywhere and cost nothing — the same bargain the skill screen
 * already made in {@link SkillTreeStyle}.
 *
 * <p>Every symbol draws inside a {@link #SIZE}×{@link #SIZE} box whose top-left
 * corner is the {@code x, y} passed in, so a button only hands over its inner
 * corner and never has to know what it is drawing.
 */
public final class ArtisanIcons {
	/** The box every symbol is drawn inside. */
	public static final int SIZE = 12;

	private ArtisanIcons() {
	}

	/** A letter, centred in the box — the plainest symbol there is. */
	public static void letter(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.text(font, text, x + (SIZE - font.width(text)) / 2, y + (SIZE - font.lineHeight) / 2 + 1, color);
	}

	/**
	 * The same letter standing on a shelf: Sort, but aimed at the container
	 * rather than at you. One bar is enough to say "in there, not in here".
	 */
	public static void letterOnShelf(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.text(font, text, x + (SIZE - font.width(text)) / 2, y, color);
		graphics.fill(x + 1, y + SIZE - 2, x + SIZE - 1, y + SIZE - 1, color);
	}

	/**
	 * A magnifying glass: a ring with a handle running down to the right. The
	 * ring is a square — at twelve pixels a circle and a rounded square are the
	 * same picture, and the square one has no anti-aliasing to fake.
	 */
	public static void magnifier(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.outline(x, y, 8, 8, color);
		graphics.fill(x + 7, y + 7, x + 11, y + 11, color);
	}

	/**
	 * Quick Stack: three stacked bars with an arrow pushing right — off you and
	 * into the chests.
	 */
	public static void outbound(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x, y + 2, x + 5, y + 3, color);
		graphics.fill(x, y + 5, x + 5, y + 6, color);
		graphics.fill(x, y + 8, x + 5, y + 9, color);
		arrowRight(graphics, x + 7, y + 2, color);
	}

	/** Restock: the same bars, with the arrow pointing back at you. */
	public static void inbound(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x + 7, y + 2, x + 12, y + 3, color);
		graphics.fill(x + 7, y + 5, x + 12, y + 6, color);
		graphics.fill(x + 7, y + 8, x + 12, y + 9, color);
		arrowLeft(graphics, x + 1, y + 2, color);
	}

	/** Artisan's Order, {@link dev.pbenchants.storage.SortMode#CATEGORY}: a grid of bins. */
	public static void byCategory(GuiGraphicsExtractor graphics, int x, int y, int color) {
		for (int column = 0; column < 3; column++) {
			for (int row = 0; row < 3; row++) {
				graphics.fill(x + column * 4, y + row * 4, x + column * 4 + 3, y + row * 4 + 3, color);
			}
		}
	}

	/** Artisan's Order, {@link dev.pbenchants.storage.SortMode#COUNT}: a rising bar chart. */
	public static void byCount(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x, y + 8, x + 2, y + 12, color);
		graphics.fill(x + 4, y + 4, x + 6, y + 12, color);
		graphics.fill(x + 8, y, x + 10, y + 12, color);
	}

	/** A solid right-pointing triangle, 4 wide and 8 tall. */
	private static void arrowRight(GuiGraphicsExtractor graphics, int x, int y, int color) {
		for (int step = 0; step < 4; step++) {
			graphics.fill(x + step, y + step, x + step + 1, y + 8 - step, color);
		}
	}

	/** A solid left-pointing triangle, 4 wide and 8 tall. */
	private static void arrowLeft(GuiGraphicsExtractor graphics, int x, int y, int color) {
		for (int step = 0; step < 4; step++) {
			graphics.fill(x + 3 - step, y + step, x + 4 - step, y + 8 - step, color);
		}
	}
}
