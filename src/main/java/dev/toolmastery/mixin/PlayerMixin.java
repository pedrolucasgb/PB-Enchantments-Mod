package dev.toolmastery.mixin;

import dev.toolmastery.perk.PerkAccess;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lumberjack's Arms — chop wood faster: +15% (I) / +30% (II) / +45% (III).
 *
 * getDestroySpeed feeds both the client's cracking animation and the server's
 * own progress check, so the bonus has to be visible on both sides — hence
 * {@link PerkAccess} rather than a straight attachment read.
 */
@Mixin(Player.class)
public class PlayerMixin {
	@Unique
	private static final String[] TOOLMASTERY$LUMBERJACKS_ARMS = {
		"lumberjacks_arms_1", "lumberjacks_arms_2", "lumberjacks_arms_3"
	};

	@Unique
	private static final float TOOLMASTERY$SPEED_PER_RANK = 0.15F;

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void toolmastery$lumberjacksArms(BlockState state, CallbackInfoReturnable<Float> cir) {
		float speed = cir.getReturnValue();
		if (speed <= 0.0F || !state.is(BlockTags.MINEABLE_WITH_AXE)) {
			return;
		}
		Player player = (Player) (Object) this;
		if (!player.getMainHandItem().is(ItemTags.AXES)) {
			return;
		}
		int rank = PerkAccess.rank(player, SkillTrees.AXE, TOOLMASTERY$LUMBERJACKS_ARMS);
		if (rank > 0) {
			cir.setReturnValue(speed * (1.0F + TOOLMASTERY$SPEED_PER_RANK * rank));
		}
	}
}
