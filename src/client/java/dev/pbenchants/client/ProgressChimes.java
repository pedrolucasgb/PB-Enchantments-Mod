package dev.pbenchants.client;

import dev.pbenchants.network.SkillStatePayload;
import dev.pbenchants.skill.GateRequirement;
import dev.pbenchants.skill.SkillNode;
import dev.pbenchants.skill.SkillTier;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.Map;

/**
 * The two moments in the progression worth hearing, both judged off the synced
 * snapshot rather than announced by the server — which is what keeps them from
 * ever landing twice.
 *
 * <ul>
 *   <li><b>A tier opens.</b> The loudest thing the mod does, and deliberately
 *       not the loudest thing the game does: a level-up chime rather than the
 *       challenge-complete fanfare vanilla saves for the End.</li>
 *   <li><b>The pinned goal comes good.</b> The scoreboard on the right edge is
 *       something you watch while you play, so the tick that finishes it should
 *       reach you without looking. A lighter bell, one step down.</li>
 * </ul>
 *
 * <p>The second never doubles up on the first. A pin only chimes on the step
 * from "still grinding" to "ready to buy" — never on the step from ready to
 * bought — so pinning a tier and then unlocking it plays the tier chime alone,
 * which is exactly the case that would otherwise fire twice in one snapshot.
 */
public final class ProgressChimes {
	/** A tier just opened: satisfying, and clearly not the game's biggest sound. */
	private static final SoundEvent TIER_SOUND = SoundEvents.PLAYER_LEVELUP;
	private static final float TIER_VOLUME = 0.55F;
	private static final float TIER_PITCH = 1.25F;

	/** The pinned goal's last box just got ticked. */
	private static final SoundEvent GOAL_SOUND = SoundEvents.AMETHYST_BLOCK_CHIME;
	private static final float GOAL_VOLUME = 0.8F;
	private static final float GOAL_PITCH = 1.5F;

	/** How far along the pinned goal is — the whole reason a chime is or is not due. */
	public enum Stage {
		/** Gates unfinished, or a prerequisite still missing. */
		PENDING,
		/** Everything asked for is done; only the XP has to be spent. */
		READY,
		/** Bought — tier unlocked, or node owned. */
		DONE
	}

	/** Tiers each tree had in the last snapshot, so an increase can be spotted. */
	private static final Map<String, Integer> lastTiers = new HashMap<>();

	/** Whether a baseline has been taken; the first snapshot after login is silent. */
	private static boolean primed;

	private static Stage lastStage;

	private ProgressChimes() {
	}

	/**
	 * Called with every progress snapshot, after the cache has taken it. The
	 * first one of a session only records where things stand — logging in with
	 * six tiers already open is not six tiers being opened.
	 */
	public static void accept(SkillStatePayload payload) {
		boolean tierOpened = false;
		for (Map.Entry<String, SkillStatePayload.TreeState> entry : payload.trees().entrySet()) {
			int now = entry.getValue().unlockedTiers();
			Integer before = lastTiers.put(entry.getKey(), now);
			if (primed && before != null && now > before) {
				tierOpened = true;
			}
		}

		Stage stage = stageOfPin();
		boolean goalReady = primed && lastStage == Stage.PENDING && stage == Stage.READY;
		lastStage = stage;

		if (!primed) {
			primed = true;
			return;
		}
		if (tierOpened) {
			play(TIER_SOUND, TIER_VOLUME, TIER_PITCH);
		} else if (goalReady) {
			// Only ever the one sound per snapshot: a tier opening is already
			// the answer to "did the thing I was working towards land?".
			play(GOAL_SOUND, GOAL_VOLUME, GOAL_PITCH);
		}
	}

	/** Pinning something new restarts the comparison — no chime for a goal already done. */
	public static void pinChanged() {
		lastStage = stageOfPin();
	}

	/** On disconnect: the next world starts its own baseline. */
	public static void clear() {
		lastTiers.clear();
		lastStage = null;
		primed = false;
	}

	/**
	 * Where the pinned goal stands, read exactly the way the HUD reads it —
	 * {@link GoalTrackerHud} paints "Gate complete"/"Ready to unlock" under the
	 * same conditions, so what the player hears matches what they see.
	 */
	private static Stage stageOfPin() {
		GoalTracker.Pin pin = GoalTracker.pinned();
		if (pin == null) {
			return null;
		}
		SkillTree tree = SkillTrees.byId(pin.treeId());
		SkillStatePayload.TreeState state = ClientSkillState.tree(pin.treeId());
		if (tree == null || state == null) {
			return null;
		}
		return pin.nodeId() != null
			? nodeStage(tree, tree.node(pin.nodeId()), state)
			: tierStage(tree, pin.tier(), state);
	}

	private static Stage tierStage(SkillTree tree, int tierIndex, SkillStatePayload.TreeState state) {
		if (tierIndex < 0 || tierIndex >= tree.tiers().size()) {
			return null;
		}
		if (tierIndex < state.unlockedTiers()) {
			return Stage.DONE;
		}
		SkillTier tier = tree.tiers().get(tierIndex);
		for (GateRequirement gate : tier.gates()) {
			if (state.counters().getOrDefault(gate.id(), 0) < gate.target()) {
				return Stage.PENDING;
			}
		}
		return Stage.READY;
	}

	private static Stage nodeStage(SkillTree tree, SkillNode node, SkillStatePayload.TreeState state) {
		if (node == null) {
			return null;
		}
		if (state.purchased().contains(node.id())) {
			return Stage.DONE;
		}
		if (node.tier() >= state.unlockedTiers()) {
			return Stage.PENDING;
		}
		if (node.requires() != null && !state.purchased().contains(node.requires())) {
			return Stage.PENDING;
		}
		return Stage.READY;
	}

	private static void play(SoundEvent sound, float volume, float pitch) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		client.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}
}
