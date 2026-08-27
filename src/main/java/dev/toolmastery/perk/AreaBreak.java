package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dig Range — driven by the Dig Range enchantment on the pickaxe:
 *   I   breaks the block below the broken one
 *   II  breaks in a cross on the plane the player is facing
 *   III breaks a full 3x3 on that plane
 * Sneaking disables. Extra blocks respect tool tier and cost durability.
 *
 * <p>Strictly a pickaxe effect: both the block you broke and every extra block
 * must be in {@code #minecraft:mineable/pickaxe}. Without that tag check,
 * {@link ServerPlayer#hasCorrectToolForDrops} alone lets dirt, gravel and sand
 * through — nothing that drops without a required tool ever fails it — and the
 * enchantment turns into a shovel.
 *
 * <p>The Pickaxe capstone {@code enduring_edge} halves what the whole swing
 * costs the tool — see {@link #chargeHalf}.
 *
 * <p>Extras also have to be about as hard as what you actually swung at, so
 * clipping a stone block next to obsidian does not hand you the obsidian. The
 * comparison is in break <em>time for this player</em>, not raw hardness: a
 * block joins the swing when it takes no longer than the block you broke plus
 * {@link #GRACE_TICKS}. That makes the rule loosen exactly the way it should —
 * with enough Efficiency every block is near-instant, the difference between
 * stone and obsidian collapses below the grace, and they do come out together.
 */
public final class AreaBreak {
	private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> false);

	/** Pickaxe tier 5: the whole Dig Range swing costs half the durability. */
	private static final String ENDURING_EDGE = "enduring_edge";

	/**
	 * How much longer than the broken block an extra block may take, in ticks.
	 * 15 (0.75s) is wide enough for the pairs that belong together — stone next
	 * to any ore, netherrack next to quartz, the deepslate boundary — and far
	 * too narrow for obsidian (50 hardness) or ancient debris (30) next to
	 * stone (1.5).
	 */
	private static final float GRACE_TICKS = 15.0F;

	private AreaBreak() {
	}

	public static boolean isAreaBreaking() {
		return BREAKING.get();
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (BREAKING.get() || TimberScheduler.isTimberBreaking()) {
			return;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (serverPlayer.isShiftKeyDown()) {
			return;
		}
		ItemStack pickaxe = serverPlayer.getMainHandItem();
		if (!pickaxe.is(ItemTags.PICKAXES)) {
			return;
		}
		if (!minesWithPickaxe(serverLevel, pos, state, serverPlayer)) {
			return;
		}
		float budgetTicks = ticksToBreak(serverLevel, pos, state, serverPlayer) + GRACE_TICKS;
		int rangeLevel = ItemAuthority.effectiveLevel(serverPlayer, pickaxe, ModEnchantments.DIG_RANGE);
		if (rangeLevel <= 0) {
			return;
		}

		boolean sparing = PerkAccess.owns(serverPlayer, SkillTrees.PICKAXE, ENDURING_EDGE);
		int damageBefore = pickaxe.getDamageValue();
		// The block that started the swing counts, and vanilla has already
		// charged the tool for it before this event ever fires.
		int broken = 1;

		BREAKING.set(true);
		try {
			for (BlockPos target : targets(serverPlayer, pos, rangeLevel)) {
				if (pickaxeAboutToBreak(serverPlayer)) {
					return;
				}
				BlockState targetState = serverLevel.getBlockState(target);
				if (!minesWithPickaxe(serverLevel, target, targetState, serverPlayer)
					|| ticksToBreak(serverLevel, target, targetState, serverPlayer) > budgetTicks) {
					continue;
				}
				if (serverPlayer.gameMode.destroyBlock(target)) {
					broken++;
				}
			}
		} finally {
			BREAKING.set(false);
			if (sparing) {
				chargeHalf(serverPlayer, pickaxe, damageBefore, broken);
			}
		}
	}

	/**
	 * Enduring Edge: the whole swing costs {@code ceil(blocks / 2)} durability
	 * instead of one point per block — 1 per 2 at Dig Range I, 3 per 5 at II,
	 * 5 per 9 at III.
	 *
	 * <p>It refunds rather than skips the damage, because the extras are broken
	 * through {@code destroyBlock} and every hook down that path is entitled to
	 * wear the tool. {@code damageBefore} is read after vanilla charged for the
	 * block that started the swing, so one point of the budget is already
	 * spent; what is left is the allowance for the extras.
	 *
	 * <p>Never charges more than actually was spent, so Unbreaking still helps —
	 * whichever of the two saves more wins, they do not stack into a refund.
	 */
	private static void chargeHalf(ServerPlayer player, ItemStack pickaxe, int damageBefore, int broken) {
		if (player.getMainHandItem() != pickaxe || !pickaxe.isDamageableItem()) {
			return; // the tool was swapped, broke, or never wore in the first place
		}
		int damageAfter = pickaxe.getDamageValue();
		int allowance = (broken + 1) / 2 - 1;
		int charged = Math.min(damageAfter, damageBefore + allowance);
		if (charged < damageAfter) {
			pickaxe.setDamageValue(Math.max(0, charged));
		}
	}

	private static java.util.List<BlockPos> targets(ServerPlayer player, BlockPos pos, int rangeLevel) {
		if (rangeLevel == 1) {
			return java.util.List.of(pos.below());
		}

		// Levels 2-3 work on the plane perpendicular to the player's view:
		// looking mostly up/down -> horizontal plane; otherwise the vertical
		// plane facing the player.
		boolean horizontalPlane = Math.abs(player.getXRot()) > 45.0F;
		boolean alongX = player.getDirection().getStepX() != 0;

		java.util.List<BlockPos> result = new java.util.ArrayList<>(8);
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				if (a == 0 && b == 0) {
					continue;
				}
				if (rangeLevel == 2 && a != 0 && b != 0) {
					continue; // cross shape: skip corners
				}
				BlockPos target;
				if (horizontalPlane) {
					target = pos.offset(a, 0, b);
				} else if (alongX) {
					target = pos.offset(0, a, b);
				} else {
					target = pos.offset(a, b, 0);
				}
				result.add(target);
			}
		}
		return result;
	}

	/**
	 * A block this pickaxe should be digging: pickaxe-mineable, breakable, and
	 * within the tool's tier (a stone pickaxe still stops at obsidian).
	 */
	private static boolean minesWithPickaxe(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
		return !state.isAir()
			&& state.is(BlockTags.MINEABLE_WITH_PICKAXE)
			&& state.getDestroySpeed(level, pos) >= 0
			&& player.hasCorrectToolForDrops(state);
	}

	/**
	 * How many ticks this player needs to break this block, from the very same
	 * progress-per-tick vanilla uses for the cracking animation — so the mod's
	 * own speed passives, Efficiency and Haste all count towards "how hard does
	 * this feel right now".
	 */
	private static float ticksToBreak(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
		float progress = state.getDestroyProgress(player, level, pos);
		if (!(progress > 0.0F)) {
			return Float.MAX_VALUE; // unbreakable for this player (also catches NaN)
		}
		return 1.0F / progress;
	}

	private static boolean pickaxeAboutToBreak(ServerPlayer player) {
		ItemStack pickaxe = player.getMainHandItem();
		return !pickaxe.is(ItemTags.PICKAXES)
			|| (pickaxe.isDamageableItem() && pickaxe.getDamageValue() >= pickaxe.getMaxDamage() - 2);
	}
}
