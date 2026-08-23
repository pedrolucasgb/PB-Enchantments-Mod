package dev.toolmastery;

import dev.toolmastery.advancement.ModAdvancements;
import dev.toolmastery.command.MasteryCommand;
import dev.toolmastery.perk.AreaBreak;
import dev.toolmastery.perk.TimberScheduler;
import dev.toolmastery.progress.ModAttachments;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import dev.toolmastery.track.BlockBreakTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolMastery implements ModInitializer {
	public static final String MOD_ID = "toolmastery";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private int slowTickCounter = 0;

	@Override
	public void onInitialize() {
		ModAttachments.init();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			MasteryCommand.register(dispatcher));

		dev.toolmastery.network.ModNetworking.init();

		// Tier advancements are the visible face of progress: catch up saves made
		// before they shipped, and anything cleared with /advancement revoke.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			ModAdvancements.syncAll(handler.getPlayer()));

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			BlockBreakTracker.onBreak(level, player, pos, state);
			dev.toolmastery.perk.MeltHandler.onBreak(level, player, pos, state);
			dev.toolmastery.perk.VeinMiner.onBreak(level, player, pos, state);
			AreaBreak.onBreak(level, player, pos, state);
			TimberScheduler.onBreak(level, player, pos, state);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			dev.toolmastery.perk.MeltHandler.tick(server);
			TimberScheduler.tick(server);

			// Slow checks (once a second): position-based gates.
			if (++slowTickCounter >= 20) {
				slowTickCounter = 0;
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					TreeProgress pickaxe = SkillService.progress(player, SkillTrees.PICKAXE);
					if (pickaxe.count("reach_y0") < 1 && player.getY() <= 0) {
						pickaxe.counters.put("reach_y0", 1);
					}
					dev.toolmastery.track.FarmingTracker.scanSaplingChecklist(player);
				}
			}
		});

		LOGGER.info("Tool Mastery initialized — sharpen your skills.");
	}
}
