package dev.pbenchants.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.pbenchants.enchant.EnchanterPerks;
import dev.pbenchants.network.EnchantPreviewPayload;
import dev.pbenchants.network.SkillActionPayload;
import dev.pbenchants.network.SkillStatePayload;
import dev.pbenchants.perk.PerkAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PBEnchantsClient implements ClientModInitializer {
	public static final KeyMapping OPEN_TREE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.pbenchants.open_tree",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_K,
		KeyMapping.Category.MISC
	));

	/**
	 * Ticks to wait after joining before the "press K" line goes into chat.
	 * Sent late on purpose: a message posted on the join tick lands under the
	 * server's own MOTD, the resource-pack notice and whatever else the world
	 * has to say, and is scrolled away before the player can read it.
	 */
	private static final int HINT_DELAY_TICKS = 60;
	private static int hintCountdown = -1;

	/**
	 * Set when the tree screen closes itself on the tree key. The screen sees
	 * the press first and gets out of the way; without this flag the same press
	 * could still be sitting in the key mapping's click queue when the tick
	 * handler below runs, and the screen would spring straight back open.
	 */
	private static boolean closedByKey;

	/** Called by the screen as it closes, so this tick's press is not read twice. */
	public static void treeClosedByKey() {
		closedByKey = true;
	}

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SkillStatePayload.TYPE, (payload, context) -> {
			ClientSkillState.accept(payload);
			// Judged off the snapshot rather than announced by the server, so a
			// tier chime and a pinned-goal chime can never both fire for the
			// same event.
			ProgressChimes.accept(payload);
		});
		// Action verdicts land where the click happened: a banner in the skill
		// screen. Chat only catches a reply that outlived the screen.
		ClientPlayNetworking.registerGlobalReceiver(dev.pbenchants.network.SkillFeedbackPayload.TYPE,
			(payload, context) -> {
				if (context.client().gui.screen() instanceof SkillTreeScreen screen) {
					screen.showFeedback(payload.ok(), payload.message());
				} else if (context.player() != null) {
					context.player().sendSystemMessage(payload.message().copy()
						.withStyle(payload.ok()
							? net.minecraft.ChatFormatting.GREEN
							: net.minecraft.ChatFormatting.RED));
				}
			});
		ClientPlayNetworking.registerGlobalReceiver(EnchantPreviewPayload.TYPE, (payload, context) ->
			EnchantPreviewState.accept(payload));

		// Set Sense draws next to the armour bar it explains.
		SetSenseHud.register();
		// Quiver Sense mirrors it on the hotbar's other side: the arrow the
		// bow will actually fire, and how many of it you carry.
		QuiverSenseHud.register();
		// The pinned-goal scoreboard on the right edge, fed by the Track button.
		GoalTrackerHud.register();

		// Lets common code (enchanting menu logic) check enchanter perk
		// ownership on the client via the synced skill state.
		EnchanterPerks.clientNodeChecker = nodeId -> {
			SkillStatePayload.TreeState state = ClientSkillState.tree("enchanter");
			return state != null && state.purchased().contains(nodeId);
		};

		// A new world is a new player as far as the hint is concerned: the mod
		// is only useful once someone knows the key exists.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> hintCountdown = HINT_DELAY_TICKS);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientSkillState.clear();
			ArtisanSearch.clear();
			GoalTracker.clear();
			ProgressChimes.clear();
			hintCountdown = -1;
		});

		// Why a borrowed tool feels dead, and why a librarian will not sell a
		// book yet — both read the same per-holder check as the gameplay hooks.
		LockedItemTooltip.register();

		// The Artisan class lives in the inventory screen rather than the skill
		// screen: its row of icon buttons, the Seeker's Eye search field and the
		// pinned-slot markers attach to whatever container the player opens.
		ArtisanScreenHooks.register();

		// Speed passives are computed on both sides; on this one the answer comes
		// from the synced snapshot, and only ever for the local player.
		PerkAccess.setClientLookup((player, treeId, nodeId) ->
			player == Minecraft.getInstance().player && ClientSkillState.owns(treeId, nodeId));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			tickJoinHint(client);

			boolean handled = false;
			while (OPEN_TREE_KEY.consumeClick()) {
				// One press, one answer: a second queued click in the same tick
				// would open the screen the first one just closed.
				if (handled || closedByKey || client.player == null) {
					continue;
				}
				handled = true;
				if (client.gui.screen() instanceof SkillTreeScreen) {
					// Belt and braces: the screen normally closes itself on this
					// key, but if anything ever swallows the press before it
					// gets there, the mapping still toggles the screen.
					client.gui.setScreen(null);
				} else if (client.gui.screen() == null) {
					ClientPlayNetworking.send(SkillActionPayload.requestState());
					client.setScreenAndShow(new SkillTreeScreen());
				}
			}
			closedByKey = false;
		});
	}

	/**
	 * The one line of onboarding the mod gets: what key opens the trees, named
	 * as the player has it bound and written in the language their client is
	 * set to — the component is built here, on the client, so both are true
	 * without the server knowing either.
	 *
	 * <p>Pressing that key is also the tab's root advancement, so this line and
	 * the advancement toast are two halves of the same first step.
	 */
	private static void tickJoinHint(Minecraft client) {
		// The clock only runs once there is a world to say it into, so a slow
		// load pushes the line back rather than eating it.
		if (hintCountdown < 0 || client.player == null) {
			return;
		}
		if (--hintCountdown > 0) {
			return;
		}
		hintCountdown = -1;
		// Through the chat listener rather than the player: on this side
		// Player.sendSystemMessage is an empty method, and the line would go
		// nowhere at all.
		client.gui.chatListener().handleSystemMessage(
			Component.translatable("msg.pbenchants.welcome",
					OPEN_TREE_KEY.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GOLD))
				.withStyle(ChatFormatting.YELLOW),
			false);
	}
}
