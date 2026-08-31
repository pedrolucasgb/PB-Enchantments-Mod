package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.skill.SkillNode;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
 * per-holder half the enchantments were missing:
 *
 * <ul>
 *   <li>hold an item carrying a Tool Mastery enchantment you own <em>no</em>
 *       rank of and the whole item goes {@link #locked inert} — bare-hand dig
 *       speed, no drops from blocks that need a tool, bare-hand melee damage,
 *       and no durability spent. The item is intact; it is carried, not
 *       usable.</li>
 *   <li>own a lower rank than the item carries and the effect is
 *       {@link #effectiveLevel clamped} down to your rank. The tool wakes up
 *       further as the tree catches up.</li>
 * </ul>
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

	/** One Tool Mastery enchantment on a stack that the holder has not earned. */
	public record Unmet(Holder<Enchantment> holder, ResourceKey<Enchantment> key, int carried, int owned) {
		/** The enchantment at the rank the item carries — what the player has to go and unlock. */
		public Component name() {
			return Enchantment.getFullname(holder, carried);
		}
	}

	/**
	 * The first enchantment on the stack the player falls short of, or null.
	 *
	 * @param requireFullRank {@code true} asks "may this player own the item at
	 *                        all" — the trade counter's question, where the rank
	 *                        and the price are both on the label and quietly
	 *                        handing over a weaker book would be a bug.
	 *                        {@code false} asks "may this player use the item",
	 *                        where a rank below the item's is fine: it clamps.
	 */
	@Nullable
	public static Unmet firstUnmet(Player player, ItemStack stack, boolean requireFullRank) {
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
			ResourceKey<Enchantment> key = ours(entry.getKey());
			if (key == null) {
				continue; // vanilla enchantments have no unlock to fall short of
			}
			int ownedRank = owned(player, key);
			int needed = requireFullRank ? entry.getIntValue() : 1;
			if (ownedRank < needed) {
				return new Unmet(entry.getKey(), key, entry.getIntValue(), ownedRank);
			}
		}
		return null;
	}

	/**
	 * True when the holder owns no rank at all of something the item carries.
	 * The whole item goes inert, not just our enchantment's effect: "your gear
	 * is only as strong as you are" is one sentence instead of a matrix, and a
	 * gifted Efficiency V netherite pickaxe would otherwise still be most of
	 * the shortcut this closes.
	 */
	public static boolean locked(Player player, ItemStack stack) {
		return firstUnmet(player, stack, false) != null;
	}

	/** Trade-counter rule: the buyer must own the exact rank on the label, or better. */
	public static boolean unbuyable(Player player, ItemStack stack) {
		return firstUnmet(player, stack, true) != null;
	}

	/**
	 * What one of our enchantments is actually worth in this player's hands:
	 * the item's rank clamped down to theirs, and 0 outright when anything else
	 * on the item is locked. Every perk reads this instead of the raw stack
	 * level, which is what makes a borrowed tool wake up rank by rank.
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
		Unmet unmet = firstUnmet(player, stack, false);
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
