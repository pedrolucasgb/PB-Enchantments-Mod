package dev.pbenchants.mixin;

import dev.pbenchants.enchant.EnchanterPerks;
import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.perk.ArmorPerks;
import dev.pbenchants.perk.BowPerks;
import dev.pbenchants.perk.CombatPerks;
import dev.pbenchants.perk.ItemAuthority;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.skill.XpMath;
import dev.pbenchants.track.EnchantTracker;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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
 * <p><b>Tool Mastery ranks</b> are clamped the same way, and for the same
 * reason: the data file has to declare Dig Range III so the rank can exist, and
 * the anvil would otherwise let two rank IIs be hammered into one.
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
	private Player pbenchants$player;

	/** The taker's experience wallet as onTake was entered — see below. */
	@Unique
	private int pbenchants$pointsBefore;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
		at = @At("RETURN"))
	private void pbenchants$capturePlayer(int containerId, Inventory inventory, ContainerLevelAccess access,
	                                       CallbackInfo ci) {
		this.pbenchants$player = inventory.player;
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void pbenchants$trackCombine(Player player, ItemStack result, CallbackInfo ci) {
		// The bill is paid inside onTake, in levels. What the gate wants is the
		// experience those levels were worth to this player, so the wallet is
		// read on both sides of the call and the difference is the answer.
		this.pbenchants$pointsBefore = XpMath.totalPoints(player);

		if (!(player.containerMenu instanceof AnvilMenu menu)) {
			return;
		}
		ItemStack base = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
		ItemStack sacrifice = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		if (pbenchants$isCombine(base, sacrifice)) {
			EnchantTracker.onAnvilCombine(player);
		}
	}

	@Inject(method = "onTake", at = @At("RETURN"))
	private void pbenchants$trackAnvilSpend(Player player, ItemStack result, CallbackInfo ci) {
		EnchantTracker.onXpPointsSpent(player, this.pbenchants$pointsBefore - XpMath.totalPoints(player));
	}

	/**
	 * Whether this take was a merge rather than a repair with raw material —
	 * vanilla's own fork in {@code createResult}, read back off the slots.
	 *
	 * <p>Anything that puts two items together counts: pickaxe on pickaxe,
	 * book on pickaxe, book on book, enchanted or not. A pickaxe fed diamonds
	 * does not, and neither does a bare rename, which leaves the second slot
	 * empty. Vanilla tests the material repair first and the merge second, so
	 * this reads in the same order — a tool is never both.
	 */
	@Unique
	private static boolean pbenchants$isCombine(ItemStack base, ItemStack sacrifice) {
		if (base.isEmpty() || sacrifice.isEmpty()) {
			return false;
		}
		if (base.isDamageableItem() && base.isValidRepairItem(sacrifice)) {
			return false;
		}
		return sacrifice.has(DataComponents.STORED_ENCHANTMENTS)
			|| (base.is(sacrifice.getItem()) && base.isDamageableItem());
	}

	/** Anvil Adept: the whole bill, discounted and then capped, in one place. */
	@Redirect(method = "createResult", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/util/Mth;clamp(JJJ)J"))
	private long pbenchants$anvilAdeptPrice(long value, long min, long max) {
		long vanilla = Mth.clamp(value, min, max);
		if (pbenchants$player == null) {
			return vanilla;
		}
		return EnchanterPerks.anvilCost(pbenchants$player, (int) Math.min(vanilla, Integer.MAX_VALUE));
	}

	/**
	 * Anvil Adept II: the "too expensive" wall only stands for players who have
	 * not bought it. Ordinal 1 is the wall's own creative check — ordinal 0
	 * guards whether an over-max enchantment survives the merge, which stays
	 * vanilla.
	 */
	@Redirect(method = "createResult", at = @At(value = "INVOKE", ordinal = 1,
		target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z"))
	private boolean pbenchants$anvilMasterIgnoresTheWall(Player player) {
		return player.hasInfiniteMaterials() || EnchanterPerks.owns(player, EnchanterPerks.ANVIL_MASTER);
	}

	/**
	 * The three vanilla ceilings the mod raises for everybody in its data pack
	 * and clamps back here for anyone without the node that earned them.
	 *
	 * <p>Mending II, Fortune IV, Looting IV and Protection V are all the same
	 * trick and now all live in the same table. Fortune used to be gated at the
	 * enchanting table only, which left two Fortune III books on an anvil as a
	 * way round the capstone entirely — that hole closes here.
	 */
	@Unique
	private int pbenchants$gatedCeiling(Enchantment enchantment, ResourceKey<Enchantment> key,
	                                     String treeId, String nodeId, int vanillaMax) {
		if (!pbenchants$is(enchantment, key)) {
			return -1;
		}
		SkillTree tree = SkillTrees.byId(treeId);
		boolean earned = tree != null && pbenchants$player instanceof ServerPlayer serverPlayer
			&& SkillService.owns(serverPlayer, tree, nodeId);
		return earned ? enchantment.getMaxLevel() : vanillaMax;
	}

	@Redirect(method = "createResult", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
	private int pbenchants$vanillaCeilings(Enchantment enchantment) {
		int max = enchantment.getMaxLevel();
		if (max <= 1 || pbenchants$player == null) {
			return max;
		}
		int mending = pbenchants$gatedCeiling(enchantment, Enchantments.MENDING,
			"enchanter", EnchanterPerks.GREATER_MENDING, 1);
		if (mending >= 0) {
			return mending;
		}
		int fortune = pbenchants$gatedCeiling(enchantment, Enchantments.FORTUNE,
			"pickaxe", "ancient_fortune", 3);
		if (fortune >= 0) {
			return fortune;
		}
		int looting = pbenchants$gatedCeiling(enchantment, Enchantments.LOOTING,
			"sword", CombatPerks.SPOILS_OF_WAR, 3);
		if (looting >= 0) {
			return looting;
		}
		int protection = pbenchants$gatedCeiling(enchantment, Enchantments.PROTECTION,
			"armor", ArmorPerks.AEGIS, 4);
		if (protection >= 0) {
			return protection;
		}
		int power = pbenchants$gatedCeiling(enchantment, Enchantments.POWER,
			"bow", BowPerks.HUNTERS_BOUNTY, 5);
		if (power >= 0) {
			return power;
		}
		return pbenchants$masteryCeiling(enchantment, max);
	}

	/**
	 * Tool Mastery ranks are earned, not forged. The enchanting table already
	 * clamps our own enchantments to the rank the tree has unlocked, but the
	 * anvil was still adding two Dig Range II books up into a Dig Range III for
	 * a player who owns rank II — the same hole Fortune III + Fortune III used
	 * to leave, one row up. The ceiling here is the player's own rank, so a
	 * merge tops out where the tree does.
	 *
	 * <p>Never below 1: a player who owns no rank at all is carrying an inert
	 * item anyway ({@link ItemAuthority#locked}), and a ceiling of 0 would strip
	 * the enchantment off the result instead of just refusing the upgrade.
	 *
	 * @return the clamped ceiling, or {@code vanillaMax} when this is not one of ours
	 */
	@Unique
	private int pbenchants$masteryCeiling(Enchantment enchantment, int vanillaMax) {
		for (ResourceKey<Enchantment> ours : ModEnchantments.ALL) {
			if (pbenchants$is(enchantment, ours)) {
				return Math.max(1, ItemAuthority.owned(pbenchants$player, ours));
			}
		}
		return vanillaMax;
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
	private void pbenchants$broadSwingAtTheAnvil(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
		if (result.isEmpty() || !result.is(ItemTags.AXES) || pbenchants$player == null) {
			return;
		}
		Holder<Enchantment> sweeping = pbenchants$holder(Enchantments.SWEEPING_EDGE);
		if (sweeping == null || EnchantmentHelper.getItemEnchantmentLevel(sweeping, result) <= 0) {
			return;
		}
		if (pbenchants$player instanceof ServerPlayer serverPlayer
			&& SkillService.owns(serverPlayer, SkillTrees.SWORD, CombatPerks.BROAD_SWING)) {
			return;
		}
		menu.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
		this.cost.set(0);
	}

	/**
	 * Endless Quiver, the anvil half: Infinity and Mending only share a bow for
	 * someone who has bought the node. The data pack empties vanilla's
	 * {@code exclusive_set/bow} tag for everybody — a tag cannot ask who is
	 * hammering — and this puts the wall back for anyone else, the same shape
	 * as Broad Swing above.
	 */
	@Inject(method = "createResult", at = @At("RETURN"))
	private void pbenchants$endlessQuiverAtTheAnvil(CallbackInfo ci) {
		AnvilMenu menu = (AnvilMenu) (Object) this;
		ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
		if (result.isEmpty() || !result.is(net.minecraft.world.item.Items.BOW) || pbenchants$player == null) {
			return;
		}
		Holder<Enchantment> infinity = pbenchants$holder(Enchantments.INFINITY);
		Holder<Enchantment> mending = pbenchants$holder(Enchantments.MENDING);
		if (infinity == null || mending == null
			|| EnchantmentHelper.getItemEnchantmentLevel(infinity, result) <= 0
			|| EnchantmentHelper.getItemEnchantmentLevel(mending, result) <= 0) {
			return;
		}
		if (pbenchants$player instanceof ServerPlayer serverPlayer
			&& SkillService.owns(serverPlayer, SkillTrees.BOW, BowPerks.ENDLESS_QUIVER)) {
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
	private boolean pbenchants$is(Enchantment enchantment, ResourceKey<Enchantment> key) {
		Holder.Reference<Enchantment> reference = pbenchants$holder(key);
		return reference != null && reference.value() == enchantment;
	}

	@Unique
	private Holder.Reference<Enchantment> pbenchants$holder(ResourceKey<Enchantment> key) {
		return pbenchants$player.level().registryAccess()
			.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.get(key)
			.orElse(null);
	}
}
