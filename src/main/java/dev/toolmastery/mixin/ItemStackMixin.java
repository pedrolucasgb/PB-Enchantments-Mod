package dev.toolmastery.mixin;

import dev.toolmastery.perk.ArmorPerks;
import dev.toolmastery.perk.Indestructible;
import dev.toolmastery.perk.ItemAuthority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The three halves of Indestructible, all on {@link ItemStack} because that is
 * where durability and tool behaviour live:
 *
 * <ol>
 *   <li>damage is clamped one point short of breaking, so vanilla's
 *       {@code applyDamage} never sees {@code isBroken()} and never shrinks the
 *       stack away;</li>
 *   <li>a spent item digs at bare-hand speed;</li>
 *   <li>and it stops counting as the right tool, so blocks that need one drop
 *       nothing — the item is carried, not usable.</li>
 * </ol>
 *
 * <p>Both read-side hooks run on either side: the client animates the break and
 * the server validates it, so they have to agree.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {
	@ModifyVariable(
		method = "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
		at = @At("HEAD"), argsOnly = true)
	private int toolmastery$indestructibleClamp(int damage, int ignored, ServerPlayer player, Consumer<?> onBreak) {
		ItemStack self = (ItemStack) (Object) this;
		// An item its holder has not earned wears not at all: they are not
		// really using it, and it closes the griefing angle where handing
		// someone a tool burns it out for them.
		if (player != null && ItemAuthority.locked(player, self)) {
			return 0;
		}
		return Indestructible.clampDamage(self, damage);
	}

	/**
	 * Padded Lining and Second Skin, on the path armour wears out by. The slot
	 * overload is the armour one specifically, so nothing here has to guess
	 * whether a stack was being worn or swung.
	 */
	@ModifyVariable(
		method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
		at = @At("HEAD"), argsOnly = true)
	private int toolmastery$armourWearsSlower(int amount, int ignored, LivingEntity wearer, EquipmentSlot slot) {
		return ArmorPerks.armourDurability(wearer, (ItemStack) (Object) this, amount);
	}

	/**
	 * Bulwark, on the path a raised shield wears out by — the hand overload,
	 * which is what a blocked hit uses.
	 */
	@ModifyVariable(
		method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)V",
		at = @At("HEAD"), argsOnly = true)
	private int toolmastery$shieldWearsSlower(int amount, int ignored, LivingEntity holder, InteractionHand hand) {
		return ArmorPerks.shieldDurability(holder, (ItemStack) (Object) this, amount);
	}

	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void toolmastery$spentDigsLikeAHand(BlockState state, CallbackInfoReturnable<Float> cir) {
		if (Indestructible.isSpent((ItemStack) (Object) this)) {
			cir.setReturnValue(1.0F); // the bare-hand rate
		}
	}

	@Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void toolmastery$spentHarvestsNothing(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (Indestructible.isSpent((ItemStack) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * A spent armour piece protects for nothing. This is the one funnel every
	 * equipped attribute flows through — {@code LivingEntity} re-reads it each
	 * time a slot's stack changes, and a durability tick <em>is</em> a change —
	 * so cancelling it here strips the armour, toughness and knockback
	 * resistance off a spent piece for whoever wears it: the player (whose
	 * armour bar empties with the attribute, which is the visible half of the
	 * promise), a zombie in a scavenged helmet, a wolf, a horse, a nautilus.
	 * Repairing the piece is a stack change too, so everything comes back.
	 *
	 * <p>Armour slots only: a held item's attack damage is judged by the
	 * attack hooks, not silently zeroed on the way in.
	 */
	@Inject(
		method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
		at = @At("HEAD"), cancellable = true)
	private void toolmastery$spentArmourProtectsNothing(EquipmentSlot slot, BiConsumer<?, ?> consumer,
			CallbackInfo ci) {
		if (slot.isArmor() && Indestructible.isSpent((ItemStack) (Object) this)) {
			ci.cancel();
		}
	}
}
