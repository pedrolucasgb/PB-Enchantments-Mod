package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the Armor tree needs to answer mid-hit: who owns what, and the
 * plain numbers each node turns into. The mixins that call this hold no policy
 * of their own — they find the hook, this file decides the answer.
 *
 * <h2>Where the tree inserts itself into damage</h2>
 *
 * <p>Issue #28 asked for that to be decided once, in one place, and it was:
 * every node that reduces damage by <em>type</em> — Thermal Weave, Ablative
 * Plating — is a data-driven {@code damage_protection} enchantment, so it lands
 * in vanilla's own protection sum and composes with Protection, Resistance and
 * absorption instead of racing them. Only the effects vanilla has no data hook
 * for live in Java: durability, knockback, blocking, fall grace, and the
 * capstones.
 */
public final class ArmorPerks {
	// --- node ids: keep in sync with the armor tree in SkillTrees ---
	public static final String PADDED_LINING = "padded_lining";
	public static final String SET_SENSE = "set_sense";
	public static final String SHIELD_WALL = "shield_wall";
	public static final String STEADY_STANCE = "steady_stance";
	public static final String FLASHPOINT = "flashpoint";
	public static final String SURE_FOOTING = "sure_footing";
	public static final String SECOND_SKIN = "second_skin";
	public static final String LAST_STAND = "last_stand";
	public static final String REPAIR_RITES = "repair_rites";
	public static final String GUARDIANS_AURA = "guardians_aura";
	public static final String WARDENS_WEIGHT = "wardens_weight";
	public static final String NIGHTPLATE = "nightplate";
	public static final String AEGIS = "aegis";
	public static final String IMMORTAL_LINE = "immortal_line";
	public static final String LIVING_ARMOR = "living_armor";

	/** The four slots this class is about, in the order everything here walks them. */
	public static final EquipmentSlot[] ARMOR_SLOTS = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	/** Durability a rank of Padded Lining saves, as a fraction. */
	private static final float PADDED_LINING_PER_RANK = 0.15F;

	/** Durability a rank of Bulwark saves the shield, as a fraction. */
	private static final float BULWARK_PER_RANK = 0.25F;

	/** The rank of Bulwark at which an axe stops disabling the shield outright. */
	public static final int BULWARK_UNDISABLED = 3;

	/** Knockback Steady Stance takes off a mob's hit. */
	private static final float STEADY_STANCE_RESIST = 0.25F;

	/** Blocks of any fall Kinetic Plating forgives, on top of vanilla's grace. */
	public static final int KINETIC_FREE_BLOCKS = 6;

	/** What is left of a fall once Kinetic Plating has had it. */
	public static final float KINETIC_REMAINDER = 0.5F;

	/** Damage Guardian's Aura takes off an ally's hit. */
	public static final float GUARDIAN_SHARE = 0.10F;

	/** How far Guardian's Aura reaches, in blocks. */
	public static final double GUARDIAN_RANGE = 6.0;

	/** The share of the blocking angle Shield Wall II reports: a smaller angle is a wider cone. */
	public static final float SHIELD_WALL_ARC = 0.6F;

	private ArmorPerks() {
	}

	public static boolean owns(Player player, String nodeId) {
		return PerkAccess.owns(player, SkillTrees.ARMOR, nodeId);
	}

	/** Highest owned rank of a I–III line ("padded_lining" → padded_lining_1..3), 0 when none. */
	public static int rank(Player player, String baseId) {
		return PerkAccess.rank(player, SkillTrees.ARMOR, baseId + "_1", baseId + "_2", baseId + "_3");
	}

	/**
	 * Level of one of our enchantments on a stack, read straight off the
	 * component map. An {@link ItemStack} carries no registry to look a holder
	 * up in, so comparing by key is what works on both sides — the same trick
	 * {@link Indestructible} uses.
	 */
	public static int enchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
		if (stack.isEmpty()) {
			return 0;
		}
		for (var entry : stack.getEnchantments().entrySet()) {
			Holder<Enchantment> present = entry.getKey();
			if (present.is(key)) {
				return entry.getIntValue();
			}
		}
		return 0;
	}

	// ---------- Padded Lining, Bulwark, Second Skin ----------

	/**
	 * The durability damage a piece of armour actually takes. Padded Lining
	 * scales it; Second Skin then stops it one point short of breaking, which
	 * is the same promise Indestructible makes and deliberately the cheap,
	 * armour-only version of it.
	 */
	public static int armourDurability(LivingEntity wearer, ItemStack stack, int amount) {
		if (amount <= 0 || !(wearer instanceof Player player) || !isArmour(stack)) {
			return amount;
		}
		int rank = rank(player, PADDED_LINING);
		if (rank > 0) {
			amount = Math.max(1, Math.round(amount * (1.0F - PADDED_LINING_PER_RANK * rank)));
		}
		if (owns(player, SECOND_SKIN)) {
			amount = stopShortOfBreaking(stack, amount);
		}
		return amount;
	}

	/** The durability damage a raised shield takes, after Bulwark. */
	public static int shieldDurability(LivingEntity holder, ItemStack stack, int amount) {
		if (amount <= 0 || !(holder instanceof Player) || !stack.is(Items.SHIELD)) {
			return amount;
		}
		int rank = enchantLevel(stack, ModEnchantments.BULWARK);
		if (rank <= 0) {
			return amount;
		}
		return Math.max(1, Math.round(amount * (1.0F - BULWARK_PER_RANK * rank)));
	}

	/**
	 * Second Skin: never spend the last point. Returns 0 once the piece is
	 * sitting on it, which is what makes it survive as a warning rather than
	 * vanish mid-fight.
	 */
	private static int stopShortOfBreaking(ItemStack stack, int amount) {
		if (!stack.isDamageableItem()) {
			return amount;
		}
		int room = stack.getMaxDamage() - 1 - stack.getDamageValue();
		return Math.max(0, Math.min(amount, room));
	}

	/** True when a piece is inside Second Skin's warning band. */
	public static boolean nearlySpent(ItemStack stack) {
		return stack.isDamageableItem()
			&& stack.getMaxDamage() - stack.getDamageValue() <= Math.max(1, stack.getMaxDamage() / 10);
	}

	public static boolean isArmour(ItemStack stack) {
		return stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
			|| stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR);
	}

	// ---------- Steady Stance and Warden's Weight ----------

	/**
	 * What is left of a knockback after this player's defensive nodes.
	 *
	 * <p>Warden's Weight is absolute while blocking — the node promises you
	 * cannot be moved, and a promise with a number on it is a different node.
	 * Steady Stance is the always-on quarter underneath it, and the two
	 * deliberately do not multiply into immobility: blocking is already the
	 * ceiling.
	 */
	public static float knockbackFactor(Player player, boolean fromMob) {
		if (owns(player, WARDENS_WEIGHT) && player.isBlocking()) {
			return 0.0F;
		}
		if (fromMob && owns(player, STEADY_STANCE)) {
			return 1.0F - STEADY_STANCE_RESIST;
		}
		return 1.0F;
	}

	// ---------- Kinetic Plating ----------

	public static boolean hasKineticPlating(LivingEntity wearer) {
		return enchantLevel(wearer.getItemBySlot(EquipmentSlot.FEET), ModEnchantments.KINETIC_PLATING) > 0;
	}

	// ---------- Guardian's Aura ----------

	/**
	 * Whether someone standing near a Guardian is covered by them: players and
	 * tamed animals only. The node is about the people and pets beside you, so
	 * a zombie in the same aura gets nothing out of it.
	 */
	public static boolean isProtectableAlly(LivingEntity entity) {
		if (entity instanceof Player) {
			return true;
		}
		return entity instanceof TamableAnimal tamed && tamed.isTame();
	}

	// ---------- Nightplate ----------

	/** The material of a full matching set, or null for a partial or mixed one. */
	@Nullable
	public static String fullSetMaterial(LivingEntity wearer) {
		String material = material(wearer.getItemBySlot(EquipmentSlot.HEAD));
		if (material == null) {
			return null;
		}
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (!material.equals(material(wearer.getItemBySlot(slot)))) {
				return null;
			}
		}
		return material;
	}

	/** The set bonus this player is wearing, or null when Nightplate is not in play. */
	@Nullable
	public static String nightplateMaterial(Player player) {
		return owns(player, NIGHTPLATE) ? fullSetMaterial(player) : null;
	}

	@Nullable
	public static String material(ItemStack stack) {
		if (stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
			|| stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)) {
			return "leather";
		}
		if (stack.is(Items.CHAINMAIL_HELMET) || stack.is(Items.CHAINMAIL_CHESTPLATE)
			|| stack.is(Items.CHAINMAIL_LEGGINGS) || stack.is(Items.CHAINMAIL_BOOTS)) {
			return "chainmail";
		}
		if (stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE)
			|| stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)) {
			return "iron";
		}
		if (stack.is(Items.GOLDEN_HELMET) || stack.is(Items.GOLDEN_CHESTPLATE)
			|| stack.is(Items.GOLDEN_LEGGINGS) || stack.is(Items.GOLDEN_BOOTS)) {
			return "gold";
		}
		if (stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE)
			|| stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)) {
			return "diamond";
		}
		if (stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE)
			|| stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS)) {
			return "netherite";
		}
		return null;
	}

	/** True when all four slots hold something, whatever it is. */
	public static boolean wearsFullSet(LivingEntity wearer) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (wearer.getItemBySlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
