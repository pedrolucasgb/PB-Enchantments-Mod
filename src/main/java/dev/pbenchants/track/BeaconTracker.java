package dev.pbenchants.track;

import dev.pbenchants.mixin.BeaconMenuAccessor;
import dev.pbenchants.perk.BeaconPerks;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.GateChecklists;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;

/**
 * Feeds the Beacon tree's gates. Every event the tree cares about already
 * fires for some other tree — a kill, a pickup, a craft, a placed block, a
 * structure underfoot — so each hook is one extra line in the tracker that
 * owns the event, calling in here. The two the tree has to itself are the
 * beacon's own: a payment at the menu, and a pulse of the beam.
 *
 * <p>Counters are per tree: {@code slay_wither} here is separate from the
 * Sword tree's {@code slay_boss}, though both come off the same death.
 */
public final class BeaconTracker {
	/** One pulse of a beacon is 80 ticks: four seconds in the beam. */
	private static final int SECONDS_PER_PULSE = 4;

	/** The blocks a pyramid is made of. Placing one anywhere counts for both placement gates. */
	private static final List<net.minecraft.world.level.block.Block> PYRAMID_BLOCKS = List.of(
		Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.EMERALD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.NETHERITE_BLOCK);

	private BeaconTracker() {
	}

	private static TreeProgress progress(ServerPlayer player) {
		return SkillService.progress(player, SkillTrees.BEACON);
	}

	// ---------- events other trackers already see ----------

	/** From {@code CombatTracker.onKill}: the skeletons for tier 1, the boss for every tier after. */
	public static void onKill(ServerPlayer player, LivingEntity victim) {
		if (victim.getType() == EntityTypes.WITHER_SKELETON) {
			progress(player).addCount("kill_wither_skeletons", 1);
		} else if (victim.getType() == EntityTypes.WITHER) {
			progress(player).addCount("slay_wither", 1);
		}
	}

	/** From the pickup mixin and the Combat Magnet: the skull, however it reached the bag. */
	public static void onPickup(ServerPlayer player, Item item, int count) {
		if (count > 0 && item == Items.WITHER_SKELETON_SKULL) {
			progress(player).counters.put("collect_wither_skull", 1);
		}
	}

	/** From {@code ItemGainTracker.onCraftTake}. */
	public static void onCraft(ServerPlayer player, ItemStack stack, int amount) {
		if (stack.is(Items.BEACON)) {
			progress(player).addCount("craft_beacon", amount);
		}
	}

	/** From {@code PlaceTracker.onPlace}: one block feeds both placement gates, by design. */
	public static void onPlace(ServerPlayer player, BlockState state) {
		if (PYRAMID_BLOCKS.contains(state.getBlock())) {
			TreeProgress progress = progress(player);
			progress.addCount("place_metal_blocks", 1);
			progress.addCount("place_pyramid_blocks", 1);
		}
	}

	/** From {@code BiomeTracker.tick}: standing in a fortress. */
	public static void onStructure(ServerPlayer player, String structureId) {
		if (structureId.equals("minecraft:fortress")) {
			progress(player).counters.put("visit_fortress", 1);
		}
	}

	// ---------- the beacon's own ----------

	/**
	 * A payment the beacon menu just consumed. Called from the packet handler
	 * with a copy of what sat in the payment slot before the click, once the
	 * slot is seen to be empty after it.
	 */
	public static void onBeaconPaid(ServerPlayer player, BeaconMenu menu, ItemStack paid) {
		TreeProgress progress = progress(player);
		progress.addCount("pay_beacon", 1);
		progress.counters.put("activate_beacon", 1);
		if (menu.getLevels() >= 4) {
			progress.counters.put("pyramid_tier_4", 1);
		}
		BlockPos pos = ((BeaconMenuAccessor) menu).pbenchants$access()
			.evaluate((level, at) -> at).orElse(null);
		if (pos != null) {
			String key = player.level().dimension().identifier() + "/" + pos.getX() + "/" + pos.getY() + "/" + pos.getZ();
			progress.see("beacon", key, "beacons_activated");
		}

		// Thrifty Offering: one payment in four is handed back.
		if (BeaconPerks.owns(player, BeaconPerks.THRIFTY_OFFERING)
			&& player.getRandom().nextFloat() < BeaconPerks.THRIFTY_CHANCE) {
			ItemStack refund = paid.copyWithCount(1);
			player.getInventory().placeItemBackInInventory(refund);
			player.sendSystemMessage(Component.translatable("perk.pbenchants.thrifty_offering.refund",
				paid.getHoverName()).withStyle(ChatFormatting.GOLD), true);
		}
	}

	/**
	 * One pulse of a beacon reached this player with these effects. Four
	 * seconds on the clock per pulse — a player inside two beams is credited
	 * by both, which is the price of never storing anything on the beacon.
	 */
	public static void onPulse(ServerPlayer player, List<MobEffectInstance> effects) {
		TreeProgress progress = progress(player);
		progress.addCount("seconds_in_beam", SECONDS_PER_PULSE);
		progress.counters.put("minutes_in_beam", progress.count("seconds_in_beam") / 60);

		boolean regenerating = false;
		for (MobEffectInstance effect : effects) {
			Holder<MobEffect> power = effect.getEffect();
			int bit = checklistBit(power);
			if (bit >= 0) {
				GateChecklists.tick(progress, "beacon_effect_checklist", bit);
			}
			if (Objects.equals(power, MobEffects.REGENERATION)) {
				regenerating = true;
			}
		}
		if (regenerating) {
			progress.addCount("seconds_regenerating", SECONDS_PER_PULSE);
			progress.counters.put("minutes_regenerating", progress.count("seconds_regenerating") / 60);
		}
	}

	/** The five powers a beacon grants, in {@code GateChecklists}' order; -1 for anything else. */
	private static int checklistBit(Holder<MobEffect> effect) {
		if (Objects.equals(effect, MobEffects.SPEED)) {
			return 0;
		}
		if (Objects.equals(effect, MobEffects.HASTE)) {
			return 1;
		}
		if (Objects.equals(effect, MobEffects.RESISTANCE)) {
			return 2;
		}
		if (Objects.equals(effect, MobEffects.JUMP_BOOST)) {
			return 3;
		}
		if (Objects.equals(effect, MobEffects.STRENGTH)) {
			return 4;
		}
		return -1;
	}
}
