package dev.toolmastery.mixin;

import dev.toolmastery.enchant.EnchanterPerks;
import dev.toolmastery.track.EnchantTracker;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The anvil, on three counts.
 *
 * <p><b>The Arcanist gate.</b> Only a real merge counts: renaming or repairing
 * with raw material leaves the sacrifice slot without enchantments, and those
 * takes are ignored. Injected at HEAD because onTake is where vanilla clears
 * the inputs — a tick later they are already gone. The inputs are read off the
 * player's open menu because inputSlots lives on ItemCombinerMenu, and a
 * {@code @Shadow} only reaches fields declared in the target class itself.
 *
 * <p><b>Anvil Adept I and II</b> (Enchanter tiers 3 and 4). The price is
 * rewritten at the one point vanilla computes it — the {@code Mth.clamp} that
 * folds the prior work penalty into the bill — so everything downstream reads
 * the discounted number: the "too expensive" wall, the level check in
 * {@code mayPickup}, and the levels {@code onTake} actually deducts. Anvil
 * Adept II then redirects vanilla's creative check on that wall, which is what
 * keeps the result item in the slot at a capped 40 levels instead of blanking
 * it.
 *
 * <p><b>Greater Mending</b> (Enchanter tier 5). The Mending data file raises
 * max_level to 2 for everybody, because a data pack cannot be per-player;
 * clamping {@code getMaxLevel} back to 1 here for anyone who has not bought the
 * capstone is what makes it a reward. The anvil is the only place Mending II
 * can be made — Mending is treasure, so it never rolls at a table.
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	@Unique
	private Player toolmastery$player;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
		at = @At("RETURN"))
	private void toolmastery$capturePlayer(int containerId, Inventory inventory, ContainerLevelAccess access,
	                                       CallbackInfo ci) {
		this.toolmastery$player = inventory.player;
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void toolmastery$trackCombine(Player player, ItemStack result, CallbackInfo ci) {
		if (!(player.containerMenu instanceof AnvilMenu menu)) {
			return;
		}
		ItemStack sacrifice = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		if (!sacrifice.isEmpty() && !EnchantmentHelper.getEnchantmentsForCrafting(sacrifice).isEmpty()) {
			EnchantTracker.onAnvilCombine(player);
		}
	}

	/** Anvil Adept: the whole bill, discounted and then capped, in one place. */
	@Redirect(method = "createResult", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/util/Mth;clamp(JJJ)J"))
	private long toolmastery$anvilAdeptPrice(long value, long min, long max) {
		long vanilla = Mth.clamp(value, min, max);
		if (toolmastery$player == null) {
			return vanilla;
		}
		return EnchanterPerks.anvilCost(toolmastery$player, (int) Math.min(vanilla, Integer.MAX_VALUE));
	}

	/**
	 * Anvil Adept II: the "too expensive" wall only stands for players who have
	 * not bought it. Ordinal 1 is the wall's own creative check — ordinal 0
	 * guards whether an over-max enchantment survives the merge, which stays
	 * vanilla.
	 */
	@Redirect(method = "createResult", at = @At(value = "INVOKE", ordinal = 1,
		target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z"))
	private boolean toolmastery$anvilMasterIgnoresTheWall(Player player) {
		return player.hasInfiniteMaterials() || EnchanterPerks.owns(player, EnchanterPerks.ANVIL_MASTER);
	}

	/** Greater Mending: Mending stops at I unless the capstone lifted the ceiling. */
	@Redirect(method = "createResult", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
	private int toolmastery$mendingCeiling(Enchantment enchantment) {
		int max = enchantment.getMaxLevel();
		if (max <= 1 || toolmastery$player == null || !toolmastery$isMending(enchantment)) {
			return max;
		}
		return EnchanterPerks.owns(toolmastery$player, EnchanterPerks.GREATER_MENDING) ? max : 1;
	}

	/**
	 * The merge loop hands the redirect an {@code Enchantment}, not its holder,
	 * so the identity check goes through the registry rather than a
	 * {@code Holder#is}.
	 */
	@Unique
	private boolean toolmastery$isMending(Enchantment enchantment) {
		Holder.Reference<Enchantment> mending = toolmastery$player.level().registryAccess()
			.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.get(Enchantments.MENDING)
			.orElse(null);
		return mending != null && mending.value() == enchantment;
	}
}
