package dev.pbenchants.client;

import dev.pbenchants.client.mixin.ContainerScreenAccessor;
import dev.pbenchants.enchant.AnvilDisenchant;
import dev.pbenchants.network.AnvilModePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import org.jetbrains.annotations.Nullable;

/**
 * The enchant/disenchant toggle on the anvil screen (0.10.0).
 *
 * <p>The button lives just outside the window's right edge, below the Artisan
 * corner row, and it does not exist until it means something: it appears the
 * frame the sacrifice slot holds a book carrying exactly one enchantment at
 * exactly the rank the base item has ({@link AnvilDisenchant#match}), and
 * leaves — resetting the mode to enchant, silently — the frame that stops
 * being true. Enchant is always the default; every fresh anvil opens there.
 *
 * <p>The press flips a local flag for the label and tells the server, which
 * re-derives the match on its own before honouring anything.
 */
public final class AnvilModeHooks {
	@Nullable
	private static Button toggle;
	private static boolean disenchanting;

	private AnvilModeHooks() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AnvilScreen anvil)) {
				return;
			}
			disenchanting = false;
			Button button = Button.builder(label(), pressed -> {
				disenchanting = !disenchanting;
				pressed.setMessage(label());
				pressed.setTooltip(tooltip());
				ClientPlayNetworking.send(new AnvilModePayload(disenchanting));
			}).bounds(0, 0, 74, 14).build();
			button.setTooltip(tooltip());
			button.visible = false;
			toggle = button;
			Screens.getWidgets(screen).add(button);
			ScreenEvents.beforeExtract(screen).register((self, graphics, mouseX, mouseY, delta) ->
				layout(anvil));
			ScreenEvents.remove(screen).register(self -> {
				toggle = null;
				disenchanting = false;
			});
		});
	}

	/**
	 * Every frame: follow the window (its origin moves without init re-running)
	 * and decide whether the toggle currently means anything.
	 */
	private static void layout(AnvilScreen screen) {
		Button button = toggle;
		if (button == null) {
			return;
		}
		ContainerScreenAccessor accessor = (ContainerScreenAccessor) screen;
		button.setX(accessor.pbenchants$leftPos() + accessor.pbenchants$imageWidth() + 3);
		button.setY(accessor.pbenchants$topPos() + 40);

		AnvilMenu menu = screen.getMenu();
		boolean matches = AnvilDisenchant.match(
			menu.getSlot(AnvilMenu.INPUT_SLOT).getItem(),
			menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem()) != null;
		if (!matches && disenchanting) {
			// The pair broke while the mode was armed: back to enchant, and the
			// server is told so its next createResult agrees with the label.
			disenchanting = false;
			button.setMessage(label());
			button.setTooltip(tooltip());
			ClientPlayNetworking.send(new AnvilModePayload(false));
		}
		button.visible = matches;
	}

	private static Component label() {
		return Component.translatable(disenchanting
			? "screen.pbenchants.anvil.disenchant"
			: "screen.pbenchants.anvil.enchant");
	}

	private static Tooltip tooltip() {
		return Tooltip.create(Component.translatable(disenchanting
			? "screen.pbenchants.anvil.disenchant.tip"
			: "screen.pbenchants.anvil.enchant.tip"));
	}
}
