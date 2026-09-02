package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Bone Thrift — a third of the bone meal you spend comes back.
 *
 * <p>Bone meal is the farming resource nobody has enough of and everybody spends
 * in stacks, so the node is a refund rather than a stronger effect: a stack
 * finally finishes a field, without changing what one application does.
 *
 * <p>The refund is rolled after vanilla has already charged the stack, and only
 * while at least one is left in it. The very last bone meal is therefore never
 * refunded — growing an emptied stack back is not a thing an item stack survives
 * cleanly, and one in a stack of sixty-four is not a difference anybody can feel.
 */
public final class BoneThrift {
	public static final String NODE = "bone_thrift";

	private static final int REFUND_PERCENT = 33;

	private BoneThrift() {
	}

	public static void refund(ServerPlayer player, ItemStack stack) {
		if (player.hasInfiniteMaterials() || !stack.is(Items.BONE_MEAL) || stack.isEmpty()) {
			return;
		}
		if (!PerkAccess.owns(player, SkillTrees.GROUND, NODE)) {
			return;
		}
		if (player.getRandom().nextInt(100) < REFUND_PERCENT) {
			stack.grow(1);
		}
	}
}
