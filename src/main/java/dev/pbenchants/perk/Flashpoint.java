package dev.pbenchants.perk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Flashpoint — touching lava or fire buys you ten seconds of immunity to both.
 *
 * <p>The window opens on the first point of lava or fire damage the node would
 * have let through, runs for ten seconds whatever you do with them, and then
 * closes. What keeps it a per-incident escape rather than a way to live in lava
 * is a flat <b>two-minute cooldown</b>, counted from the moment the window
 * opens: jumping back in during the window does not extend it, and once it is
 * spent the node stays dark until the cooldown runs out, wherever you are
 * standing.
 *
 * <p>Fire-tagged damage only — the ten seconds cover the swim out, the fire
 * the lava lit on you, and anything else vanilla calls fire, but not the fall
 * that dropped you in.
 *
 * <p>State is transient. A relog forgets a running cooldown, which is a small
 * mercy rather than an exploit lever: two minutes is short enough that dying
 * and relogging to skip it costs more than it saves.
 */
public final class Flashpoint {
	/** How long one window lasts. Ten seconds, in ticks. */
	public static final int WINDOW_TICKS = 200;

	/** How long from one window opening to the next being allowed. Two minutes, in ticks. */
	public static final int COOLDOWN_TICKS = 2400;

	private static final class State {
		int ticksLeft;
		int cooldown;
	}

	private static final Map<UUID, State> states = new HashMap<>();

	private Flashpoint() {
	}

	/**
	 * {@code ServerLivingEntityEvents.ALLOW_DAMAGE} hook: returning false drops
	 * the hit entirely.
	 *
	 * <p>Arming happens here rather than on the tick that first sees the player
	 * standing in lava, so the entry hit itself is covered — a window that
	 * opened one tick late would let four points through every time and read as
	 * a bug.
	 */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!covered(source) || !(entity instanceof ServerPlayer player)
			|| !ArmorPerks.owns(player, ArmorPerks.FLASHPOINT)) {
			return true;
		}
		State state = states.computeIfAbsent(player.getUUID(), uuid -> new State());
		if (state.ticksLeft <= 0 && state.cooldown <= 0) {
			state.ticksLeft = WINDOW_TICKS;
			state.cooldown = COOLDOWN_TICKS;
			say(player, "perk.pbenchants.flashpoint.open", ChatFormatting.GOLD);
		}
		return state.ticksLeft <= 0;
	}

	/**
	 * The damage the window absorbs: everything vanilla files under fire — the
	 * same {@code IS_FIRE} tag Fire Resistance reads, so lava, burning, magma,
	 * campfires and fireballs all count. Ten seconds of the potion, in effect.
	 */
	private static boolean covered(DamageSource source) {
		return source.is(DamageTypeTags.IS_FIRE);
	}

	/** Called every server tick per online player. */
	public static void tick(ServerPlayer player) {
		State state = states.get(player.getUUID());
		if (state == null) {
			return;
		}
		if (state.ticksLeft > 0) {
			state.ticksLeft--;
			if (state.ticksLeft > 0 && state.ticksLeft % 20 == 0) {
				player.sendSystemMessage(Component.translatable("perk.pbenchants.flashpoint.countdown",
					Component.literal(String.valueOf(state.ticksLeft / 20)).withStyle(ChatFormatting.GOLD)), true);
			} else if (state.ticksLeft == 0) {
				say(player, "perk.pbenchants.flashpoint.spent", ChatFormatting.RED);
			}
		}
		if (state.cooldown > 0) {
			state.cooldown--;
			if (state.cooldown == 0) {
				say(player, "perk.pbenchants.flashpoint.ready", ChatFormatting.AQUA);
			}
		}
	}

	/** Drops the transient state when a player leaves. */
	public static void forget(ServerPlayer player) {
		states.remove(player.getUUID());
	}

	private static void say(ServerPlayer player, String key, ChatFormatting colour) {
		player.sendSystemMessage(Component.translatable(key).withStyle(colour), true);
	}
}
