package dev.pbenchants.client;

import dev.pbenchants.client.gui.ArtisanIconButton;
import dev.pbenchants.client.gui.ArtisanIcons;
import dev.pbenchants.client.gui.SkillTreeStyle;
import dev.pbenchants.client.mixin.ContainerScreenAccessor;
import dev.pbenchants.network.ArtisanActionPayload;
import dev.pbenchants.storage.SortMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the Artisan class actually lives: a row of small symbol buttons in the
 * top-right corner of the inventory and of every container screen.
 *
 * <p>Three rules shape it, and each one was a bug first:
 *
 * <ul>
 *   <li><b>Symbols, not words.</b> Six labelled vanilla buttons are a second
 *       window bolted onto the first. Slot-sized icons in a corner nobody was
 *       using cost no space, and the tooltip that appears the instant you hover
 *       one is what teaches it — a picture with a delayed tooltip is a guess.</li>
 *   <li><b>The layout is recomputed every frame.</b> Opening the recipe book
 *       slides the window sideways <em>without</em> re-running {@code init}, so
 *       anything positioned once at init is left stranded in the middle of the
 *       screen. {@link #layout} runs before every extract instead, off the
 *       window's live origin, and the row simply travels with it.</li>
 *   <li><b>Nothing appears until it is earned.</b> A player who has not touched
 *       the tree sees an inventory screen with nothing new on it at all.</li>
 * </ul>
 */
public final class ArtisanScreenHooks {
	private static final int GAP = 2;

	/** Distance from the window edge to the button row. */
	private static final int MARGIN = 3;

	private static final int SEARCH_WIDTH = 92;

	/** The yellow behind a slot Seeker's Eye matched — see-through, so the item still reads. */
	private static final int MATCH_FILL = 0x66FFE14D;

	/** This screen's buttons, in the order they are laid out from the corner leftwards. */
	private static final List<ArtisanIconButton> BUTTONS = new ArrayList<>();

	@Nullable
	private static ArtisanIconButton orderButton;

	@Nullable
	private static EditBox search;

	/** The order the order button is currently titled with, so it is only retitled on a change. */
	@Nullable
	private static SortMode shownMode;

	private ArtisanScreenHooks() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container)
				|| screen instanceof CreativeModeInventoryScreen) {
				return;
			}
			attach(container);
			ScreenEvents.beforeExtract(screen).register((self, graphics, mouseX, mouseY, delta) ->
				layout(container));
			ScreenEvents.afterForeground(screen).register(ArtisanScreenHooks::drawOverlay);
			ScreenMouseEvents.allowMouseClick(screen).register((clicked, event) ->
				!pinClicked(container, event));
			ScreenEvents.remove(screen).register(self -> {
				forget();
				ArtisanSearch.screenClosed();
			});
		});
	}

	// ---------- the button row ----------

	/**
	 * Builds the row for this screen. Positions are left at zero on purpose:
	 * {@link #layout} sets them before the first frame is drawn and on every
	 * frame after it, which is precisely what makes the recipe book harmless.
	 */
	private static void attach(AbstractContainerScreen<?> screen) {
		forget();
		if (!ArtisanSearch.available()) {
			// The node can be lost to a /pbenchants reset between two screens.
			ArtisanSearch.clear();
		}
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		boolean hasStorage = isStorageMenu(screen);

		if (ArtisanSearch.available()) {
			add(widgets, "screen.pbenchants.button.search",
				(graphics, font, x, y, color) -> ArtisanIcons.magnifier(graphics, x, y, color),
				() -> toggleSearch(screen));
		}
		// One Sort button, not two, and the screen decides which one it is: a
		// chest on screen means the chest, anything else means your backpack.
		//
		// Which is why a chest gets no button at all from Sorter's Hand I. The
		// rank that sorts containers is II, and offering to tidy your backpack
		// instead would be answering a question nobody asked — you opened a
		// chest. The button appears when you close it.
		if (hasStorage) {
			if (ClientArtisanState.owns("sorters_hand_2")) {
				add(widgets, "screen.pbenchants.button.sort_chest",
					(graphics, font, x, y, color) -> ArtisanIcons.letterOnShelf(graphics, font, "S", x, y, color),
					() -> send(ArtisanActionPayload.Action.SORT_CONTAINER));
			}
		} else if (ClientArtisanState.owns("sorters_hand_1")) {
			add(widgets, "screen.pbenchants.button.sort",
				(graphics, font, x, y, color) -> ArtisanIcons.letter(graphics, font, "S", x, y, color),
				() -> send(ArtisanActionPayload.Action.SORT_INVENTORY));
		}
		if (ClientArtisanState.owns("sort_profiles")) {
			shownMode = ClientArtisanState.sortMode();
			orderButton = add(widgets, orderLabel(shownMode), ArtisanScreenHooks::drawOrderSymbol,
				() -> send(ArtisanActionPayload.Action.CYCLE_SORT_MODE));
		}
		if (ClientArtisanState.owns("hand_of_order")) {
			add(widgets, "screen.pbenchants.button.quick_stack",
				(graphics, font, x, y, color) -> ArtisanIcons.outbound(graphics, x, y, color),
				() -> send(ArtisanActionPayload.Action.QUICK_STACK));
		}
		if (ClientArtisanState.owns("restock_nearby")) {
			add(widgets, "screen.pbenchants.button.restock",
				(graphics, font, x, y, color) -> ArtisanIcons.inbound(graphics, x, y, color),
				() -> send(ArtisanActionPayload.Action.RESTOCK));
		}
		if (ArtisanSearch.available()) {
			attachSearch(screen, widgets);
		}
	}

	private static ArtisanIconButton add(List<AbstractWidget> widgets, String key,
			ArtisanIconButton.Symbol symbol, Runnable onPress) {
		return add(widgets, Component.translatable(key), symbol, onPress);
	}

	private static ArtisanIconButton add(List<AbstractWidget> widgets, Component name,
			ArtisanIconButton.Symbol symbol, Runnable onPress) {
		ArtisanIconButton button = new ArtisanIconButton(0, 0, name, symbol, onPress);
		BUTTONS.add(button);
		widgets.add(button);
		return button;
	}

	/**
	 * Puts the row in the window's top-right corner, just outside the frame. The
	 * corner inside belongs to the container's own title; the strip outside
	 * belongs to nobody, and the recipe book — which only ever opens to the
	 * left — never reaches it.
	 *
	 * <p>A window tall enough to leave no room above it gets the row underneath
	 * instead, which beats drawing it off the top of the display.
	 */
	private static void layout(AbstractContainerScreen<?> screen) {
		if (BUTTONS.isEmpty()) {
			return;
		}
		ContainerScreenAccessor geometry = (ContainerScreenAccessor) screen;
		int right = geometry.pbenchants$leftPos() + geometry.pbenchants$imageWidth();
		int above = geometry.pbenchants$topPos() - ArtisanIconButton.SIZE - MARGIN;
		int y = above >= MARGIN
			? above
			: geometry.pbenchants$topPos() + geometry.pbenchants$imageHeight() + MARGIN;

		int x = right - ArtisanIconButton.SIZE;
		for (ArtisanIconButton button : BUTTONS) {
			button.setPosition(x, y);
			x -= ArtisanIconButton.SIZE + GAP;
		}

		if (orderButton != null) {
			SortMode mode = ClientArtisanState.sortMode();
			if (mode != shownMode) {
				shownMode = mode;
				orderButton.rename(orderLabel(mode));
			}
		}
		if (search != null) {
			boolean showing = ArtisanSearch.isOpen() || !ArtisanSearch.query().isEmpty();
			search.visible = showing;
			search.active = showing;
			// x now sits one slot to the left of the leftmost button: the field
			// ends where that button begins and grows away from the corner.
			search.setPosition(x + ArtisanIconButton.SIZE - SEARCH_WIDTH, y);
		}
	}

	private static void attachSearch(AbstractContainerScreen<?> screen, List<AbstractWidget> widgets) {
		EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, SEARCH_WIDTH, ArtisanIconButton.SIZE,
			Component.translatable("screen.pbenchants.search"));
		box.setMaxLength(ArtisanSearch.MAX_QUERY);
		box.setHint(Component.translatable("screen.pbenchants.search.hint"));
		box.setValue(ArtisanSearch.query());
		box.setResponder(ArtisanSearch::setQuery);
		box.visible = false;
		box.active = false;
		widgets.add(box);
		search = box;
	}

	/** The magnifier is a switch: it shows the field, hands it the caret, and clears it again. */
	private static void toggleSearch(AbstractContainerScreen<?> screen) {
		boolean opening = !ArtisanSearch.isOpen();
		ArtisanSearch.setOpen(opening);
		if (search == null) {
			return;
		}
		if (opening) {
			search.visible = true;
			search.active = true;
			search.setFocused(true);
			screen.setFocused(search);
		} else {
			search.setValue("");
			search.setFocused(false);
			screen.setFocused(null);
		}
	}

	/** Artisan's Order wears the order it is set to, so one glance says which one that is. */
	private static void drawOrderSymbol(GuiGraphicsExtractor graphics, Font font, int x, int y, int color) {
		switch (ClientArtisanState.sortMode()) {
			case CATEGORY -> ArtisanIcons.byCategory(graphics, x, y, color);
			case NAME -> ArtisanIcons.letter(graphics, font, "A", x, y, color);
			case COUNT -> ArtisanIcons.byCount(graphics, x, y, color);
		}
	}

	private static Component orderLabel(SortMode mode) {
		return Component.translatable("screen.pbenchants.button.order", mode.label());
	}

	private static void forget() {
		BUTTONS.clear();
		orderButton = null;
		search = null;
		shownMode = null;
	}

	// ---------- overlays ----------

	/**
	 * Everything the mod paints onto a container: the yellow behind the slots
	 * Seeker's Eye matched, and the gold corner on the pinned ones.
	 */
	private static void drawOverlay(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float delta) {
		if (!(screen instanceof AbstractContainerScreen<?> container)) {
			return;
		}
		boolean searching = ArtisanSearch.available() && !ArtisanSearch.query().isEmpty();
		boolean pinning = ClientArtisanState.owns("slot_lock");
		if (!searching && !pinning) {
			return;
		}
		int left = ((ContainerScreenAccessor) container).pbenchants$leftPos();
		int top = ((ContainerScreenAccessor) container).pbenchants$topPos();

		for (Slot slot : container.getMenu().slots) {
			if (searching && ArtisanSearch.matches(slot)) {
				graphics.fill(left + slot.x, top + slot.y, left + slot.x + 16, top + slot.y + 16, MATCH_FILL);
				graphics.outline(left + slot.x, top + slot.y, 16, 16, SkillTreeStyle.GOLD);
			}
			if (pinning && slot.container instanceof Inventory
				&& ClientArtisanState.slotLocked(slot.getContainerSlot())) {
				graphics.fill(left + slot.x, top + slot.y, left + slot.x + 4, top + slot.y + 4,
					SkillTreeStyle.GOLD);
			}
		}
	}

	// ---------- pinned slots ----------

	/**
	 * Alt-click pins the slot under the cursor. Alt is free in vanilla, it needs
	 * no keybind to explain, and holding it makes the intent unambiguous — a
	 * plain click on a pinned slot still picks the item up, because pinning is
	 * about what the <em>mod</em> may move, not about what you may.
	 */
	private static boolean pinClicked(AbstractContainerScreen<?> screen, MouseButtonEvent event) {
		if (!event.hasAltDown() || !ClientArtisanState.owns("slot_lock")) {
			return false;
		}
		Slot slot = slotAt(screen, event.x(), event.y());
		if (slot == null || !(slot.container instanceof Inventory)) {
			return false;
		}
		ClientPlayNetworking.send(ArtisanActionPayload.lock(slot.getContainerSlot()));
		return true;
	}

	@Nullable
	private static Slot slotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		int left = ((ContainerScreenAccessor) screen).pbenchants$leftPos();
		int top = ((ContainerScreenAccessor) screen).pbenchants$topPos();
		for (Slot slot : screen.getMenu().slots) {
			double x = mouseX - (left + slot.x);
			double y = mouseY - (top + slot.y);
			if (x >= 0 && x < 16 && y >= 0 && y < 16) {
				return slot;
			}
		}
		return null;
	}

	// ---------- plumbing ----------

	static boolean isStorageMenu(AbstractContainerScreen<?> screen) {
		return screen.getMenu() instanceof ChestMenu || screen.getMenu() instanceof ShulkerBoxMenu;
	}

	static void send(ArtisanActionPayload.Action action) {
		ClientPlayNetworking.send(ArtisanActionPayload.of(action));
	}
}
