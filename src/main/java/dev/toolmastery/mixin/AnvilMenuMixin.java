package dev.toolmastery.mixin;

import dev.toolmastery.enchant.EnchanterPerks;
import dev.toolmastery.perk.CombatPerks;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTree;
import dev.toolmastery.skill.SkillTrees;
import dev.toolmastery.track.EnchantTracker;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
 * <p><b>The raised vanilla ceilings</b> — Mending II (Enchanter), Fortune IV
 * (Pickaxe) and Looting IV (Sword). Each data file raises max_level for
 * everybody, because a data pack cannot be per-player; clamping
 * {@code getMaxLevel} back here for anyone without the node is what makes them
 * rewards. The anvil is the only place Mending II can be made at all, Mending
 * being treasure.
 *
 * <p><b>Broad Swing</b> (Sword tier 2) — a Sweeping Edge book only takes on an
 * axe for someone who has bought the node.
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	@Shadow
	@Final
	private DataSlot cost;

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

	/**
	 * The three vanilla ceilings the mod raises for everybody in its data pack
	 * and clamps back here for anyone without the node that earned them.
	 *
	 * <p>Mending II, Fortune IV and Looting IV are all the same trick and now
	 * all live in the same table. Fortune used to be gated at the enchanting
	 * table only, which left two Fortune III books on an anvil as a way round
	 * the capstone entirely — that hole closes here.
	 */
	@Unique
	private int toolmastery$gatedCeiling(Enchantment enchantment, ResourceKey<Enchantment> key,
	                                     String treeId, String nodeId, int vanillaMax) {
		if (!toolmastery$is(enchantment, key)) {
			return -1;
		}
		SkillTree tree = SkillTrees.byId(treeId);
		boolean earned = tree != null && toolmastery$player instanceof ServerPlayer serverPlayer
			&& SkillService.owns(serverPlayer, tree, nodeId);
		return earned ? enchantment.getMaxLevel() : vanillaMax;
	}

	@Redirect(method = "createResult", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
	private int toolmastery$vanillaCeilings(Enchantment enchantment) {
		int max = enchantment.getMaxLevel();
		if (max <= 1 || toolmastery$player == null) {
			return max;
		}
		int mending = toolmastery$gatedCeiling(enchantment, Enchantments.MENDING,
			"enchanter", EnchanterPerks.GREATER_MENDING, 1);
		if (mending >= 0) {
			return mending;
		}
		int fortune = toolmastery$gatedCeiling(enchantment, Enchantments.FORTUNE,
			"pickaxe", "ancient_fortune", 3);
		if (fortune >= 0) {
			return fortune;
		}
		int looting = toolmastery$gatedCeiling(enchantment, Enchantments.LOOTING,
			"sword", CombatPerks.SPOILS_OF_WAR, 3);
		return looting >= 0 ? looting : max;
	}

	/**
	 * Broad Swing, the anvil half: a Sweeping Edge book only takes on an axe for
	 * someone who has bought the node. The tag says every axe can carry it,
	 * because a data pack cannot ask who is standing at the anvil.
	 *
	 * <p>Judged on the finished result rather than by redirecting
	 * {@code Enchantment.canEnchant}, which is where the obvious hook was:
	 * Fabric API already redirects that exact call for its own
	 * {@code ALLOW_ENCHANTING} event, and a second redirect on it makes theirs
	 * fail its injection check and takes the whole game down at boot. An
	 * unearned combination is refused the way vanilla refuses one — no result,
	 * no price.
	 */
	@Inject(method = "createResult", at = @At("RETURN"))
	private void toolmastery$broadSwingAtTheAnvil(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
		if (result.isEmpty() || !result.is(ItemTags.AXES) || toolmastery$player == null) {
			return;
		}
		Holder<Enchantment> sweeping = toolmastery$holder(Enchantments.SWEEPING_EDGE);
		if (sweeping == null || EnchantmentHelper.getItemEnchantmentLevel(sweeping, result) <= 0) {
			return;
		}
		if (toolmastery$player instanceof ServerPlayer serverPlayer
			&& SkillService.owns(serverPlayer, SkillTrees.SWORD, CombatPerks.BROAD_SWING)) {
			return;
		}
		menu.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
		this.cost.set(0);
	}

	/**
	 * The merge loop hands these redirects an {@code Enchantment}, not its
	 * holder, so the identity check goes through the registry rather than a
	 * {@code Holder#is}.
	 */
	@Unique
	private boolean toolmastery$is(Enchantment enchantment, ResourceKey<Enchantment> key) {
		Holder.Reference<Enchantment> reference = toolmastery$holder(key);
		return reference != null && reference.value() == enchantment;
	}

	@Unique
	private Holder.Reference<Enchantment> toolmastery$holder(ResourceKey<Enchantment> key) {
		return toolmastery$player.level().registryAccess()
			.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.get(key)
			.orElse(null);
	}
}
