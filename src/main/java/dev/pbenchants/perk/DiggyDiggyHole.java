package dev.pbenchants.perk;

import dev.pbenchants.network.AbilityStatePayload;
import dev.pbenchants.skill.SkillTrees;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Diggy Diggy Hole — the Ground capstone, and the mod's first held ability.
 *
 * <p>Sneak and right-click with a shovel and everything within reach comes apart,
 * block after block, until you switch it off or something switches it off for you.
 * It obeys the same floor rule as Flat Earth: never a block below the one the
 * player is standing on, and never the block holding them up. You dig the room,
 * not the shaft.
 *
 * <p><b>It costs what swinging would have cost.</b> Each tick hands the aura one
 * tick of mining and every block is charged what vanilla would have charged to
 * break it, so the shovel in your hand is what decides the rate: Efficiency,
 * Haste, Spade's Grip and a locked tool's penalty all land here exactly as they
 * land on a swing, and an Efficiency V netherite shovel clears its whole reach of
 * dirt in a tick. Durability, hunger and drops are charged per block through
 * {@code destroyBlock}, which is also what lets the Digger's Magnet pocket what
 * the aura breaks and what makes a spent Indestructible shovel stop it dead.
 *
 * <p><b>Two callbacks, not one.</b> {@code UseItemCallback} only fires when the
 * click misses every block, and a shovel that hits dirt goes to {@code useOn} and
 * makes a dirt path instead. So the block callback runs too and reports
 * {@code SUCCESS}, which cancels the path. Both are registered ahead of
 * {@code Indestructible.vetoUse}, whose {@code FAIL} would otherwise swallow the
 * toggle on a spent tool with no explanation; spent shovels are refused here
 * instead, with a reason.
 *
 * <p>State is transient and keyed by UUID. A toggle that survived a relog would
 * be a trap, and the whole point of the ability is that the player is holding it
 * on.
 */
public final class DiggyDiggyHole {
	public static final String NODE = "diggy_diggy_hole";

	/**
	 * How much mining the aura is handed per tick: exactly one tick of it, the
	 * same budget a player swinging by hand gets. The shovel's own speed decides
	 * what that buys, so Efficiency, Haste and Spade's Grip all scale the aura
	 * for free and an Efficiency V netherite shovel clears dirt as fast as it
	 * would have swung at it — which is to say instantly.
	 */
	private static final float TICK_BUDGET = 1.0F;

	/**
	 * Ceiling on banked budget, in ticks. Without it, standing in the air for a
	 * minute would buy a minute of instant digging the moment you land.
	 */
	private static final float MAX_CARRY = 20.0F;

	/** Ceiling on one tick's work, so a fast shovel cannot stall the server. */
	private static final int MAX_BREAKS_PER_TICK = 32;

	/** Caps the scan volume regardless of reach modifiers. Not a balance knob. */
	private static final int MAX_RADIUS = 5;

	private static final float EXHAUSTION_PER_BLOCK = 0.025F;

	/** It stops before it can starve you. */
	private static final int MIN_FOOD = 7;

	/**
	 * The shovel is remembered by reference: comparing it to the main hand catches
	 * a hotbar swap, a shulker swap and an off-hand shuffle in one check. Same
	 * trick {@code AreaBreak.chargeHalf} uses to notice the tool changed under it.
	 */
	private record Armed(ItemStack shovel, float carry) {
	}

	private static final Map<UUID, Armed> ACTIVE = new HashMap<>();

	private DiggyDiggyHole() {
	}

	public static boolean isActive(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	/** Sneak + right-click while aiming at a block. Consumes the click, so no dirt path. */
	public static boolean onUseBlock(Player player, InteractionHand hand) {
		return handleUse(player, hand);
	}

	/** Sneak + right-click at the sky. */
	public static boolean onUseItem(Player player, InteractionHand hand) {
		return handleUse(player, hand);
	}

	private static boolean handleUse(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (!player.isShiftKeyDown() || !player.getMainHandItem().is(ItemTags.SHOVELS)) {
			return false;
		}
		return toggle(serverPlayer);
	}

	/**
	 * Returns true when the click was ours — so the caller can cancel the vanilla
	 * shovel action — which includes the refusals, since a player who sneak-clicked
	 * a shovel meant to do this and not to make a path.
	 */
	private static boolean toggle(ServerPlayer player) {
		if (ACTIVE.remove(player.getUUID()) != null) {
			announce(player, "off.manual");
			return true;
		}
		ItemStack shovel = player.getMainHandItem();
		if (!PerkAccess.owns(player, SkillTrees.GROUND, NODE)) {
			return false; // not their ability: let the shovel make its path
		}
		if (ItemAuthority.locked(player, shovel)) {
			ItemAuthority.noticeInertUse(player, shovel);
			return true;
		}
		if (Indestructible.isSpent(shovel)) {
			announce(player, "deny.spent");
			return true;
		}
		ACTIVE.put(player.getUUID(), new Armed(shovel, 0.0F));
		announce(player, "on");
		return true;
	}

	/** Called every tick per online player. */
	public static void tick(ServerPlayer player) {
		Armed armed = ACTIVE.get(player.getUUID());
		if (armed == null) {
			return;
		}
		String off = offReason(player, armed);
		if (off != null) {
			ACTIVE.remove(player.getUUID());
			announce(player, "off." + off);
			return;
		}
		float carry = Math.min(armed.carry() + TICK_BUDGET, MAX_CARRY);

		ServerLevel level = player.level();
		BlockPos support = GroundLevel.support(player);
		double reach = Math.min(player.blockInteractionRange(), MAX_RADIUS);
		double reachSq = reach * reach;
		Vec3 eye = player.getEyePosition();
		int scan = Mth.ceil(reach);

		List<BlockPos> candidates = new ArrayList<>();
		for (int dx = -scan; dx <= scan; dx++) {
			for (int dy = -scan; dy <= scan; dy++) {
				for (int dz = -scan; dz <= scan; dz++) {
					BlockPos target = player.blockPosition().offset(dx, dy, dz);
					if (!GroundLevel.allowed(support, target)) {
						continue; // never below the floor, never the block holding you up
					}
					if (target.distToCenterSqr(eye) > reachSq) {
						continue;
					}
					BlockState state = level.getBlockState(target);
					if (state.isAir() || !state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
						continue;
					}
					if (state.getDestroySpeed(level, target) < 0 || !player.hasCorrectToolForDrops(state)) {
						continue;
					}
					candidates.add(target);
				}
			}
		}
		// Nearest first, so the hole opens out from the player rather than starting
		// with whatever corner the scan happened to reach first.
		candidates.sort(Comparator.comparingDouble(target -> target.distToCenterSqr(eye)));

		// Spend the budget block by block at the price the shovel actually pays
		// for each one. A block costs what vanilla would have charged to swing at
		// it — so a wooden shovel gnaws through gravel a block at a time while an
		// Efficiency V netherite one empties the whole reach in a tick.
		BreakGuard.enter();
		try {
			int broken = 0;
			for (BlockPos target : candidates) {
				if (broken >= MAX_BREAKS_PER_TICK || aboutToBreak(player)) {
					break;
				}
				float cost = ticksToBreak(level, target, player);
				if (cost > carry) {
					// Nearest first also means cheapest-to-reach first, but not
					// cheapest to break; keep looking rather than stopping, so a
					// patch of dirt is not held up by one block of gravel in it.
					continue;
				}
				if (player.gameMode.destroyBlock(target)) {
					carry -= cost;
					broken++;
					player.causeFoodExhaustion(EXHAUSTION_PER_BLOCK);
				}
			}
		} finally {
			BreakGuard.exit();
		}
		// Re-read: the tool may have worn out or been swapped inside the loop.
		Armed still = ACTIVE.get(player.getUUID());
		if (still != null) {
			ACTIVE.put(player.getUUID(), new Armed(still.shovel(), carry));
		}
	}

	/**
	 * How many ticks this player needs to break this block, from the very same
	 * progress-per-tick vanilla uses for the cracking animation — so Efficiency,
	 * Haste, Spade's Grip and a locked tool's penalty all land on the aura
	 * exactly as they land on a swing.
	 */
	private static float ticksToBreak(ServerLevel level, BlockPos pos, ServerPlayer player) {
		float progress = level.getBlockState(pos).getDestroyProgress(player, level, pos);
		if (!(progress > 0.0F)) {
			return Float.MAX_VALUE; // unbreakable for this player (also catches NaN)
		}
		return 1.0F / progress;
	}

	/**
	 * Every way the ability turns itself off, in one place, each returning the
	 * suffix of the message the player gets. Silence would read as a bug.
	 */
	@Nullable
	private static String offReason(ServerPlayer player, Armed armed) {
		ItemStack held = player.getMainHandItem();
		if (held != armed.shovel() || !held.is(ItemTags.SHOVELS)) {
			return "unequipped";
		}
		if (held.isDamageableItem() && held.getDamageValue() >= held.getMaxDamage() - 2) {
			return "broken";
		}
		if (Indestructible.isSpent(held)) {
			return "spent";
		}
		if (ItemAuthority.locked(player, held)) {
			return "locked";
		}
		if (!PerkAccess.owns(player, SkillTrees.GROUND, NODE)) {
			return "not_owned";
		}
		if (player.getFoodData().getFoodLevel() <= MIN_FOOD) {
			return "hungry";
		}
		// getOnPos drifts while falling, so the floor rule is only trustworthy on
		// the ground — and a mid-air aura would carve the ledge out from under you.
		if (!player.onGround()) {
			return "airborne";
		}
		if (player.isDeadOrDying() || player.isRemoved()) {
			return "dead";
		}
		return null;
	}

	private static boolean aboutToBreak(ServerPlayer player) {
		ItemStack shovel = player.getMainHandItem();
		return !shovel.is(ItemTags.SHOVELS)
			|| (shovel.isDamageableItem() && shovel.getDamageValue() >= shovel.getMaxDamage() - 2);
	}

	/** Chat line plus the HUD flag, so the state is legible while it is running. */
	private static void announce(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable("perk.pbenchants.diggy." + key), true);
		ServerPlayNetworking.send(player, new AbilityStatePayload(ACTIVE.containsKey(player.getUUID())));
	}

	/** Disconnect and respawn: the map is keyed by UUID, respawn hands over a new player object. */
	public static void forget(@Nullable ServerPlayer player) {
		if (player != null) {
			ACTIVE.remove(player.getUUID());
		}
	}
}
