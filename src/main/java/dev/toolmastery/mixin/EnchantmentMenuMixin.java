package dev.toolmastery.mixin;

import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.skill.SkillService;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Per-player enchanting table for Tool Mastery enchantments:
 *
 * - Locked enchantments are removed from the candidate pool BEFORE the roll,
 *   so the table fills the slot with something else — no empty offers.
 * - Rolls above the unlocked level are clamped down, not discarded.
 *
 * Vanilla enchantments are untouched; combinations happen through the vanilla
 * bonus-enchantment mechanic. Runs server-side (clues sync automatically).
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
	@Unique
	private Player toolmastery$player;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
	private void toolmastery$capturePlayer(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		this.toolmastery$player = inventory.player;
	}

	@Redirect(method = "getEnchantmentList", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;selectEnchantment(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILjava/util/stream/Stream;)Ljava/util/List;"))
	private List<EnchantmentInstance> toolmastery$gatedSelect(RandomSource random, ItemStack stack, int cost,
	                                                          Stream<Holder<Enchantment>> candidates) {
		if (!(toolmastery$player instanceof ServerPlayer serverPlayer)) {
			return EnchantmentHelper.selectEnchantment(random, stack, cost, candidates);
		}

		// 1. Locked Tool Mastery enchantments never enter the roll.
		Stream<Holder<Enchantment>> unlocked = candidates.filter(holder -> {
			ResourceKey<Enchantment> ours = toolmastery$matchOurs(holder);
			return ours == null || SkillService.maxEnchantLevelOwned(serverPlayer, ours) > 0;
		});

		List<EnchantmentInstance> rolled = EnchantmentHelper.selectEnchantment(random, stack, cost, unlocked);

		// 2. Clamp our levels to what the skill tree has unlocked.
		List<EnchantmentInstance> result = new ArrayList<>(rolled.size());
		for (EnchantmentInstance instance : rolled) {
			ResourceKey<Enchantment> ours = toolmastery$matchOurs(instance.enchantment());
			if (ours != null) {
				int owned = SkillService.maxEnchantLevelOwned(serverPlayer, ours);
				if (owned > 0 && instance.level() > owned) {
					result.add(new EnchantmentInstance(instance.enchantment(), owned));
					continue;
				}
			}
			result.add(instance);
		}
		return result;
	}

	@Unique
	private static ResourceKey<Enchantment> toolmastery$matchOurs(Holder<Enchantment> holder) {
		for (ModEnchantments.Grant grant : ModEnchantments.NODE_GRANTS.values()) {
			if (holder.is(grant.enchantment())) {
				return grant.enchantment();
			}
		}
		return null;
	}
}
