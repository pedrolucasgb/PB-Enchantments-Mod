package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;

import java.util.Optional;

/**
 * What an Explorer does with a compass in hand.
 *
 * <p><b>Waypoint Stone</b> (tier 4) — sneak and right-click with a compass to
 * bind the spot you are standing on. The compass gets a real lodestone tracker
 * component, so the needle is drawn by vanilla and works in every dimension the
 * waypoint was set in; sneak-clicking again moves it.
 *
 * <p>Sneaking is the whole gesture. A plain right-click is left alone, so a
 * compass still behaves like a compass in the hand of an Explorer.
 */
public final class Waypoints {
	private Waypoints() {
	}

	/**
	 * Handles a compass right-click. Returns true when the perk consumed the
	 * interaction, so the caller can stop vanilla from also handling it.
	 */
	public static boolean onCompassUse(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.COMPASS) || !player.isShiftKeyDown()) {
			return false;
		}
		return bindWaypoint(player, stack);
	}

	/** Waypoint Stone: pins the compass to where the player is standing. */
	private static boolean bindWaypoint(ServerPlayer player, ItemStack stack) {
		if (!SkillService.owns(player, SkillTrees.EXPLORER, ExplorerPerks.WAYPOINT)) {
			return false;
		}
		BlockPos pos = player.blockPosition();
		// tracked = false: the needle keeps pointing at the spot even though
		// there is no lodestone block there to keep it honest.
		stack.set(DataComponents.LODESTONE_TRACKER,
			new LodestoneTracker(Optional.of(GlobalPos.of(player.level().dimension(), pos)), false));
		player.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.2F);
		player.sendSystemMessage(Component.translatable("perk.pbenchants.waypoint.bound",
			Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
				.withStyle(ChatFormatting.GOLD)), true);
		return true;
	}
}
