package dev.toolmastery.mixin;

import dev.toolmastery.enchant.EnchanterPerks;
import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/**
	 * Logic level 1 trades chop speed for the instant fell: breaking a log
	 * with a Logic I axe is noticeably slower. Levels 2+ chop at normal
	 * speed. Runs on both sides so the client's break progress matches the
	 * server. Sneaking (which disables timber) also disables the slowdown.
	 */
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void toolmastery$logicLevel1Slowdown(BlockState state, CallbackInfoReturnable<Float> cir) {
		Player self = (Player) (Object) this;
		if (!state.is(BlockTags.LOGS) || self.isShiftKeyDown()) {
			return;
		}
		ItemStack held = self.getMainHandItem();
		if (!held.is(ItemTags.AXES)) {
			return;
		}
		if (ModEnchantments.level(self, held, ModEnchantments.LOGIC) == 1) {
			cir.setReturnValue(cir.getReturnValueF() * 0.3f);
		}
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
