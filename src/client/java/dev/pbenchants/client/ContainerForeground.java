package dev.pbenchants.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.21.1 stand-in for Fabric's {@code ScreenEvents.afterForeground}, which this
 * version of the screen API does not have: a per-screen hook that runs inside
 * {@code AbstractContainerScreen.render} after the slots and their items are
 * drawn and before the tooltip, in screen coordinates.
 *
 * <p>Fired by {@code ContainerScreenForegroundMixin}. Registrations are per
 * screen and are dropped whenever the screen re-initialises (a window resize),
 * exactly like Fabric's own per-screen events, so an AFTER_INIT handler can
 * register on every init without stacking duplicates.
 */
public final class ContainerForeground {
	@FunctionalInterface
	public interface Listener {
		void draw(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float delta);
	}

	private static final Map<Screen, List<Listener>> LISTENERS = new WeakHashMap<>();

	static {
		ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> LISTENERS.remove(screen));
	}

	private ContainerForeground() {
	}

	public static void register(Screen screen, Listener listener) {
		LISTENERS.computeIfAbsent(screen, key -> new ArrayList<>()).add(listener);
	}

	public static void fire(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		List<Listener> listeners = LISTENERS.get(screen);
		if (listeners == null) {
			return;
		}
		for (Listener listener : List.copyOf(listeners)) {
			listener.draw(screen, graphics, mouseX, mouseY, delta);
		}
	}
}
