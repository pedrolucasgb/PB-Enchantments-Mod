package dev.pbenchants.perk;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Flashpoint — touching lava buys you ten seconds of immunity to it.
 *
 * <p>The window opens on the first point of lava damage the node would have let
 * through, runs for ten seconds whatever you do with them, and then closes. It
 * <b>only rearms once you are no longer burning</b>, which is what keeps it a
 * per-incident escape rather than a way to live in lava: jumping back in during
 * the window does not extend it, and hopping out for a tick does not refill it,
 * because the fire lava sets is still on you. Reach water — or wait the fire
 * out — and the next dip is a fresh ten seconds.
 *
 * <p>Lava damage only. You still burn the whole time, at the usual one point a
 * second, so the node buys you a swim to the shore and not a bath: ten seconds
 * of lava is 80 points of damage, ten seconds of the fire it lit is 10.
 *
 * <p>State is transient. A relog rearms the node, which is harmless — you have
 * to be out of the lava and off the fire to get back in, and that is the same
 * condition the tick handler checks anyway.
 */
public final class Flashpoint {
	/** How long one window lasts. Ten seconds, in ticks. */
	public static final int WINDOW_TICKS = 200;

	private static final class State {
		int ticksLeft;
		boolean armed = true;
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
		if (!source.is(DamageTypes.LAVA) || !(entity instanceof ServerPlayer player)
			|| !ArmorPerks.owns(player, ArmorPerks.FLASHPOINT)) {
			return true;
		}
		State state = states.computeIfAbsent(player.getUUID(), uuid -> new State());
		if (state.armed) {
			state.armed = false;
			state.ticksLeft = WINDOW_TICKS;
			say(player, "perk.pbenchants.flashpoint.open", ChatFormatting.GOLD);
		}
		return state.ticksLeft <= 0;
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
		// The rearm condition, and the whole reason the node is not infinite:
		// lava sets you alight for fifteen seconds, so you cannot be off the
		// fire before the window you just spent has closed.
		if (!state.armed && !player.isOnFire() && !player.isInLava()) {
			state.armed = true;
			state.ticksLeft = 0;
			say(player, "perk.pbenchants.flashpoint.ready", ChatFormatting.AQUA);
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
