package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Indestructible — the Enchanter's answer to losing a tool you spent an
 * afternoon enchanting. The item never actually breaks: damage stops one point
 * short, exactly like a vanilla Elytra. What it costs you is the item itself
 * until you repair it — spent, it is inert: a tool mines and harvests as if
 * your hand were empty, a right click does nothing, and a worn armour piece
 * protects for nothing.
 *
 * <p>Detection reads the enchantment straight off the stack's component map
 * rather than through a registry lookup, because {@link ItemStack} has no level
 * to look one up in. Comparing holders by key is enough.
 */
public final class Indestructible {
	private Indestructible() {
	}

	/**
	 * The right-click gate, asked by the use events for whichever stack is
	 * about to act: true swallows the use. Every use item lives on this — a
	 * bow's draw, a crossbow's load, a hoe's till, a brush's brushing — so
	 * without it a spent item kept most of its job. The reason lands on the
	 * action bar from the server side only; the client returns the same
	 * verdict silently so no ghost animation ever starts.
	 */
	public static boolean vetoUse(Player player, ItemStack stack) {
		if (!isSpent(stack)) {
			return false;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(
				Component.translatable("item.pbenchants.spent.use", stack.getHoverName()), true);
		}
		return true;
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
