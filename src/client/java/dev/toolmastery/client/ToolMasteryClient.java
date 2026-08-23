package dev.toolmastery.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.toolmastery.network.SkillActionPayload;
import dev.toolmastery.network.SkillStatePayload;
import dev.toolmastery.skill.NodeOwnership;
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

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientSkillState.clear());

		// Speed passives are computed on both sides; on this one the answer comes
		// from the synced snapshot, and only ever for the local player.
		NodeOwnership.setClientView((player, treeId, nodeId) ->
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
