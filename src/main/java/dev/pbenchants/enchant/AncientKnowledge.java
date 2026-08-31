package dev.pbenchants.enchant;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Ancient Knowledge — the Archmage capstone: a table that offers what a table
 * was never able to offer.
 *
 * <h2>The three offers become 35, 40 and 45</h2>
 *
 * <p>Vanilla tops out at 30, and 30 is the reason a whole shelf of
 * enchantments is folklore rather than gameplay. Sharpness V wants an
 * enchanting level of 45 before it will even enter the draw; Efficiency V wants
 * 41. On a diamond sword a 30-level offer lands around 36 after vanilla's
 * enchantability roll, so those two are not "rare" at a vanilla table — they
 * are <em>impossible</em>, and every one you have ever seen came off an anvil.
 * Raising the three slots to 35 / 40 / 45 puts them back in the draw, and drags
 * a longer list of enchantments in with them, because vanilla keeps rolling for
 * extra enchantments as long as the level holds up.
 *
 * <p>It only applies at a <b>fully powered table</b> — fifteen bookshelves.
 * Making it unconditional would delete the one ritual the class is built
 * around; this way the capstone raises the ceiling of a proper table instead of
 * excusing you from building one.
 *
 * <p>The XP is a <em>requirement</em>, not a bill: vanilla charges the slot
 * number in levels (1, 2 or 3) and only asks that you <em>have</em> the offered
 * level. That is unchanged — Ancient Knowledge asks you to be level 45, not to
 * spend 45.
 *
 * <h2>And at 45, a chance at a perfect item</h2>
 *
 * <p>The top slot sometimes skips the weighted draw entirely and hands over
 * every enchantment the item can legally carry, each at its maximum level, in a
 * mutually compatible set. {@link #PERFECT_CHANCE} of the time — a chance, not
 * a promise, and rerolling the seed costs a level like it always did.
 *
 * <p>Two guard rails keep it a reward rather than a cheat:
 *
 * <ul>
 *   <li><b>Never above the known maximum.</b> Every level is the enchantment's
 *       own {@code getMaxLevel()}, so a perfect sword is Sharpness V, never a
 *       Sharpness VI that no other route in the game can produce.</li>
 *   <li><b>Only what a table could legally give.</b> The pool is the table's own
 *       pool, filtered to enchantments that treat this item as a primary
 *       target. Mending and the other treasure enchantments are not in it, so
 *       librarians and loot chests keep the job they had.</li>
 * </ul>
 *
 * <p>Which compatible set you get is rolled too. Sharpness or Smite, Fortune or
 * Silk Touch — those are exclusive by design, and a fixed pick would mean every
 * perfect axe in the world was the same axe. Rolling it is done off the same
 * seeded {@link RandomSource} vanilla uses, so the offer stays deterministic for
 * a given seed and the Arcane Insight preview still shows exactly what you are
 * about to get.
 */
public final class AncientKnowledge {
	/** What the three slots ask for once the capstone is in hand. */
	public static final int[] COSTS = {35, 40, 45};

	/** Bookshelves needed before any of this applies — vanilla's own full-power number. */
	public static final int REQUIRED_POWER = 15;

	/** How often the top slot rolls a perfect set instead of a weighted draw. */
	public static final float PERFECT_CHANCE = 0.15F;

	/** The slot the perfect roll can happen in: the one asking for 45. */
	private static final int PERFECT_SLOT = 2;

	private AncientKnowledge() {
	}

	public static boolean owns(Player player) {
		return EnchanterPerks.owns(player, EnchanterPerks.ANCIENT_KNOWLEDGE);
	}

	/**
	 * The level this slot should ask for, or {@code vanilla} when the capstone
	 * does not apply here.
	 */
	public static int costFor(Player player, int slot, int bookshelves, int vanilla) {
		if (vanilla <= 0 || slot < 0 || slot >= COSTS.length) {
			return vanilla;
		}
		if (bookshelves < REQUIRED_POWER || !owns(player)) {
			return vanilla;
		}
		return COSTS[slot];
	}

	/**
	 * Whether this particular offer is a perfect one. Consumes one number from
	 * the seeded random, so the answer is stable for a given enchantment seed —
	 * which is what lets the preview and the click agree.
	 */
	public static boolean perfectRollDue(Player player, int slot, int bookshelves, RandomSource random) {
		if (slot != PERFECT_SLOT || bookshelves < REQUIRED_POWER || !owns(player)) {
			return false;
		}
		return random.nextFloat() < PERFECT_CHANCE;
	}

	/**
	 * Every enchantment in {@code pool} this item can legally carry, at its
	 * maximum level, in a compatible set — with the order rolled so the
	 * exclusive pairs are not always decided the same way.
	 */
	public static List<EnchantmentInstance> perfectRoll(RandomSource random, ItemStack stack,
			List<Holder<Enchantment>> pool) {
		List<Holder<Enchantment>> shuffled = new ArrayList<>(pool.size());
		for (Holder<Enchantment> holder : pool) {
			if (holder.value().isPrimaryItem(stack)) {
				shuffled.add(holder);
			}
		}
		shuffle(random, shuffled);

		List<Holder<Enchantment>> chosen = new ArrayList<>();
		List<EnchantmentInstance> result = new ArrayList<>();
		for (Holder<Enchantment> holder : shuffled) {
			// Greedy is optimal here: vanilla's exclusivity sets are small
			// mutual-exclusion groups, so taking one never costs you two.
			if (!EnchantmentHelper.isEnchantmentCompatible(chosen, holder)) {
				continue;
			}
			chosen.add(holder);
			result.add(new EnchantmentInstance(holder, holder.value().getMaxLevel()));
		}
		return result;
	}

	/** Fisher-Yates off the table's seeded random, because {@code RandomSource} is not a {@code Random}. */
	private static void shuffle(RandomSource random, List<Holder<Enchantment>> entries) {
		for (int index = entries.size() - 1; index > 0; index--) {
			int swap = random.nextInt(index + 1);
			Holder<Enchantment> held = entries.get(index);
			entries.set(index, entries.get(swap));
			entries.set(swap, held);
		}
	}
}
