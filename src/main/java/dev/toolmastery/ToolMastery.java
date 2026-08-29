package dev.toolmastery;

import dev.toolmastery.advancement.ModAdvancements;
import dev.toolmastery.command.MasteryCommand;
import dev.toolmastery.perk.AquaLungs;
import dev.toolmastery.perk.ArrowOrigin;
import dev.toolmastery.perk.BowPerks;
import dev.toolmastery.perk.ArmorUpkeep;
import dev.toolmastery.perk.AreaBreak;
import dev.toolmastery.perk.CombatDrops;
import dev.toolmastery.perk.CombatPerks;
import dev.toolmastery.perk.AxeHarvest;
import dev.toolmastery.perk.DeepHaste;
import dev.toolmastery.perk.Flashpoint;
import dev.toolmastery.perk.ItemAuthority;
import dev.toolmastery.perk.MinersMagnet;
import dev.toolmastery.perk.Remember;
import dev.toolmastery.perk.TimberScheduler;
import dev.toolmastery.perk.Trailblazer;
import dev.toolmastery.perk.Waypoints;
import dev.toolmastery.perk.ToolMasteryConfig;
import dev.toolmastery.progress.ModAttachments;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import dev.toolmastery.storage.DeftHands;
import dev.toolmastery.storage.SteadyGrid;
import dev.toolmastery.track.ArmorTracker;
import dev.toolmastery.track.BiomeTracker;
import dev.toolmastery.track.BlockBreakTracker;
import dev.toolmastery.track.BowTracker;
import dev.toolmastery.track.CombatTracker;
import dev.toolmastery.track.MovementTracker;
import dev.toolmastery.track.StorageTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
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
		// The sword tree is the first part of the mod whose balance depends on
		// what kind of server it is running on. Two switches, read once.
		ToolMasteryConfig.load();

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

		// Indestructible: a spent item is inert, and that has to include the
		// right click — a bow that still draws, a crossbow that still loads and
		// a hoe that still tills would make "spent" mean nothing for half the
		// tools in the game. Registered before every other use handler and on
		// both sides, so no ghost animation starts and no perk sees the click.
		// The mace's right hook needs nothing here: it has no use action, and
		// its damage path is judged by the attack hooks like any weapon.
		UseItemCallback.EVENT.register((player, level, hand) ->
			dev.toolmastery.perk.Indestructible.vetoUse(player, player.getItemInHand(hand))
				? InteractionResult.FAIL
				: InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
			dev.toolmastery.perk.Indestructible.vetoUse(player, player.getItemInHand(hand))
				? InteractionResult.FAIL
				: InteractionResult.PASS);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
			dev.toolmastery.perk.Indestructible.vetoUse(player, player.getItemInHand(hand))
				? InteractionResult.FAIL
				: InteractionResult.PASS);

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
			if (source.getEntity() instanceof ServerPlayer killer && killer != entity
				&& entity instanceof LivingEntity victim) {
				if (!(entity instanceof ServerPlayer)) {
					// Armor: the tier 3 gate only wants the mobs you killed while hurt.
					ArmorTracker.onMobKill(killer);
				}
				// Sword: a kill is the class's block-break. The tracker runs
				// first, while the crit that ended it is still the last one
				// remembered.
				CombatTracker.onKill(killer, victim);
				CombatPerks.onKill(killer, victim);
				CombatPerks.warlordsWake(killer, victim);
				CombatDrops.onKill(killer, victim);
				// Bow: an arrow kill is judged by the arrow that landed it —
				// the weapon and the launch point both travel on the entity.
				if (source.getDirectEntity() instanceof AbstractArrow arrow) {
					BowTracker.onKill(killer, victim, arrow, ((ArrowOrigin) arrow).toolmastery$origin());
					BowPerks.ricochet(killer, victim, arrow);
				}
				BowPerks.featherBounty(killer, victim);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Remember.onRespawn(newPlayer));

		// Armor levels from what happens to you, so both halves of a hit are
		// read: ALLOW_DAMAGE is where Flashpoint drops the lava, AFTER_DAMAGE
		// is where the difference between raw and applied becomes the gate.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Flashpoint::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			ArmorTracker.onDamage(entity, source, base, taken, blocked);
			if (entity instanceof ServerPlayer hurt) {
				// Repair Rites wants calm, and any hit at all is not calm.
				ArmorUpkeep.onDamaged(hurt);
			}
		});
		// Immortal Line is the only thing in the mod that answers a death, so it
		// is the only listener here: returning false calls the death off.
		ServerLivingEntityEvents.ALLOW_DEATH.register(ArmorUpkeep::allowDeath);

		// Anything held per player outside the save goes back where it belongs
		// when they leave: a sprint ramp is worth nothing, a stashed crafting
		// grid is worth items.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			Trailblazer.forget(player);
			CombatPerks.forget(player);
			BowPerks.forget(player);
			DeftHands.forget(player);
			Flashpoint.forget(player);
			ArmorUpkeep.forget(player);
			StorageTracker.forget(player);
			SteadyGrid.release(player);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			dev.toolmastery.perk.SmeltHandler.tick(server);
			// After Smelt, so the magnet pockets the smelted result rather than the raw ore.
			MinersMagnet.tick(server);
			// Last: both collect the drops the events above have just spawned.
			AxeHarvest.tick(server);
			CombatDrops.tick(server);

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				// Every tick, because both react to a state that changes within
				// one: a sprint that just broke, a hotbar stack that just ran out.
				Trailblazer.tick(player);
				DeftHands.tick(player);
				// Every tick too: a braced spear reaches as far as the spear in
				// hand, and a Hunter's Mark has to go out on time.
				CombatPerks.tick(player);
				// Same clock: a Piercing Sight outline burns out on time, and
				// an over-drawn Storm of Arrows reports what it has banked.
				BowPerks.tick(player);
				// Every tick as well: a ten-second window has to close on the
				// tick it runs out, not up to a second late.
				Flashpoint.tick(player);
				// Sure Footing's attributes and Last Stand's rescue: both react
				// to a state that can change inside one tick.
				ArmorUpkeep.tick(player);
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
					AquaLungs.tick(player);
					// Artisan: nine of an ore material pack into the block.
					dev.toolmastery.perk.AutoBlock.slowTick(player);
					// Explorer: distance off the vanilla statistics, places off
					// where the player is standing.
					MovementTracker.tick(player);
					BiomeTracker.tick(player);
					CombatTracker.tick(player);
					// Armor: the gates measured in time worn and time on fire.
					ArmorTracker.tick(player);
					ArmorUpkeep.slowTick(player);
					// Bow: Rapid Reload II's background crossbow load.
					BowPerks.slowTick(player);
				}
			}
		});

		LOGGER.info("Tool Mastery initialized — sharpen your skills.");
	}
}
