package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Pufferfish Lungs — a lungful of pufferfish air on every dive.
 *
 * <p>The first cut of this capstone was permanent Water Breathing, and it made
 * the breath meter a dead mechanic: infinite air is no decision at all. Now
 * each time the player goes under they get 15 seconds of Water Breathing —
 * one dive's worth. Stay down longer and the meter starts running like anyone
 * else's; surface, even for a moment, and the next dive fills the lungs again.
 *
 * <p>Driven from the once-a-second slow tick, so "going under" is detected as
 * the head crossing the surface between two ticks. A longer Water Breathing
 * already on the player — a potion, a conduit — is left alone; the dive only
 * tops the effect up, never trims it. Ambient and particle-free, because it is
 * a state the player earned rather than a potion they drank.
 */
public final class AquaLungs {
	/** One diveful of air: 15 seconds of Water Breathing. */
	private static final int DIVE_TICKS = 15 * 20;

	public static final String PUFFERFISH_LUNGS = "pufferfish_lungs";

	/**
	 * Players whose head is currently under water, so one dive grants one
	 * lungful rather than a refresh every second. Surfacing clears the entry;
	 * so does losing the node or logging out and back in above water.
	 */
	private static final Set<UUID> submerged = new HashSet<>();

	private AquaLungs() {
	}

	/** Called once a second per online player. */
	public static void tick(ServerPlayer player) {
		if (!SkillService.owns(player, SkillTrees.EXPLORER, PUFFERFISH_LUNGS) || !player.isUnderWater()) {
			submerged.remove(player.getUUID());
			return;
		}
		if (!submerged.add(player.getUUID())) {
			return; // still the same dive
		}
		MobEffectInstance current = player.getEffect(MobEffects.WATER_BREATHING);
		if (current != null && (current.isInfiniteDuration() || current.getDuration() > DIVE_TICKS)) {
			return;
		}
		player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, DIVE_TICKS, 0, true, false, true));
	}
}
