package dev.toolmastery.track;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Feeds enchanter gate counters from enchanting-table use. Called from the
 * EnchantmentMenu mixin right after a successful enchant, server-side only.
 */
public final class EnchantTracker {
	private EnchantTracker() {
	}

	/**
	 * @param slotId 0-based table slot used (2 = the bottom, level-30 slot)
	 * @param levels XP levels actually deducted (slotId + 1; 0 in creative-like
	 *               flows is still a valid enchant)
	 */
	public static void onTableEnchant(Player player, int slotId, int levels) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		TreeProgress progress = SkillService.progress(serverPlayer, SkillTrees.ENCHANTER);
		progress.addCount("enchant_items", 1);
		if (levels > 0) {
			progress.addCount("spend_levels", levels);
		}
		if (slotId == 2) {
			progress.addCount("max_slot_enchants", 1);
		}
	}
}
