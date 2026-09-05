package dev.pbenchants.perk;

import dev.pbenchants.enchant.EnchanterPerks;
import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.skill.SkillNode;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gear is only as strong as the player holding it has earned.
 *
 * <p>Every perk effect used to read the item and nothing else, so a gifted
 * Dig Range III pickaxe skipped ten hours of progression. This class is the
 * per-holder half the enchantments were missing.
 *
 * <p>The rule is one sentence: <b>hold a rank you have not unlocked and the
 * whole item goes {@link #locked inert}</b> — bare-hand dig speed, no drops
 * from blocks that need a tool, bare-hand melee damage, no armour points, no
 * Protection, and no durability spent. The item is intact; it is carried, not
 * usable. It does not matter how it got into your hands — enchanted at a
 * table, bought off a librarian, hammered together on an anvil or handed over
 * by a player who did earn it.
 *
 * <p>It is the exact rank on the label that has to be earned, not merely some
 * rank of the same enchantment. Own Dig Range II, pick up a Dig Range III
 * pickaxe and it is a stick until you buy rank III: the tool advertises the
 * tier you have not reached instead of quietly handing you two thirds of it.
 * That also makes the trade counter and the swing agree — a book you may not
 * buy is a book you could not have used.
 *
 * <p><b>The raised vanilla ceilings follow the same rule.</b> The data pack
 * lifts Fortune to IV, Looting to IV, Protection to V, Power to VI and Mending
 * to II for everybody, because a data pack cannot be per-player; the node that
 * earns each of them is what makes it a reward. Until 0.8.4 the table and the
 * anvil rewrote those ranks downward for anyone without the node — a
 * Protection V chestplate came off the anvil as Protection IV, and worked. Now
 * the rank stays on the label and the piece is inert until the node is bought,
 * exactly as a Dig Range III pickaxe is for a rank II player. See
 * {@link #CEILINGS}.
 *
 * <p>Both answers have to match on the client and the server: the client draws
 * the cracking animation and the server validates the break, so a disagreement
 * makes blocks heal mid-swing. Ownership therefore goes through
 * {@link PerkAccess}, which already reads the attachment server-side and the
 * synced snapshot client-side, rather than touching either directly.
 *
 * <p>Creative players bypass everything — otherwise testing every other feature
 * means grinding the tree first.
 */
public final class ItemAuthority {
	/** How long a player goes without being told again that their gear is inert. */
	private static final int NOTICE_COOLDOWN_TICKS = 100;

	/** One node that grants {@code level} of some enchantment. */
	private record Granting(SkillTree tree, String nodeId, int level) {
	}

	/**
	 * One vanilla enchantment whose ceiling the data pack raises, and the node
	 * that earns the raised rank. {@code vanillaMax} is the rank anyone may hold
	 * without asking the tree; anything above it is inert until the node on the
	 * matching tree is bought.
	 */
	public record Ceiling(ResourceKey<Enchantment> key, int vanillaMax, String treeId, String nodeId) {
		/** True when this player has bought the node that lifts the ceiling. */
		public boolean earned(Player player) {
			SkillTree tree = SkillTrees.byId(treeId);
			return tree != null && PerkAccess.owns(player, tree, nodeId);
		}

		/** The highest rank the player may be handed at a table or anvil: {@code raisedMax} once earned, vanilla's otherwise. */
		public int ceilingFor(Player player, int raisedMax) {
			return earned(player) ? raisedMax : vanillaMax;
		}
	}

	/** Every vanilla ceiling the mod raises — keep in step with the data files under {@code data/minecraft/enchantment}. */
	public static final List<Ceiling> CEILINGS = List.of(
		new Ceiling(Enchantments.MENDING, 1, "enchanter", EnchanterPerks.GREATER_MENDING),
		new Ceiling(Enchantments.FORTUNE, 3, "pickaxe", "ancient_fortune"),
		new Ceiling(Enchantments.LOOTING, 3, "sword", CombatPerks.SPOILS_OF_WAR),
		new Ceiling(Enchantments.PROTECTION, 4, "armor", ArmorPerks.AEGIS),
		new Ceiling(Enchantments.POWER, 5, "bow", BowPerks.HUNTERS_BOUNTY)
	);

	/**
	 * enchantment to every node that grants it, built once. The naive form of
	 * this query walks every node of every tree; it runs inside
	 * {@code getDestroySpeed}, which is asked about every block on every frame.
	 */
	@Nullable
	private static Map<ResourceKey<Enchantment>, List<Granting>> index;

	/** Server-side rate limit for the "this is inert" nudge, keyed by player id. */
	private static final Map<UUID, Long> lastNotice = new HashMap<>();

	private ItemAuthority() {
	}

	private static Map<ResourceKey<Enchantment>, List<Granting>> index() {
		Map<ResourceKey<Enchantment>, List<Granting>> built = index;
		if (built != null) {
			return built;
		}
		built = new HashMap<>();
		for (SkillTree tree : SkillTrees.ALL.values()) {
			for (SkillNode node : tree.nodes().values()) {
				ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(node.id());
				if (grant != null) {
					built.computeIfAbsent(grant.enchantment(), key -> new ArrayList<>())
						.add(new Granting(tree, node.id(), grant.level()));
				}
			}
		}
		index = built;
		return built;
	}

	/** Highest rank of one of our enchantments this player owns, on either side. 0 when locked. */
	public static int owned(Player player, ResourceKey<Enchantment> enchantmentKey) {
		int max = 0;
		for (Granting granting : index().getOrDefault(enchantmentKey, List.of())) {
			if (granting.level() > max && PerkAccess.owns(player, granting.tree(), granting.nodeId())) {
				max = granting.level();
			}
		}
		return max;
	}

	/** The enchantment key behind a holder when it is one of ours, else null. */
	@Nullable
	public static ResourceKey<Enchantment> ours(Holder<Enchantment> holder) {
		for (ResourceKey<Enchantment> key : ModEnchantments.ALL) {
			if (holder.is(key)) {
				return key;
			}
		}
		return null;
	}

	/** The raised vanilla ceiling behind a holder, or null when vanilla's own maximum is the whole story. */
	@Nullable
	public static Ceiling ceiling(Holder<Enchantment> holder) {
		for (Ceiling ceiling : CEILINGS) {
			if (holder.is(ceiling.key())) {
				return ceiling;
			}
		}
		return null;
	}

	/**
	 * One enchantment on a stack that the holder has not earned.
	 *
	 * @param requirement what the player has to go and unlock, ready to print:
	 *                    the enchantment at the rank the item carries for one of
	 *                    ours, the node (and the rank waiting on it) for a raised
	 *                    vanilla ceiling
	 */
	public record Unmet(Holder<Enchantment> holder, int carried, Component requirement) {
		public Component name() {
			return requirement;
		}
	}

	/**
	 * The first enchantment on the stack the player falls short of, or null.
	 *
	 * <p>"Falls short" means the player owns a lower rank than the item carries,
	 * or none at all — the same question at the trade counter and in the hand.
	 * For a raised vanilla ceiling it means the item carries a rank above
	 * vanilla's maximum and the node that lifts it is not bought.
	 */
	@Nullable
	public static Unmet firstUnmet(Player player, ItemStack stack) {
		if (stack.isEmpty() || player.hasInfiniteMaterials()) {
			return null;
		}
		// Books keep their enchantments in stored_enchantments, tools in
		// enchantments; this is the one getter that reads whichever applies.
		ItemEnchantments carried = EnchantmentHelper.getEnchantmentsForCrafting(stack);
		if (carried.isEmpty()) {
			return null;
		}
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : carried.entrySet()) {
			Holder<Enchantment> holder = entry.getKey();
			int level = entry.getIntValue();
			ResourceKey<Enchantment> key = ours(holder);
			if (key != null) {
				if (owned(player, key) < level) {
					return new Unmet(holder, level, Enchantment.getFullname(holder, level));
				}
				continue;
			}
			Ceiling ceiling = ceiling(holder);
			if (ceiling != null && level > ceiling.vanillaMax() && !ceiling.earned(player)) {
				return new Unmet(holder, level, ceilingRequirement(ceiling, holder, level));
			}
			// Every other vanilla enchantment has no unlock to fall short of.
		}
		return null;
	}

	/** "Aegis (Protection V)": the node to buy, and the rank that is waiting on it. */
	private static Component ceilingRequirement(Ceiling ceiling, Holder<Enchantment> holder, int level) {
		MutableComponent rank = Enchantment.getFullname(holder, level).copy();
		rank.setStyle(Style.EMPTY); // inherit the line's colour instead of vanilla's grey
		return Component.translatable("node.pbenchants." + ceiling.nodeId())
			.append(" (").append(rank).append(")");
	}

	/**
	 * True when the item carries a rank this holder has not unlocked. The whole
	 * item goes inert, not just our enchantment's effect: "your gear is only as
	 * strong as you are" is one sentence instead of a matrix, and a gifted
	 * Efficiency V netherite pickaxe would otherwise still be most of the
	 * shortcut this closes.
	 */
	public static boolean locked(Player player, ItemStack stack) {
		return firstUnmet(player, stack) != null;
	}

	/**
	 * Trade-counter rule: the buyer must own the exact rank on the label, or
	 * better. The same test as {@link #locked} — deliberately, since a book you
	 * may not buy would be a book you could not have used — but kept under its
	 * own name because the two callers are asking different questions.
	 */
	public static boolean unbuyable(Player player, ItemStack stack) {
		return firstUnmet(player, stack) != null;
	}

	/**
	 * What one of our enchantments is actually worth in this player's hands: the
	 * rank on the item, or 0 when anything the item carries is above what the
	 * holder has unlocked. Every perk reads this instead of the raw stack level,
	 * which is what makes an unearned tool dig like a bare hand.
	 *
	 * <p>The clamp on the way out is belt and braces — {@link #locked} has
	 * already refused anything above the player's rank — so that a future caller
	 * that reaches this without the lock check cannot hand out a rank either.
	 */
	public static int effectiveLevel(Player player, ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
		int carried = ModEnchantments.level(player, stack, enchantmentKey);
		if (carried <= 0 || player.hasInfiniteMaterials()) {
			return carried;
		}
		if (locked(player, stack)) {
			return 0;
		}
		return Math.min(carried, owned(player, enchantmentKey));
	}

	/**
	 * Tells the player, now and then, that the thing in their hand is inert.
	 * Silent degradation reads as a bug, and the tooltip only reaches someone
	 * who is already hovering the item. Server-side; the rate limit is per
	 * player, not per swing.
	 */
	public static void noticeInertUse(Player player, ItemStack stack) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		Unmet unmet = firstUnmet(player, stack);
		if (unmet == null) {
			return;
		}
		long now = player.level().getGameTime();
		Long previous = lastNotice.get(player.getUUID());
		if (previous != null && now - previous < NOTICE_COOLDOWN_TICKS) {
			return;
		}
		lastNotice.put(player.getUUID(), now);
		serverPlayer.sendSystemMessage(
			Component.translatable("item.pbenchants.locked.use", stack.getHoverName(), unmet.name()), true);
	}
}
