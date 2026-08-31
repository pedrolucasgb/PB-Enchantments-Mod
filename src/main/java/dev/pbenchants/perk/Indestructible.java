package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Indestructible — the Enchanter's answer to losing a tool you spent an
 * afternoon enchanting. The item never actually breaks: damage stops one point
 * short, exactly like a vanilla Elytra. What it costs you is the tool itself
 * until you repair it — a spent item mines and harvests as if your hand were
 * empty.
 *
 * <p>Detection reads the enchantment straight off the stack's component map
 * rather than through a registry lookup, because {@link ItemStack} has no level
 * to look one up in. Comparing holders by key is enough.
 */
public final class Indestructible {
	private Indestructible() {
	}

	public static boolean has(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		for (Holder<Enchantment> present : stack.getEnchantments().keySet()) {
			if (present.is(ModEnchantments.INDESTRUCTIBLE)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * True when an Indestructible item has run out: it is sitting on its last
	 * durability point, alive but useless until repaired.
	 *
	 * <p>Vanilla's own {@code isBroken()} cannot answer this — the whole point
	 * of the enchantment is that the damage never reaches the max, so vanilla
	 * never considers the item broken and never destroys it.
	 */
	public static boolean isSpent(ItemStack stack) {
		return stack.isDamageableItem()
			&& stack.getDamageValue() >= stack.getMaxDamage() - 1
			&& has(stack);
	}

	/** The highest damage an Indestructible item may take: one short of breaking. */
	public static int clampDamage(ItemStack stack, int damage) {
		return has(stack) ? Math.min(damage, Math.max(0, stack.getMaxDamage() - 1)) : damage;
	}
}
