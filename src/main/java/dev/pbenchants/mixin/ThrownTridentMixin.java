package dev.pbenchants.mixin;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.perk.CombatPerks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The thrown half of the trident nodes.
 *
 * <p><b>Tidecaller</b> works by lying to vanilla about the Loyalty on the item.
 * Everything a returning trident does — whether it comes back at all, and how
 * fast — is driven by that one byte, so a rank of Tidecaller is simply a floor
 * under it. The trident returns without Loyalty at rank I and returns at
 * Loyalty II speed at rank II, and none of the return logic had to be
 * duplicated to say so.
 *
 * <p><b>Storm Bearer</b> summons the bolt itself. Channeling's weather
 * requirement is a data-driven condition on the vanilla enchantment, and
 * relaxing it in a data pack would relax it for everyone on the server — so the
 * node checks its own conditions here and calls the lightning down by hand,
 * once per in-game day.
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
	/** Tidecaller: the Loyalty a rank is worth, whatever the item actually says. */
	@Inject(method = "getLoyaltyFromItem", at = @At("RETURN"), cancellable = true)
	private void pbenchants$tidecallerLoyalty(ItemStack stack, CallbackInfoReturnable<Byte> cir) {
		ThrownTrident self = (ThrownTrident) (Object) this;
		if (!(self.getOwner() instanceof ServerPlayer owner)) {
			return;
		}
		int rank = CombatPerks.level(owner, stack, ModEnchantments.TIDECALLER);
		if (rank > cir.getReturnValueB()) {
			cir.setReturnValue((byte) rank);
		}
	}

	/**
	 * Storm Bearer: Channeling without a thunderstorm, once per in-game day. The
	 * rest of Channeling's conditions still stand — the target has to be able to
	 * see the sky, and the trident has to actually carry the enchantment. The
	 * node lifts the weather requirement, not the ritual.
	 */
	@Inject(method = "onHitEntity", at = @At("RETURN"))
	private void pbenchants$stormBearer(EntityHitResult hit, CallbackInfo ci) {
		ThrownTrident self = (ThrownTrident) (Object) this;
		if (!(self.level() instanceof ServerLevel level)
			|| !(self.getOwner() instanceof ServerPlayer owner)
			|| level.isThundering()
			|| !CombatPerks.owns(owner, CombatPerks.STORM_BEARER)
			|| pbenchants$channeling(level, self.getWeaponItem()) <= 0) {
			return;
		}
		BlockPos pos = hit.getEntity().blockPosition();
		if (!level.canSeeSky(pos)) {
			return;
		}
		long day = level.getOverworldClockTime() / 24000L;
		CombatPerks.State state = CombatPerks.state(owner);
		if (state.stormBearerDay == day) {
			return;
		}
		state.stormBearerDay = day;

		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt == null) {
			return;
		}
		bolt.snapTo(Vec3.atBottomCenterOf(pos));
		bolt.setCause(owner);
		level.addFreshEntity(bolt);
		level.playSound(null, self, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.WEATHER, 5.0F, 1.0F);
	}

	@Unique
	private static int pbenchants$channeling(ServerLevel level, ItemStack stack) {
		Holder<Enchantment> channeling = level.registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(Enchantments.CHANNELING)
			.orElse(null);
		return channeling == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(channeling, stack);
	}
}
