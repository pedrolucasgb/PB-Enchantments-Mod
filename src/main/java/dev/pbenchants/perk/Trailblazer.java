package dev.pbenchants.perk;

import dev.pbenchants.PBEnchants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Trailblazer — a long overland run builds up speed.
 *
 * <p>Sprint without stopping for eight seconds and the bonus ramps in to +12%;
 * drop to a walk and it lingers for two seconds before it goes, so a hop over a
 * fence or a step around a tree does not reset the run. It rewards crossing a
 * continent, not winning a fight: the moment you stop sprinting the clock stops
 * building.
 *
 * <p>Implemented as a transient attribute modifier rather than a Speed effect —
 * no particles, no potion icon, and it composes with Speed instead of fighting
 * it for the same slot.
 */
public final class Trailblazer {
	private static final Identifier MODIFIER_ID =
		Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "trailblazer");

	/** Ticks of unbroken sprinting before the bonus is at full strength. */
	private static final int RAMP_TICKS = 160;

	/** Ticks the bonus survives after the player stops sprinting. */
	private static final int GRACE_TICKS = 40;

	private static final double MAX_BONUS = 0.12;

	/** Ticks of sprint credit per player. Transient: a relog starts the run over. */
	private static final Map<UUID, Integer> sprintTicks = new HashMap<>();

	private Trailblazer() {
	}

	/** Called every server tick per online player. */
	public static void tick(ServerPlayer player) {
		if (!ExplorerPerks.owns(player, ExplorerPerks.TRAILBLAZER)) {
			if (sprintTicks.remove(player.getUUID()) != null) {
				apply(player, 0.0);
			}
			return;
		}
		int ticks = sprintTicks.getOrDefault(player.getUUID(), 0);
		if (player.isSprinting()) {
			ticks = Math.min(RAMP_TICKS + GRACE_TICKS, ticks + 1);
		} else {
			// Falling back through the grace window first, then losing the run.
			ticks = Math.max(0, ticks - 1);
			if (ticks < RAMP_TICKS) {
				ticks = Math.max(0, ticks - 3);
			}
		}
		sprintTicks.put(player.getUUID(), ticks);
		apply(player, MAX_BONUS * Math.min(1.0, (double) ticks / RAMP_TICKS));
	}

	/** Drops the run when a player leaves, so the map does not grow forever. */
	public static void forget(ServerPlayer player) {
		sprintTicks.remove(player.getUUID());
	}

	private static void apply(ServerPlayer player, double bonus) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		AttributeModifier current = speed.getModifier(MODIFIER_ID);
		if (bonus <= 0.0) {
			if (current != null) {
				speed.removeModifier(MODIFIER_ID);
			}
			return;
		}
		if (current != null && Math.abs(current.amount() - bonus) < 0.001) {
			return; // already at this step; do not churn the attribute every tick
		}
		speed.removeModifier(MODIFIER_ID);
		speed.addTransientModifier(
			new AttributeModifier(MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}
}
