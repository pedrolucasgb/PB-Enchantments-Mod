package dev.pbenchants.perk;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.apache.commons.lang3.mutable.MutableFloat;

/**
 * The armour half of {@link ItemAuthority}: what a worn piece the wearer has
 * not earned is worth. Nothing.
 *
 * <p>A tool goes inert by digging at the bare-hand rate; a piece of armour goes
 * inert by protecting like bare skin. Vanilla applies armour in exactly two
 * places on the damage path — the armour points and toughness in
 * {@code getDamageAfterArmorAbsorb}, the enchantment protection sum in
 * {@code getDamageAfterMagicAbsorb} — and this class answers both with the
 * inert pieces left out. The piece stays on, keeps its enchantments, takes no
 * wear (that part {@code ItemStackMixin} already does for any locked item) and
 * simply counts for nothing until the rank on its label is bought.
 *
 * <p>Only the four armour slots are read for the attribute half, because that
 * is where armour points come from. The enchantment half walks every slot the
 * way vanilla does, so an inert pickaxe in the main hand is skipped too — it
 * has no protection to give, but the rule is simpler with no exceptions.
 */
public final class ArmorAuthority {
	private ArmorAuthority() {
	}

	/** The first worn piece carrying a rank the wearer has not earned, or {@link ItemStack#EMPTY}. */
	public static ItemStack firstInertPiece(Player player) {
		for (EquipmentSlot slot : ArmorPerks.ARMOR_SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (!piece.isEmpty() && ItemAuthority.locked(player, piece)) {
				return piece;
			}
		}
		return ItemStack.EMPTY;
	}

	/**
	 * Armour points and toughness the inert pieces are adding to the wearer's
	 * attributes, as {@code [armor, toughness]}. Armour items only ever add flat
	 * values, so subtracting them from the attribute total is exact; a modifier
	 * of any other kind on a piece is left alone rather than guessed at.
	 */
	public static float[] inertArmour(Player player) {
		float[] sum = new float[2];
		for (EquipmentSlot slot : ArmorPerks.ARMOR_SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (piece.isEmpty() || !ItemAuthority.locked(player, piece)) {
				continue;
			}
			piece.forEachModifier(slot, (attribute, modifier) -> {
				if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE) {
					return;
				}
				if (attribute.value() == Attributes.ARMOR.value()) {
					sum[0] += (float) modifier.amount();
				} else if (attribute.value() == Attributes.ARMOR_TOUGHNESS.value()) {
					sum[1] += (float) modifier.amount();
				}
			});
		}
		return sum;
	}

	/**
	 * Vanilla's own enchantment protection sum — {@code EnchantmentHelper
	 * .getDamageProtection}, re-walked here — with the inert pieces skipped.
	 * Same slots, same slot-matching rule, same per-enchantment hook, so
	 * Protection, Feather Falling, Blast Protection and this mod's own
	 * data-driven Thermal Weave and Ablative Plating all count on an earned
	 * piece and none of them on an inert one.
	 */
	public static float damageProtection(ServerLevel level, Player player, DamageSource source) {
		MutableFloat total = new MutableFloat(0.0F);
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			ItemStack stack = player.getItemBySlot(slot);
			if (stack.isEmpty() || ItemAuthority.locked(player, stack)) {
				continue;
			}
			for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
				Enchantment enchantment = entry.getKey().value();
				if (enchantment.matchingSlot(slot)) {
					enchantment.modifyDamageProtection(level, entry.getIntValue(), stack, player, source, total);
				}
			}
		}
		return total.floatValue();
	}
}
