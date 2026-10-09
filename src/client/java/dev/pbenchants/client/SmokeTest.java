package dev.pbenchants.client;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.TreeSwitch;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Compatibility testing only ({@code -Dpbenchants.smokeTest=true}, used by the
 * production client run): once a world is joined, opens the skill screen and
 * the inventory, saves a screenshot of each under {@code screenshots/}, logs
 * what the server said about disabled trees, and quits. Lets a modpack boot be
 * checked end to end — sync payload, screen, container overlay — with nobody
 * at the keyboard.
 */
final class SmokeTest {
	private static int ticks = -1;

	private SmokeTest() {
	}

	static void register() {
		if (!Boolean.getBoolean("pbenchants.smokeTest")) {
			return;
		}
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ticks = 0);
		ClientTickEvents.END_CLIENT_TICK.register(SmokeTest::tick);
	}

	private static void tick(Minecraft client) {
		if (ticks < 0 || client.player == null) {
			return;
		}
		ticks++;
		switch (ticks) {
			case 100 -> {
				PBEnchants.LOGGER.info("PB smoke: server-disabled trees = {}; tabs shown = {}",
					TreeSwitch.disabledIds(),
					TreeSwitch.enabledTrees().stream().map(SkillTree::id).toList());
				client.setScreen(new SkillTreeScreen());
			}
			case 140 -> shot(client, "pb_smoke_tree.png");
			case 150 -> client.setScreen(new InventoryScreen(client.player));
			case 190 -> shot(client, "pb_smoke_inventory.png");
			case 200 -> {
				client.setScreen(null);
				PBEnchants.LOGGER.info("PB smoke: done, quitting");
				client.stop();
			}
			default -> {
			}
		}
	}

	private static void shot(Minecraft client, String name) {
		Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(),
			message -> PBEnchants.LOGGER.info("PB smoke: {}", message.getString()));
	}
}
