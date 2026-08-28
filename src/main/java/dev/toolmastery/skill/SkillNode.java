package dev.toolmastery.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A node in a skill tree, and the two things a player can spend on it.
 *
 * <p><b>Unlock</b> is the one-off purchase: {@code unlockCost} XP levels plus
 * {@code materials}. It opens the node — a passive starts working the moment it
 * is unlocked, an enchantment joins the player's enchanting-table pool and
 * becomes imbuable from the skill screen.
 *
 * <p><b>Enchant</b> only exists for {@link SkillType#ENCHANTMENT} nodes: it
 * stamps the enchantment onto the item in the player's hand for
 * {@code enchantCost} whole XP levels, as many times as they like. It is
 * deliberately much pricier than the unlock so the enchanting table stays the
 * cheaper route.
 *
 * <p>Adding a node is one entry in {@link SkillTrees}: a factory call, then the
 * optional {@code .costing(...) / .enchantFor(...) / .icon(...) / .future()}
 * modifiers. Everything the skill screen draws — the icon tile, the type badge,
 * the connector to its prerequisite — comes off this record, so no GUI code has
 * to learn about the new node.
 *
 * @param id            unique within its tree, snake_case (e.g. "smelt_2")
 * @param tier          0-based tier index this node belongs to
 * @param unlockCost    XP levels consumed on unlock
 * @param materials     items consumed on unlock, alongside the XP
 * @param enchantCost   XP levels per direct enchant; 0 when the node has none
 * @param requires      node id that must be unlocked first, or null
 * @param exclusiveWith node id that locks this one when unlocked (capstone choice), or null
 * @param type          what kind of unlock this grants
 * @param implemented   false while the effect ships in a later update — not unlockable yet
 * @param icon          item drawn on the node tile, or null to fall back on {@link #iconStack()}
 * @param pveOnly       the effect never fires against another player
 * @param requiresAll   only unlockable once every other node of the tree is owned
 */
public record SkillNode(
	String id,
	int tier,
	int unlockCost,
	List<MaterialCost> materials,
	int enchantCost,
	@Nullable String requires,
	@Nullable String exclusiveWith,
	SkillType type,
	boolean implemented,
	@Nullable Item icon,
	boolean pveOnly,
	boolean requiresAll
) {
	public SkillNode {
		materials = List.copyOf(materials);
	}

	public static SkillNode of(String id, int tier, int unlockCost, SkillType type) {
		return new SkillNode(id, tier, unlockCost, List.of(), 0, null, null, type, true, null, false, false);
	}

	public static SkillNode chained(String id, int tier, int unlockCost, String requires, SkillType type) {
		return new SkillNode(id, tier, unlockCost, List.of(), 0, requires, null, type, true, null, false, false);
	}

	public static SkillNode capstone(String id, int tier, int unlockCost, String exclusiveWith, SkillType type) {
		return new SkillNode(id, tier, unlockCost, List.of(), 0, null, exclusiveWith, type, true, null, false, false);
	}

	/** The material half of the unlock price. */
	public SkillNode costing(MaterialCost... materials) {
		return new SkillNode(id, tier, unlockCost, List.of(materials), enchantCost, requires, exclusiveWith,
			type, implemented, icon, pveOnly, requiresAll);
	}

	/** Enables the repeatable Enchant action at the given price in XP levels. */
	public SkillNode enchantFor(int levels) {
		return new SkillNode(id, tier, unlockCost, materials, levels, requires, exclusiveWith,
			type, implemented, icon, pveOnly, requiresAll);
	}

	/** Marks this node as coming in a future update: visible in the tree but locked. */
	public SkillNode future() {
		return new SkillNode(id, tier, unlockCost, materials, enchantCost, requires, exclusiveWith,
			type, false, icon, pveOnly, requiresAll);
	}

	/** The item drawn on this node's tile in the skill screen. */
	public SkillNode icon(Item icon) {
		return new SkillNode(id, tier, unlockCost, materials, enchantCost, requires, exclusiveWith,
			type, implemented, icon, pveOnly, requiresAll);
	}

	/**
	 * Marks a node whose effect never applies to another player. The check is a
	 * single one in {@code CombatPerks} rather than a condition per effect, and
	 * the skill screen puts the fact on the card — a PvP server that installs
	 * the mod should not have to find this out in a fight.
	 */
	public SkillNode pve() {
		return new SkillNode(id, tier, unlockCost, materials, enchantCost, requires, exclusiveWith,
			type, implemented, icon, true, requiresAll);
	}

	/**
	 * Marks the one node in a tree that opens only once every other node in it
	 * is owned — the end of the class rather than a choice inside it. Capstones
	 * a player deliberately passed over (the losing half of an
	 * {@code exclusiveWith} pair) do not count against it, or a pick-one choice
	 * would make this unreachable forever.
	 */
	public SkillNode endOfTree() {
		return new SkillNode(id, tier, unlockCost, materials, enchantCost, requires, exclusiveWith,
			type, implemented, icon, pveOnly, true);
	}

	/**
	 * What the skill screen draws on the node tile: the icon this node asked
	 * for, or — for a node that has not picked one yet — the first material of
	 * its price, and failing that a stand-in for its type. A new node is
	 * therefore never iconless, it just looks generic until someone chooses.
	 */
	public ItemStack iconStack() {
		if (icon != null) {
			return new ItemStack(icon);
		}
		for (MaterialCost material : materials) {
			if (material.item() != null) {
				return new ItemStack(material.item());
			}
		}
		return new ItemStack(switch (type) {
			case ENCHANTMENT -> Items.ENCHANTED_BOOK;
			case PASSIVE -> Items.NETHER_STAR;
			case ACTIVE -> Items.BLAZE_ROD;
			case ITEM -> Items.CHEST;
		});
	}

	/** True when this node offers the repeatable "enchant what I'm holding" action. */
	public boolean enchantable() {
		return type == SkillType.ENCHANTMENT && enchantCost > 0;
	}

	public Component displayName() {
		return displayName(id);
	}

	/**
	 * "dig_range_2" -> "Dig Range II", "miners_magnet" -> "Miner's Magnet".
	 * Takes a raw id so it also names the {@link #requires()} /
	 * {@link #exclusiveWith()} pointers, which are ids and not nodes.
	 */
	public static Component displayName(String nodeId) {
		String base = baseId(nodeId);
		Component name = Component.translatable("node.toolmastery." + base);
		if (base.length() == nodeId.length()) {
			return name;
		}
		return Component.empty().append(name).append(" " + roman(nodeId.charAt(nodeId.length() - 1) - '0'));
	}

	/** Strips the "_2" rank suffix of a chained node id. */
	public static String baseId(String nodeId) {
		int lastUnderscore = nodeId.lastIndexOf('_');
		if (lastUnderscore > 0 && lastUnderscore == nodeId.length() - 2
			&& Character.isDigit(nodeId.charAt(nodeId.length() - 1))) {
			return nodeId.substring(0, lastUnderscore);
		}
		return nodeId;
	}

	public static String roman(int rank) {
		return switch (rank) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> String.valueOf(rank);
		};
	}
}
