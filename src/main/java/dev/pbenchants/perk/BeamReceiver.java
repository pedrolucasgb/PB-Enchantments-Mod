package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.track.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What a beacon's pulse becomes for each player standing in it.
 *
 * <p>Vanilla's {@code BeaconBlockEntity.applyEffects} runs every 80 ticks: it
 * collects the players in a box the size of the pyramid's range and gives each
 * of them the same primary (and, on a full pyramid, the same secondary) for
 * {@code (9 + 2 * levels)} seconds. This class is that method with the player
 * asked what they have earned, and it is the <b>only</b> place the mod
 * touches a beacon's effects — the mixin cancels vanilla and calls
 * {@link #apply}, so nothing can desync between two code paths.
 *
 * <p>Everything here is receiver-side. The beacon's own range, level and
 * choices are read, never written; two players in the same beam can leave
 * with different effects and the block is none the wiser.
 *
 * <ul>
 *   <li><b>Reach of the Beam</b> I–III: the player is served from 10 / 20 / 40
 *       blocks beyond the range. One wider box query per pulse, then a
 *       per-player box for anyone outside the vanilla one.</li>
 *   <li><b>Lingering Light</b> I–III: the duration is longer for that player
 *       by 30 s / 90 s / 5 min, so the effect outlives the beam by that.</li>
 *   <li><b>Phantom Tier</b>: the pyramid counts one layer higher for the
 *       player's effects (not its range).</li>
 *   <li><b>Brighter Beam</b>: on a full pyramid the primary is one level
 *       stronger whatever the secondary choice.</li>
 *   <li><b>Resonant Haste</b>: a Haste that would be II is IV instead.
 *       Vanilla's ceiling is II (a full pyramid with Haste in both slots).
 *       Deepslate is hardness 3, so one tick needs destroy speed 90: an
 *       Efficiency V netherite pickaxe is 35, Mason's Grip III makes it 56,
 *       and Haste IV (x1.8) lands at 100.8; diamond lands at 97.9. Haste
 *       III would stop at 89.6 — two ticks — and Mason's Grip II at 88.2,
 *       so the instamine is exactly beacon + full grip + Efficiency V, and
 *       never the pickaxe alone. Never past IV.</li>
 *   <li><b>Early Regeneration</b>: two layers or more add Regeneration I.</li>
 *   <li><b>Prism</b>: the player's attuned extra power joins the list.</li>
 * </ul>
 *
 * <p>The tracker is told what was applied, so time in the beam, the power
 * checklist and time under Regeneration are all counted from the same pulse.
 */
public final class BeamReceiver {
	/** How long a pulse's effects last at each pyramid level, before any node. Vanilla's own formula. */
	private static int vanillaDuration(int levels) {
		return (9 + levels * 2) * 20;
	}

	/** Vanilla's range, in blocks, for a pyramid of this many layers. */
	private static double vanillaRange(int levels) {
		return levels * 10 + 10;
	}

	/** Blocks beyond the range per rank of Reach of the Beam; index 0 is no node. */
	private static final int[] REACH_BLOCKS = {0, 10, 20, 40};

	/** Extra ticks per rank of Lingering Light; index 0 is no node. */
	private static final int[] LINGER_TICKS = {0, 30 * 20, 90 * 20, 5 * 60 * 20};

	/** The widest any reach gets — the one box query the pulse makes. */
	private static final int MAX_REACH = REACH_BLOCKS[REACH_BLOCKS.length - 1];

	/** A pyramid this tall has a secondary slot. Vanilla's LEVELS_NEEDED_FOR_SECONDARY. */
	private static final int FULL_PYRAMID = 4;

	/** Early Regeneration wants at least this many layers. */
	private static final int REGENERATION_LEVELS = 2;

	/** Vanilla's Night Vision flickers under ten seconds left; a pulse is four seconds apart. */
	private static final int NIGHT_VISION_PADDING = 10 * 20;

	/**
	 * Saturation heals a food point every tick it is on, so a pulse hands out
	 * two ticks of it and no more: a bite every four seconds, not a full
	 * stomach forever.
	 */
	private static final int SATURATION_TICKS = 2;

	private BeamReceiver() {
	}

	/** The mixin's whole body: vanilla's method, per player. */
	public static void apply(ServerLevel level, BlockPos pos, int levels, Holder<MobEffect> primary,
			@Nullable Holder<MobEffect> secondary) {
		double range = vanillaRange(levels);
		int height = level.getHeight();
		AABB core = new AABB(pos).inflate(range).expandTowards(0.0, height, 0.0);
		AABB widest = new AABB(pos).inflate(range + MAX_REACH).expandTowards(0.0, height, 0.0);

		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, widest)) {
			AABB served = core;
			int reach = REACH_BLOCKS[PerkAccess.rank(player, SkillTrees.BEACON, BeaconPerks.REACH_OF_THE_BEAM)];
			if (reach > 0) {
				served = new AABB(pos).inflate(range + reach).expandTowards(0.0, height, 0.0);
			}
			if (!served.intersects(player.getBoundingBox())) {
				continue;
			}
			List<MobEffectInstance> effects = effectsFor(player, levels, primary, secondary);
			for (MobEffectInstance effect : effects) {
				player.addEffect(effect);
			}
			BeaconTracker.onPulse(player, effects);
		}
	}

	/**
	 * The list of effects one pulse gives this player. Pure apart from the
	 * node lookups, so the manual test matrix in the release notes can be
	 * checked line by line against it.
	 */
	public static List<MobEffectInstance> effectsFor(ServerPlayer player, int levels, Holder<MobEffect> primary,
			@Nullable Holder<MobEffect> secondary) {
		int tier = BeaconPerks.owns(player, BeaconPerks.PHANTOM_TIER) ? Math.min(FULL_PYRAMID, levels + 1) : levels;
		int duration = vanillaDuration(levels)
			+ LINGER_TICKS[PerkAccess.rank(player, SkillTrees.BEACON, BeaconPerks.LINGERING_LIGHT)];
		boolean full = tier >= FULL_PYRAMID;
		// Vanilla: the same power in both slots is the primary at level II.
		boolean doubled = full && Objects.equals(primary, secondary);
		int amplifier = doubled || (full && BeaconPerks.owns(player, BeaconPerks.BRIGHTER_BEAM)) ? 1 : 0;

		List<MobEffectInstance> out = new ArrayList<>(4);
		out.add(new MobEffectInstance(primary, duration, resonate(player, primary, amplifier), true, true));
		if (full && secondary != null && !doubled) {
			out.add(new MobEffectInstance(secondary, duration, 0, true, true));
		}
		if (tier >= REGENERATION_LEVELS && BeaconPerks.owns(player, BeaconPerks.EARLY_REGENERATION)
			&& !has(out, MobEffects.REGENERATION)) {
			out.add(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, true, true));
		}
		Holder<MobEffect> attuned = BeaconPerks.attunement(player);
		if (attuned != null && !has(out, attuned)) {
			out.add(new MobEffectInstance(attuned, attunedDuration(attuned, duration), 0, true, true));
		}
		return out;
	}

	/** Resonant Haste: II becomes IV, and nothing ever goes past IV. */
	private static int resonate(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
		if (amplifier == 1 && Objects.equals(effect, MobEffects.HASTE)
			&& BeaconPerks.owns(player, BeaconPerks.RESONANT_HASTE)) {
			return RESONANT_HASTE_AMPLIFIER;
		}
		return amplifier;
	}

	/** Haste IV. Each Haste level is +20% destroy speed and +10% attack speed. */
	private static final int RESONANT_HASTE_AMPLIFIER = 3;

	private static int attunedDuration(Holder<MobEffect> effect, int duration) {
		if (Objects.equals(effect, MobEffects.SATURATION)) {
			return SATURATION_TICKS;
		}
		if (Objects.equals(effect, MobEffects.NIGHT_VISION)) {
			return duration + NIGHT_VISION_PADDING;
		}
		return duration;
	}

	private static boolean has(List<MobEffectInstance> effects, Holder<MobEffect> effect) {
		for (MobEffectInstance instance : effects) {
			if (Objects.equals(instance.getEffect(), effect)) {
				return true;
			}
		}
		return false;
	}
}
