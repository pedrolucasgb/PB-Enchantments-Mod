package dev.toolmastery;

import dev.toolmastery.advancement.ModAdvancements;
import dev.toolmastery.command.MasteryCommand;
import dev.toolmastery.perk.AreaBreak;
import dev.toolmastery.perk.AxeHarvest;
import dev.toolmastery.perk.DeepHaste;
import dev.toolmastery.perk.ItemAuthority;
import dev.toolmastery.perk.MinersMagnet;
import dev.toolmastery.perk.Remember;
import dev.toolmastery.perk.TimberScheduler;
import dev.toolmastery.perk.Trailblazer;
import dev.toolmastery.perk.Waypoints;
import dev.toolmastery.progress.ModAttachments;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import dev.toolmastery.storage.DeftHands;
import dev.toolmastery.storage.SteadyGrid;
import dev.toolmastery.track.BiomeTracker;
import dev.toolmastery.track.BlockBreakTracker;
import dev.toolmastery.track.MovementTracker;
import dev.toolmastery.track.StorageTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
		// (the skill snapshot the speed passives need on join is pushed by
		// ModNetworking.init above.)
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			ModAdvancements.syncAll(handler.getPlayer()));

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			// Before anything reads the tool: if the holder has not earned what
			// it carries, none of the perks below will fire and the block just
			// gave up no drops. Say so, or it reads as a bug.
			ItemAuthority.noticeInertUse(player, player.getMainHandItem());
			BlockBreakTracker.onBreak(level, player, pos, state);
			dev.toolmastery.perk.SmeltHandler.onBreak(level, player, pos, state);
			dev.toolmastery.perk.VeinMiner.onBreak(level, player, pos, state);
			MinersMagnet.onBreak(level, player, pos, state);
			AreaBreak.onBreak(level, player, pos, state);
			TimberScheduler.onBreak(level, player, pos, state);
			AxeHarvest.onBreak(level, player, pos, state);
		});

		// Explorer: the compass is the class's tool. Sneaking binds a waypoint,
		// standing up asks the world where the nearest known structure is.
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& Waypoints.onCompassUse(serverPlayer, hand)) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		// Remember: the coordinates are taken where the player fell and handed
		// over once they are back on their feet.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				Remember.onDeath(player);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Remember.onRespawn(newPlayer));

		// Anything held per player outside the save goes back where it belongs
		// when they leave: a sprint ramp is worth nothing, a stashed crafting
		// grid is worth items.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			Trailblazer.forget(player);
			DeftHands.forget(player);
			StorageTracker.forget(player);
			SteadyGrid.release(player);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			dev.toolmastery.perk.SmeltHandler.tick(server);
			// After Smelt, so the magnet pockets the smelted result rather than the raw ore.
			MinersMagnet.tick(server);
			// Last: it collects the drops the break events above have just spawned.
			AxeHarvest.tick(server);

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				// Every tick, because both react to a state that changes within
				// one: a sprint that just broke, a hotbar stack that just ran out.
				Trailblazer.tick(player);
				DeftHands.tick(player);
			}

			// Slow checks (once a second): position-based gates.
			if (++slowTickCounter >= 20) {
				slowTickCounter = 0;
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					TreeProgress pickaxe = SkillService.progress(player, SkillTrees.PICKAXE);
					if (pickaxe.count("reach_y0") < 1 && player.getY() <= 0) {
						pickaxe.counters.put("reach_y0", 1);
					}
					TreeProgress enchanter = SkillService.progress(player, SkillTrees.ENCHANTER);
					if (enchanter.count("reach_level_30") < 1 && player.experienceLevel >= 30) {
						enchanter.counters.put("reach_level_30", 1);
					}
					dev.toolmastery.track.FarmingTracker.scanSaplingChecklist(player);
					dev.toolmastery.track.EnchantTracker.scanBookChecklist(player);
					DeepHaste.tick(player);
					// Explorer: distance off the vanilla statistics, places off
					// where the player is standing.
					MovementTracker.tick(player);
					BiomeTracker.tick(player);
				}
			}
		});

		LOGGER.info("Tool Mastery initialized — sharpen your skills.");
	}
}
