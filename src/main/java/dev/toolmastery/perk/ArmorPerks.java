package dev.toolmastery.perk;

import dev.toolmastery.skill.SkillTrees;
import net.minecraft.world.entity.player.Player;

/**
 * Armor-tree perk lookups, the same shape as {@link ExplorerPerks}.
 *
 * <p>Only the nodes that have an effect behind them are named here. The rest of
 * the tree ships as visible "coming soon" tiles, so a constant for them would
 * point at nothing.
 */
public final class ArmorPerks {
	public static final String FLASHPOINT = "flashpoint";

	private ArmorPerks() {
	}

	public static boolean owns(Player player, String nodeId) {
		return PerkAccess.owns(player, SkillTrees.ARMOR, nodeId);
	}
}
