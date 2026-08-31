package dev.pbenchants.mixin;

import dev.pbenchants.enchant.AncientKnowledge;
import dev.pbenchants.enchant.EnchanterPerks;
import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.network.EnchantPreviewPayload;
import dev.pbenchants.perk.ArmorPerks;
import dev.pbenchants.perk.BowPerks;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.track.EnchantTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
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
import net.minecraft.world.item.enchantment.Enchantments;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
	/** The pickaxe passive that lifts vanilla Fortune from III to IV. */
	@Unique
	private static final String ANCIENT_FORTUNE = "ancient_fortune";

	/** The sword capstone that lifts vanilla Looting from III to IV. */
	@Unique
	private static final String SPOILS_OF_WAR = "spoils_of_war";

	/** The sword passive that lets an axe carry vanilla Sweeping Edge. */
	@Unique
	private static final String BROAD_SWING = "broad_swing";

	@Unique
	private Player pbenchants$player;

	/**
	 * The slot the offer being built belongs to, and how many bookshelves the
	 * table had when the offers were priced. Ancient Knowledge needs both, and
	 * neither reaches the roll itself — {@code selectEnchantment} is handed only
	 * a level and a candidate list.
	 */
	@Unique
	private int pbenchants$slot;

	@Unique
	private int pbenchants$bookshelves;

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
	private void pbenchants$capturePlayer(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		this.pbenchants$player = inventory.player;
	}

	/** Remembers which slot is being rolled; {@code selectEnchantment} is never told. */
	@Inject(method = "getEnchantmentList", at = @At("HEAD"))
	private void pbenchants$rememberSlot(RegistryAccess registryAccess, ItemStack stack, int slot, int cost,
	                                      CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
		this.pbenchants$slot = slot;
	}

	@Redirect(method = "getEnchantmentList", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;selectEnchantment(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILjava/util/stream/Stream;)Ljava/util/List;"))
	private List<EnchantmentInstance> pbenchants$gatedSelect(RandomSource random, ItemStack stack, int cost,
	                                                          Stream<Holder<Enchantment>> candidates) {
		if (!(pbenchants$player instanceof ServerPlayer serverPlayer)) {
			return EnchantmentHelper.selectEnchantment(random, stack, cost, candidates);
		}

		// 1. Locked Tool Mastery enchantments never enter the roll. Collected
		//    rather than streamed on, because Ancient Knowledge needs a second
		//    pass over the same pool and a stream is spent once.
		List<Holder<Enchantment>> pool = candidates.filter(holder -> {
			if (pbenchants$sweepingOnUnearnedAxe(serverPlayer, holder, stack)) {
				return false;
			}
			ResourceKey<Enchantment> ours = pbenchants$matchOurs(holder);
			return ours == null || SkillService.maxEnchantLevelOwned(serverPlayer, ours) > 0;
		}).toList();

		// 2. The Archmage's lottery, or vanilla's weighted draw. The check
		//    consumes a number off the seeded random either way, so the offer
		//    stays reproducible and the Arcane Insight preview keeps matching.
		List<EnchantmentInstance> rolled =
			AncientKnowledge.perfectRollDue(serverPlayer, pbenchants$slot, pbenchants$bookshelves, random)
				? AncientKnowledge.perfectRoll(random, stack, pool)
				: EnchantmentHelper.selectEnchantment(random, stack, cost, pool.stream());

		// 3. Clamp our levels to what the skill tree has unlocked, vanilla
		//    Fortune to III unless Ancient Fortune has lifted the ceiling, and
		//    everything to its own maximum. The Fortune data file raises
		//    max_level to 4 for everybody, because a data pack cannot be
		//    per-player; this is what makes it a reward.
		List<EnchantmentInstance> result = new ArrayList<>(rolled.size());
		for (EnchantmentInstance instance : rolled) {
			if (instance.enchantment().is(Enchantments.FORTUNE) && instance.level() > 3
					&& !SkillService.owns(serverPlayer, SkillTrees.PICKAXE, ANCIENT_FORTUNE)) {
				result.add(new EnchantmentInstance(instance.enchantment(), 3));
				continue;
			}
			if (instance.enchantment().is(Enchantments.LOOTING) && instance.level() > 3
					&& !SkillService.owns(serverPlayer, SkillTrees.SWORD, SPOILS_OF_WAR)) {
				result.add(new EnchantmentInstance(instance.enchantment(), 3));
				continue;
			}
			if (instance.enchantment().is(Enchantments.PROTECTION) && instance.level() > 4
					&& !SkillService.owns(serverPlayer, SkillTrees.ARMOR, ArmorPerks.AEGIS)) {
				result.add(new EnchantmentInstance(instance.enchantment(), 4));
				continue;
			}
			if (instance.enchantment().is(Enchantments.POWER) && instance.level() > 5
					&& !SkillService.owns(serverPlayer, SkillTrees.BOW, BowPerks.HUNTERS_BOUNTY)) {
				result.add(new EnchantmentInstance(instance.enchantment(), 5));
				continue;
			}
			int allowed = instance.enchantment().value().getMaxLevel();
			ResourceKey<Enchantment> ours = pbenchants$matchOurs(instance.enchantment());
			if (ours != null) {
				int owned = SkillService.maxEnchantLevelOwned(serverPlayer, ours);
				if (owned > 0) {
					allowed = Math.min(allowed, owned);
				}
			}
			result.add(instance.level() > allowed
				? new EnchantmentInstance(instance.enchantment(), allowed)
				: instance);
		}
		return result;
	}

	/**
	 * Ancient Knowledge: the three offers become 35, 40 and 45 at a fully
	 * powered table. Redirected at the pricing call rather than patched
	 * afterwards, so the clue enchantments, the Arcane Insight preview and the
	 * enchantment that is finally applied all read the same number.
	 */
	@Redirect(method = "lambda$slotsChanged$0", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"))
	private int pbenchants$ancientOffer(RandomSource random, int slot, int bookshelves, ItemStack stack) {
		this.pbenchants$bookshelves = bookshelves;
		int vanilla = EnchantmentHelper.getEnchantmentCost(random, slot, bookshelves, stack);
		return AncientKnowledge.costFor(pbenchants$player, slot, bookshelves, vanilla);
	}

	/**
	 * Broad Swing widens {@code #minecraft:enchantable/sweeping} to axes for
	 * everybody, because a data pack cannot be per-player. This is the half that
	 * makes it a reward: without the node, an axe is not offered Sweeping Edge,
	 * and the table fills the slot with something else instead of leaving it
	 * empty.
	 */
	@Unique
	private static boolean pbenchants$sweepingOnUnearnedAxe(ServerPlayer player, Holder<Enchantment> holder,
	                                                         ItemStack stack) {
		return holder.is(Enchantments.SWEEPING_EDGE) && stack.is(ItemTags.AXES)
			&& !SkillService.owns(player, SkillTrees.SWORD, BROAD_SWING);
	}

	@Unique
	private static ResourceKey<Enchantment> pbenchants$matchOurs(Holder<Enchantment> holder) {
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
	private void pbenchants$sendInsightPreview(Container container, CallbackInfo ci) {
		if (container != enchantSlots || !(pbenchants$player instanceof ServerPlayer serverPlayer)) {
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
	private boolean pbenchants$innerFocusLapisCheck(Player player) {
		return player.hasInfiniteMaterials() || EnchanterPerks.owns(player, EnchanterPerks.INNER_FOCUS);
	}

	/** Inner Focus: skip the lapis consumption on a successful enchant. */
	@Redirect(method = "lambda$clickMenuButton$0", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
	private void pbenchants$innerFocusKeepLapis(ItemStack lapisStack, int amount, LivingEntity entity) {
		if (entity instanceof Player player && EnchanterPerks.owns(player, EnchanterPerks.INNER_FOCUS)) {
			return;
		}
		lapisStack.consume(amount, entity);
	}

	/** Enchanter gates: count each successful table enchant, server-side. */
	@Inject(method = "lambda$clickMenuButton$0", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V",
		shift = At.Shift.AFTER))
	private void pbenchants$trackTableEnchant(ItemStack itemStack, int id, Player player, int levels,
	                                           ItemStack lapisStack, Level level, BlockPos pos, CallbackInfo ci) {
		EnchantTracker.onTableEnchant(player, itemStack, id, levels);
	}
}
