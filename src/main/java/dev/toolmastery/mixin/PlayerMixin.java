package dev.toolmastery.mixin;

import dev.toolmastery.enchant.EnchanterPerks;
import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.perk.MiningSpeed;
import dev.toolmastery.perk.PerkAccess;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Everything that hangs off the vanilla {@code Player}: the block-breaking
 * speed modifiers (Mason's Grip, Obsidian Breaker, Lumberjack's Arms, the
 * Logic I trade-off) and the Enchanter's XP hook.
 *
 * <p>The speed part is deliberately a common mixin, not a client one: the
 * client drives the breaking animation and the server validates the break, so
 * both sides have to compute the same number or the block "heals" mid-swing.
 * Ownership therefore goes through {@link PerkAccess} instead of a straight
 * attachment read.
 *
 * <p>All the speed modifiers share a single injector on purpose. A cancellable
 * callback that calls {@code setReturnValue} returns from the method right
 * there, so a second injector on the same RETURN would silently never run.
 */
@Mixin(Player.class)
public class PlayerMixin {
	@Unique
	private static final String[] TOOLMASTERY$LUMBERJACKS_ARMS = {
		"lumberjacks_arms_1", "lumberjacks_arms_2", "lumberjacks_arms_3"
	};

	@Unique
	private static final float TOOLMASTERY$AXE_SPEED_PER_RANK = 0.15F;

	/** What a Logic I axe pays for felling the whole tree in one swing. */
	@Unique
	private static final float TOOLMASTERY$LOGIC_1_SLOWDOWN = 0.3F;

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void toolmastery$applySpeedPassives(BlockState state, CallbackInfoReturnable<Float> cir) {
		float speed = cir.getReturnValueF();
		if (speed <= 0.0F) {
			return; // unbreakable, or the wrong tool — nothing to change
		}
		Player player = (Player) (Object) this;
		ItemStack held = player.getMainHandItem();
		float multiplier = MiningSpeed.multiplier(player, state)
			* toolmastery$lumberjacksArms(player, held, state)
			* toolmastery$logicSlowdown(player, held, state);
		if (multiplier != 1.0F) {
			cir.setReturnValue(speed * multiplier);
		}
	}

	@Unique
	private float toolmastery$lumberjacksArms(Player player, ItemStack held, BlockState state) {
		if (!state.is(BlockTags.MINEABLE_WITH_AXE) || !held.is(ItemTags.AXES)) {
			return 1.0F;
		}
		int rank = PerkAccess.rank(player, SkillTrees.AXE, TOOLMASTERY$LUMBERJACKS_ARMS);
		return rank > 0 ? 1.0F + TOOLMASTERY$AXE_SPEED_PER_RANK * rank : 1.0F;
	}

	/**
	 * Logic level 1 trades chop speed for the instant fell: breaking a log with
	 * a Logic I axe is noticeably slower. Levels 2+ chop at normal speed.
	 * Sneaking (which disables timber) also disables the slowdown.
	 */
	@Unique
	private float toolmastery$logicSlowdown(Player player, ItemStack held, BlockState state) {
		if (!state.is(BlockTags.LOGS) || player.isShiftKeyDown() || !held.is(ItemTags.AXES)) {
			return 1.0F;
		}
		return ModEnchantments.level(player, held, ModEnchantments.LOGIC) == 1
			? TOOLMASTERY$LOGIC_1_SLOWDOWN
			: 1.0F;
	}

	/**
	 * Enchanter tree: every XP point gained feeds the collect_xp gate, and the
	 * Scholar passive adds +20% per rank (rounded up, so even 1-point orbs
	 * benefit). Level deductions (negative amounts) are untouched.
	 */
	@ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
	private int toolmastery$scholarXpBonus(int amount) {
		if (!((Object) this instanceof ServerPlayer serverPlayer) || amount <= 0) {
			return amount;
		}
		SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "collect_xp", amount);
		int scholar = EnchanterPerks.rankedLevel(serverPlayer, EnchanterPerks.SCHOLAR);
		if (scholar > 0) {
			amount += Math.max(1, amount * scholar / 5);
		}
		return amount;
	}
}
