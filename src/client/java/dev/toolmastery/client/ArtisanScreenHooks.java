package dev.toolmastery.client;

import dev.toolmastery.client.gui.SkillTreeStyle;
import dev.toolmastery.client.gui.StorageResultWidget;
import dev.toolmastery.client.mixin.ContainerScreenAccessor;
import dev.toolmastery.network.ArtisanActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
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

import java.util.List;

/**
 * Where the Artisan class actually lives: a column of buttons down the left of
 * the inventory screen and every container screen.
 *
 * <p>The mod always meant to have an inventory button as a discovery route and
 * never built one; this class finally delivers it, and Sort, Quick Stack,
 * Restock and Seeker's Eye all hang off the same anchor.
 *
 * <p>Every control here is added only when the node behind it is unlocked, so a
 * player who has not touched the tree sees precisely nothing new. The anchor is
 * on the <em>outside</em> of the window on purpose: the inside is where
 * Inventory Profiles Next, Quark and Sophisticated Storage all put theirs.
 */
public final class ArtisanScreenHooks {
	private static final int BUTTON_WIDTH = 58;
	private static final int BUTTON_HEIGHT = 16;
	private static final int GAP = 2;
	private static final int PANEL_WIDTH = 150;

	/** Search text survives a screen rebuild so a resize does not wipe the query. */
	private static String lastQuery = "";

	private ArtisanScreenHooks() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container)
				|| screen instanceof CreativeModeInventoryScreen) {
				return;
			}
			attach(container);
			ScreenEvents.afterForeground(screen).register(ArtisanScreenHooks::drawPins);
			ScreenMouseEvents.allowMouseClick(screen).register((clicked, event) ->
				!pinClicked(container, event));
		});
	}

	// ---------- the button column ----------

	private static void attach(AbstractContainerScreen<?> screen) {
		int left = ((ContainerScreenAccessor) screen).toolmastery$leftPos() - BUTTON_WIDTH - 4;
		int y = ((ContainerScreenAccessor) screen).toolmastery$topPos() + 4;
		boolean hasStorage = isStorageMenu(screen);
		List<AbstractWidget> widgets = Screens.getWidgets(screen);

		if (ClientArtisanState.owns("sorters_hand_1")) {
			widgets.add(button(left, y, "screen.toolmastery.button.sort",
				() -> send(ArtisanActionPayload.of(ArtisanActionPayload.Action.SORT_INVENTORY))));
			y += BUTTON_HEIGHT + GAP;
		}
		if (hasStorage && ClientArtisanState.owns("sorters_hand_2")) {
			widgets.add(button(left, y, "screen.toolmastery.button.sort_chest",
				() -> send(ArtisanActionPayload.of(ArtisanActionPayload.Action.SORT_CONTAINER))));
			y += BUTTON_HEIGHT + GAP;
		}
		if (ClientArtisanState.owns("sort_profiles")) {
			Button order = Button.builder(
					Component.translatable("screen.toolmastery.button.order", ClientArtisanState.sortMode().label()),
					widget -> send(ArtisanActionPayload.of(ArtisanActionPayload.Action.CYCLE_SORT_MODE)))
				.bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build();
			widgets.add(order);
			y += BUTTON_HEIGHT + GAP;
		}
		if (ClientArtisanState.owns("hand_of_order")) {
			widgets.add(button(left, y, "screen.toolmastery.button.quick_stack",
				() -> send(ArtisanActionPayload.of(ArtisanActionPayload.Action.QUICK_STACK))));
			y += BUTTON_HEIGHT + GAP;
		}
		if (ClientArtisanState.owns("restock_nearby")) {
			widgets.add(button(left, y, "screen.toolmastery.button.restock",
				() -> send(ArtisanActionPayload.of(ArtisanActionPayload.Action.RESTOCK))));
			y += BUTTON_HEIGHT + GAP;
		}
		if (ClientArtisanState.owns("storage_ledger")) {
			widgets.add(button(left, y, "screen.toolmastery.button.ledger", () -> {
				send(ArtisanActionPayload.search(""));
				net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new LedgerScreen(screen));
			}));
			y += BUTTON_HEIGHT + GAP;
		}
		if (ClientArtisanState.owns("chest_search_1")) {
			attachSearch(screen, widgets, left, y);
		}
	}

	private static void attachSearch(AbstractContainerScreen<?> screen, List<AbstractWidget> widgets,
			int left, int y) {
		EditBox search = new EditBox(Screens.getFont(screen), left, y, BUTTON_WIDTH, BUTTON_HEIGHT,
			Component.translatable("screen.toolmastery.search"));
		search.setMaxLength(ArtisanActionPayload.MAX_QUERY);
		search.setHint(Component.translatable("screen.toolmastery.search.hint"));
		search.setValue(lastQuery);
		search.setResponder(query -> {
			lastQuery = query;
			send(ArtisanActionPayload.search(query));
		});
		widgets.add(search);

		// Just outside the right edge of the window, pulled back on screen when
		// the window is wide enough to leave no room there.
		ContainerScreenAccessor geometry = (ContainerScreenAccessor) screen;
		int panelX = geometry.toolmastery$leftPos() + geometry.toolmastery$imageWidth() + 4;
		widgets.add(new StorageResultWidget(
			Math.min(panelX, Math.max(0, screen.width - PANEL_WIDTH - 4)),
			geometry.toolmastery$topPos(),
			PANEL_WIDTH, screen.height - 40, ClientArtisanState::searchResults));

		if (!lastQuery.isEmpty()) {
			send(ArtisanActionPayload.search(lastQuery));
		}
	}

	private static Button button(int x, int y, String key, Runnable onPress) {
		return Button.builder(Component.translatable(key), widget -> onPress.run())
			.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build();
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
		send(ArtisanActionPayload.lock(slot.getContainerSlot()));
		return true;
	}

	/** A gold corner on every pinned slot — small enough not to hide the item. */
	private static void drawPins(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (!(screen instanceof AbstractContainerScreen<?> container) || !ClientArtisanState.owns("slot_lock")) {
			return;
		}
		int left = ((ContainerScreenAccessor) container).toolmastery$leftPos();
		int top = ((ContainerScreenAccessor) container).toolmastery$topPos();
		for (Slot slot : container.getMenu().slots) {
			if (slot.container instanceof Inventory && ClientArtisanState.slotLocked(slot.getContainerSlot())) {
				graphics.fill(left + slot.x, top + slot.y, left + slot.x + 4, top + slot.y + 4,
					SkillTreeStyle.GOLD);
			}
		}
	}

	@Nullable
	private static Slot slotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		int left = ((ContainerScreenAccessor) screen).toolmastery$leftPos();
		int top = ((ContainerScreenAccessor) screen).toolmastery$topPos();
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

	static void send(ArtisanActionPayload payload) {
		ClientPlayNetworking.send(payload);
	}
}
