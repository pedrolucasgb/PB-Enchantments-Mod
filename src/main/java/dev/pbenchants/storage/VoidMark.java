package dev.pbenchants.storage;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Void Mark, the half that destroys things.
 *
 * <p>One question, asked at one place: {@code Inventory.add(ItemStack)} is the
 * door every pickup comes through — a stack walked over on the ground, and
 * everything the Miner's, Logger's, Digger's, Harvester's and Combat magnets
 * pocket, since all five hand their drops to that same method. Answering it
 * with "the stack is now empty, and yes it was taken" makes the caller do the
 * rest of a normal pickup: the animation, the statistic, the discarded entity.
 * Nothing else in the mod has to know the mark exists.
 *
 * <p>What it refuses to eat: a stack that carries a mark of its own (that was
 * put down on purpose, and picking your own filter back up must not feed it
 * to itself), anything in creative mode, and anything for a player who has
 * not bought the node — the mark is a component, so it survives the node
 * being sold back and would otherwise keep working for free.
 */
public final class VoidMark {
	private VoidMark() {
	}

	/**
	 * True when the stack about to enter this inventory should be destroyed
	 * instead. Server-side only; the client never adds pickups itself.
	 */
	public static boolean absorbs(Player player, ItemStack incoming) {
		if (incoming.isEmpty() || !(player instanceof ServerPlayer serverPlayer) || player.isCreative()
			|| ItemLock.marked(incoming)) {
			return false;
		}
		if (!SkillService.owns(serverPlayer, SkillTrees.ARTISAN, ItemLock.VOID_NODE)) {
			return false;
		}
		Inventory inventory = player.getInventory();
		for (ItemStack stack : inventory.getNonEquipmentItems()) {
			// The lock mixin already makes the marks invisible to this comparison.
			if (ItemLock.voided(stack) && ItemStack.isSameItemSameComponents(stack, incoming)) {
				return true;
			}
		}
		return false;
	}
}
