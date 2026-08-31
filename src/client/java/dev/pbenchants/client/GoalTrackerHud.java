package dev.pbenchants.client;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.client.gui.SkillTreeStyle;
import dev.pbenchants.network.SkillStatePayload;
import dev.pbenchants.skill.GateRequirement;
import dev.pbenchants.skill.SkillNode;
import dev.pbenchants.skill.SkillTier;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.skill.XpMath;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The pinned-goal tracker: a small scoreboard on the right edge of the screen
 * showing the gate achievements still missing for the one node or tier the
 * player pinned with the skill screen's Track button.
 *
 * <p>Counters come from the same synced snapshot the skill screen reads, so
 * the lines tick up live as the player plays — the whole point of pinning a
 * goal is not having to reopen the tree to see how far along the grind is.
 */
public final class GoalTrackerHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "goal_tracker");

	/** Right-edge padding, and the vertical start — below where potion effect icons live. */
	private static final int PADDING = 6;

	private record Line(String text, int color) {
	}

	private GoalTrackerHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, ID, (graphics, delta) -> draw(graphics));
	}

	private static void draw(GuiGraphicsExtractor graphics) {
		GoalTracker.Pin pin = GoalTracker.pinned();
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (pin == null || player == null || player.isSpectator()) {
			return;
		}
		SkillTree tree = SkillTrees.byId(pin.treeId());
		SkillStatePayload.TreeState state = ClientSkillState.tree(pin.treeId());
		if (tree == null || state == null) {
			return;
		}

		List<Line> lines = pin.nodeId() != null
			? nodeLines(tree, tree.node(pin.nodeId()), state)
			: tierLines(tree, pin.tier(), state);
		if (lines.isEmpty()) {
			return;
		}

		Font font = client.font;
		int y = graphics.guiHeight() / 3;
		for (Line line : lines) {
			int x = graphics.guiWidth() - PADDING - font.width(line.text());
			SkillTreeStyle.outlinedText(graphics, font, line.text(), x, y, line.color());
			y += 10;
		}
	}

	private static List<Line> tierLines(SkillTree tree, int tierIndex, SkillStatePayload.TreeState state) {
		List<Line> lines = new ArrayList<>();
		if (tierIndex < 0 || tierIndex >= tree.tiers().size()) {
			return lines;
		}
		lines.add(new Line("⚑ " + tree.tierName(tierIndex).getString(), SkillTreeStyle.GOLD));
		lines.add(new Line(tree.shortName().getString() + " — "
			+ Component.translatable("hud.pbenchants.track.tier", tierIndex + 1).getString(), SkillTreeStyle.MUTED));
		if (tierIndex < state.unlockedTiers()) {
			lines.add(new Line(Component.translatable("hud.pbenchants.track.unlocked").getString(),
				SkillTreeStyle.GREEN));
			return lines;
		}
		SkillTier tier = tree.tiers().get(tierIndex);
		if (addGateLines(lines, tier, state)) {
			lines.add(new Line(Component.translatable("hud.pbenchants.track.gate_done",
				XpMath.pointsForLevel(tier.accessCost())).getString(), SkillTreeStyle.GOLD));
		}
		return lines;
	}

	private static List<Line> nodeLines(SkillTree tree, SkillNode node, SkillStatePayload.TreeState state) {
		List<Line> lines = new ArrayList<>();
		if (node == null) {
			return lines;
		}
		lines.add(new Line("⚑ " + node.displayName().getString(), SkillTreeStyle.GOLD));
		lines.add(new Line(tree.shortName().getString(), SkillTreeStyle.MUTED));
		if (state.purchased().contains(node.id())) {
			lines.add(new Line(Component.translatable("hud.pbenchants.track.unlocked").getString(),
				SkillTreeStyle.GREEN));
			return lines;
		}

		boolean blocked = false;
		if (node.tier() >= state.unlockedTiers()) {
			blocked = true;
			// The gate that matters right now is the next tier's — tiers open in
			// order, so that is always the one the player is actually grinding.
			int gateTier = state.unlockedTiers();
			lines.add(new Line(Component.translatable("hud.pbenchants.track.needs_tier",
				node.tier() + 1).getString(), SkillTreeStyle.TEXT));
			addGateLines(lines, tree.tiers().get(gateTier), state);
			if (node.tier() > gateTier) {
				lines.add(new Line(Component.translatable("hud.pbenchants.track.more_tiers",
					gateTier + 1, node.tier() + 1).getString(), SkillTreeStyle.DIM));
			}
		}
		if (node.requires() != null && !state.purchased().contains(node.requires())) {
			blocked = true;
			lines.add(new Line("□ " + Component.translatable("hud.pbenchants.track.requires",
				SkillNode.displayName(node.requires()).getString()).getString(), SkillTreeStyle.BAD));
		}
		if (!blocked) {
			lines.add(new Line(Component.translatable("hud.pbenchants.track.ready",
				XpMath.pointsForLevel(node.unlockCost())).getString(), SkillTreeStyle.GOLD));
		}
		return lines;
	}

	/** One "✓/□ name 3/10" line per gate. Returns true when every line is complete. */
	private static boolean addGateLines(List<Line> lines, SkillTier tier, SkillStatePayload.TreeState state) {
		boolean allDone = true;
		for (GateRequirement gate : tier.gates()) {
			int count = Math.min(state.counters().getOrDefault(gate.id(), 0), gate.target());
			boolean done = count >= gate.target();
			allDone &= done;
			lines.add(new Line((done ? "✓ " : "□ ") + gate.displayName() + " " + count + "/" + gate.target(),
				done ? SkillTreeStyle.GREEN : SkillTreeStyle.TEXT));
		}
		return allDone;
	}
}
