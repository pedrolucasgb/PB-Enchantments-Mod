package dev.toolmastery.mixin;

import dev.toolmastery.enchant.EnchanterPerks;
import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.network.EnchantPreviewPayload;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.track.EnchantTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

	@Shadow
	@Final
	private Container enchantSlots;

	@Shadow
	@Final
	public int[] costs;

	@Shadow
	private List<EnchantmentInstance> getEnchantmentList(RegistryAccess registryAccess, ItemStack stack,
	                                                     int slot, int cost) {
		throw new AssertionError("shadowed");
	}

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

	// ---------- Enchanter class perks ----------

	/**
	 * Arcane Insight: after the offers are (re)computed, send the player the
	 * full enchantment list behind each slot they can read (rank N reads slots
	 * 1..N). getEnchantmentList is deterministic for a given seed, so calling
	 * it again yields exactly what clicking the slot would apply. Sending an
	 * all-empty payload clears the client overlay when the item is removed.
	 */
	@Inject(method = "slotsChanged", at = @At("TAIL"))
	private void toolmastery$sendInsightPreview(Container container, CallbackInfo ci) {
		if (container != enchantSlots || !(toolmastery$player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		int insight = EnchanterPerks.rankedLevel(serverPlayer, EnchanterPerks.ARCANE_INSIGHT);
		if (insight <= 0) {
			return;
		}
		ItemStack stack = enchantSlots.getItem(0);
		List<List<Component>> slots = new ArrayList<>(EnchantPreviewPayload.SLOT_COUNT);
		for (int slot = 0; slot < EnchantPreviewPayload.SLOT_COUNT; slot++) {
			List<Component> lines = new ArrayList<>();
			if (!stack.isEmpty() && slot < insight && costs[slot] > 0) {
				for (EnchantmentInstance instance : getEnchantmentList(
						serverPlayer.level().registryAccess(), stack, slot, costs[slot])) {
					lines.add(Enchantment.getFullname(instance.enchantment(), instance.level()));
				}
			}
			slots.add(lines);
		}
		ServerPlayNetworking.send(serverPlayer, new EnchantPreviewPayload(slots));
	}

	/**
	 * Inner Focus: the lapis-presence check treats the player as if in
	 * creative, so enchanting works with an empty lapis slot. This is the
	 * first hasInfiniteMaterials call only — the second one guards the XP
	 * level requirement, which stays vanilla. Runs on both sides, matching
	 * the client's pre-click validation.
	 */
	@Redirect(method = "clickMenuButton", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z", ordinal = 0))
	private boolean toolmastery$innerFocusLapisCheck(Player player) {
		return player.hasInfiniteMaterials() || EnchanterPerks.owns(player, EnchanterPerks.INNER_FOCUS);
	}

	/** Inner Focus: skip the lapis consumption on a successful enchant. */
	@Redirect(method = "lambda$clickMenuButton$0", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
	private void toolmastery$innerFocusKeepLapis(ItemStack lapisStack, int amount, LivingEntity entity) {
		if (entity instanceof Player player && EnchanterPerks.owns(player, EnchanterPerks.INNER_FOCUS)) {
			return;
		}
		lapisStack.consume(amount, entity);
	}

	/** Enchanter gates: count each successful table enchant, server-side. */
	@Inject(method = "lambda$clickMenuButton$0", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V",
		shift = At.Shift.AFTER))
	private void toolmastery$trackTableEnchant(ItemStack itemStack, int id, Player player, int levels,
	                                           ItemStack lapisStack, Level level, BlockPos pos, CallbackInfo ci) {
		EnchantTracker.onTableEnchant(player, id, levels);
	}
}
