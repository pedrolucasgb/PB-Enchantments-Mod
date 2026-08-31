package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Pufferfish Lungs — permanent Water Breathing for the Explorer capstone.
 *
 * <p>Same shape as {@link DeepHaste}: re-applied from the once-a-second slow
 * tick with a short duration rather than as an infinite effect, so a
 * {@code /effect clear} or a milk bucket is never permanently overridden and
 * the perk simply comes back on the next tick. Ambient and particle-free,
 * because it is a state the player earned rather than a potion they drank.
 *
 * <p>A longer Water Breathing already on the player — a potion, a conduit — is
 * left alone; there is nothing to gain from replacing it with a shorter one.
 */
public final class AquaLungs {
	private static final int DURATION_TICKS = 100;

	/** Only refresh once the remaining duration drops below this. */
	private static final int REFRESH_BELOW_TICKS = 40;

	public static final String PUFFERFISH_LUNGS = "pufferfish_lungs";

	private AquaLungs() {
	}

	/** Called once a second per online player. */
	public static void tick(ServerPlayer player) {
		if (!SkillService.owns(player, SkillTrees.EXPLORER, PUFFERFISH_LUNGS)) {
			return;
		}
		MobEffectInstance current = player.getEffect(MobEffects.WATER_BREATHING);
		if (current != null
			&& (current.isInfiniteDuration() || current.getDuration() > REFRESH_BELOW_TICKS)) {
			return;
		}
		player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, DURATION_TICKS, 0, true, false, true));
	}
}
