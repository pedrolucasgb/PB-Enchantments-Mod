package dev.toolmastery.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.toolmastery.enchant.EnchanterPerks;
import dev.toolmastery.network.EnchantPreviewPayload;
import dev.toolmastery.network.SkillActionPayload;
import dev.toolmastery.network.SkillStatePayload;
import dev.toolmastery.perk.PerkAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ToolMasteryClient implements ClientModInitializer {
	public static final KeyMapping OPEN_TREE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.toolmastery.open_tree",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_K,
		KeyMapping.Category.MISC
	));

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SkillStatePayload.TYPE, (payload, context) ->
			ClientSkillState.accept(payload));
		// Action verdicts land where the click happened: a banner in the skill
		// screen. Chat only catches a reply that outlived the screen.
		ClientPlayNetworking.registerGlobalReceiver(dev.toolmastery.network.SkillFeedbackPayload.TYPE,
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

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientSkillState.clear();
			ArtisanSearch.clear();
			GoalTracker.clear();
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
			while (OPEN_TREE_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(SkillActionPayload.requestState());
					client.setScreenAndShow(new SkillTreeScreen());
				}
			}
		});
	}
}
