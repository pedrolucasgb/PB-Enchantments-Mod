package dev.toolmastery.mixin;

import dev.toolmastery.perk.MiningSpeed;
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
 * Every passive that changes how fast a block breaks: the pickaxe line
 * (Mason's Grip, Obsidian Breaker) and the axe line (Lumberjack's Arms).
 *
 * <p>Deliberately a common mixin, not a client one: the client drives the
 * breaking animation and the server validates the break, so both sides have to
 * compute the same speed or the block "heals" mid-swing. Ownership therefore
 * goes through {@link PerkAccess} instead of a straight attachment read.
 *
 * <p>Both lines share a single injector on purpose. A cancellable callback that
 * calls {@code setReturnValue} returns from the method right there, so a second
 * injector on the same RETURN would silently never run.
 */
@Mixin(Player.class)
public class PlayerMixin {
	@Unique
	private static final String[] TOOLMASTERY$LUMBERJACKS_ARMS = {
		"lumberjacks_arms_1", "lumberjacks_arms_2", "lumberjacks_arms_3"
	};

	@Unique
	private static final float TOOLMASTERY$AXE_SPEED_PER_RANK = 0.15F;

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void toolmastery$applySpeedPassives(BlockState state, CallbackInfoReturnable<Float> cir) {
		float speed = cir.getReturnValue();
		if (speed <= 0.0F) {
			return; // unbreakable, or the wrong tool — nothing to speed up
		}
		Player player = (Player) (Object) this;
		float multiplier = MiningSpeed.multiplier(player, state)
			* toolmastery$lumberjacksArms(player, state);
		if (multiplier != 1.0F) {
			cir.setReturnValue(speed * multiplier);
		}
	}

	@Unique
	private float toolmastery$lumberjacksArms(Player player, BlockState state) {
		if (!state.is(BlockTags.MINEABLE_WITH_AXE) || !player.getMainHandItem().is(ItemTags.AXES)) {
			return 1.0F;
		}
		int rank = PerkAccess.rank(player, SkillTrees.AXE, TOOLMASTERY$LUMBERJACKS_ARMS);
		return rank > 0 ? 1.0F + TOOLMASTERY$AXE_SPEED_PER_RANK * rank : 1.0F;
	}
}
