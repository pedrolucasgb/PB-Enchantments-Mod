package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Deep Haste — permanent Haste I below Y = 0.
 *
 * <p>Re-applied from the once-a-second slow tick with a short duration, so it
 * feels permanent while you are down there and fades a few seconds after you
 * climb out. A stronger or longer Haste already on the player (beacon, potion)
 * is never overwritten.
 */
public final class DeepHaste {
	private static final double MAX_Y = 0.0;
	private static final int DURATION_TICKS = 100;
	/** Only refresh once the remaining duration drops below this, to avoid a packet every second. */
	private static final int REFRESH_BELOW_TICKS = 40;

	private DeepHaste() {
	}

	/** Called once a second per online player. */
	public static void tick(ServerPlayer player) {
		if (player.getY() > MAX_Y) {
			return;
		}
		if (!SkillService.owns(player, SkillTrees.PICKAXE, "deep_haste")) {
			return;
		}
		MobEffectInstance current = player.getEffect(MobEffects.HASTE);
		if (current != null
			&& (current.getAmplifier() > 0
			|| current.isInfiniteDuration()
			|| current.getDuration() > REFRESH_BELOW_TICKS)) {
			return;
		}
		// ambient + no particles: it is a permanent state, not a buff you just drank.
		player.addEffect(new MobEffectInstance(MobEffects.HASTE, DURATION_TICKS, 0, true, false, true));
	}
}
