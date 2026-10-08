package dev.pbenchants.skill;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.perk.PBEnchantsConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The server feature switch: which skill trees are turned off.
 *
 * <p>A disabled tree does not exist as far as play is concerned. It has no tab
 * in the skill screen, its nodes are never owned (so every perk and mixin
 * that asks {@link SkillService#owns} or {@link dev.pbenchants.perk.PerkAccess}
 * goes inert), its gate counters are written into a scratch object that is
 * thrown away, nothing in it can be bought, sold or stamped, its
 * enchantments are refused by the enchanting table, the villager trades, the
 * anvil and loot, and {@code /pbenchants} refuses to name it.
 *
 * <p>The server is the authority. Its set comes from {@code disabled_trees} in
 * {@code config/pbenchants.json}; every skill-state snapshot carries it to the
 * client, which then hides the same tabs whatever its own config says. In a
 * single-player world both halves share this class and agree by construction.
 *
 * <p>{@link #NOT_ON_THIS_BUILD} is the 1.21.1 build floor: the Bow, Armor and
 * Sword trees ride on 26.x-only hooks that were left out of the port, so they
 * stay off even if the config list is emptied.
 */
public final class TreeSwitch {
	/** Trees this build cannot run at all — always disabled, config or not. */
	public static final Set<String> NOT_ON_THIS_BUILD = Set.of("bow", "armor", "sword");

	private static volatile Set<String> disabled = NOT_ON_THIS_BUILD;

	/** Enchantment to the tree whose nodes grant it, built on first use. */
	@Nullable
	private static volatile Map<ResourceKey<Enchantment>, String> enchantTrees;

	private TreeSwitch() {
	}

	public static boolean disabled(@Nullable String treeId) {
		return treeId != null && disabled.contains(treeId);
	}

	public static boolean disabled(SkillTree tree) {
		return disabled(tree.id());
	}

	public static Set<String> disabledIds() {
		return disabled;
	}

	/** The trees players can actually see and use, in tab order. */
	public static List<SkillTree> enabledTrees() {
		List<SkillTree> enabled = new ArrayList<>();
		for (SkillTree tree : SkillTrees.ORDER) {
			if (!disabled(tree)) {
				enabled.add(tree);
			}
		}
		return enabled;
	}

	/** Server side: (re)read from the config. Unknown ids are ignored with a warning. */
	public static void applyConfig() {
		Set<String> fromConfig = new LinkedHashSet<>(PBEnchantsConfig.disabledTrees());
		for (String id : List.copyOf(fromConfig)) {
			if (!SkillTrees.ALL.containsKey(id)) {
				PBEnchants.LOGGER.warn("disabled_trees: unknown tree id '{}' ignored (known: {})", id,
					String.join(", ", SkillTrees.ALL.keySet()));
				fromConfig.remove(id);
			}
		}
		for (String id : NOT_ON_THIS_BUILD) {
			if (!fromConfig.contains(id)) {
				PBEnchants.LOGGER.warn("disabled_trees: '{}' cannot be enabled on the 1.21.1 build "
					+ "(its hooks were not ported) - keeping it disabled", id);
			}
		}
		set(fromConfig);
		PBEnchants.LOGGER.info("PB Enchantments: disabled skill trees = {}", disabled);
	}

	/** Client side: what the server said. The build floor still applies. */
	public static void acceptFromServer(Collection<String> serverDisabled) {
		set(serverDisabled);
	}

	private static void set(Collection<String> ids) {
		Set<String> next = new LinkedHashSet<>(NOT_ON_THIS_BUILD);
		next.addAll(ids);
		disabled = Set.copyOf(next);
	}

	/** The tree that grants an enchantment of ours, or null for anything else. */
	@Nullable
	public static String treeOf(ResourceKey<Enchantment> enchantment) {
		Map<ResourceKey<Enchantment>, String> map = enchantTrees;
		if (map == null) {
			map = new HashMap<>();
			for (SkillTree tree : SkillTrees.ORDER) {
				for (String nodeId : tree.nodes().keySet()) {
					ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(nodeId);
					if (grant != null) {
						map.putIfAbsent(grant.enchantment(), tree.id());
					}
				}
			}
			enchantTrees = map;
		}
		return map.get(enchantment);
	}

	/** False for an enchantment of ours whose tree is switched off. */
	public static boolean enchantmentAllowed(ResourceKey<Enchantment> enchantment) {
		return !disabled(treeOf(enchantment));
	}

	/** Holder flavour of {@link #enchantmentAllowed(ResourceKey)}. */
	public static boolean enchantmentAllowed(net.minecraft.core.Holder<Enchantment> enchantment) {
		return enchantment.unwrapKey().map(TreeSwitch::enchantmentAllowed).orElse(true);
	}
}
