package dev.toolmastery.enchant;

import dev.toolmastery.ToolMastery;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Real data-driven enchantments (data/toolmastery/enchantment/*.json).
 * They exist in the registry like any vanilla enchantment — /enchant works,
 * anvils work, tooltips work. Unlocking a skill node adds one to the player's
 * enchanting-table pool; the node's Enchant action stamps it on a held tool.
 */
public final class ModEnchantments {
	public static final ResourceKey<Enchantment> DIG_RANGE = key("dig_range");
	public static final ResourceKey<Enchantment> SMELT = key("smelt");
	public static final ResourceKey<Enchantment> LOGIC = key("logic");
	public static final ResourceKey<Enchantment> RICH_VEIN = key("rich_vein");
	public static final ResourceKey<Enchantment> ENVIRONMENT = key("environment");
	public static final ResourceKey<Enchantment> MAGMA_TOUCH = key("magma_touch");
	public static final ResourceKey<Enchantment> ANCIENT_FORTUNE = key("ancient_fortune");

	/** Which skill node grants which enchantment level. */
	public record Grant(ResourceKey<Enchantment> enchantment, int level) {
	}

	public static final Map<String, Grant> NODE_GRANTS = Map.ofEntries(
		Map.entry("dig_range_1", new Grant(DIG_RANGE, 1)),
		Map.entry("dig_range_2", new Grant(DIG_RANGE, 2)),
		Map.entry("dig_range_3", new Grant(DIG_RANGE, 3)),
		Map.entry("smelt_1", new Grant(SMELT, 1)),
		Map.entry("smelt_2", new Grant(SMELT, 2)),
		Map.entry("smelt_3", new Grant(SMELT, 3)),
		Map.entry("logic_1", new Grant(LOGIC, 1)),
		Map.entry("logic_2", new Grant(LOGIC, 2)),
		Map.entry("logic_3", new Grant(LOGIC, 3)),
		Map.entry("rich_vein_1", new Grant(RICH_VEIN, 1)),
		Map.entry("rich_vein_2", new Grant(RICH_VEIN, 2)),
		Map.entry("environment", new Grant(ENVIRONMENT, 1)),
		Map.entry("magma_touch", new Grant(MAGMA_TOUCH, 1)),
		Map.entry("ancient_fortune", new Grant(ANCIENT_FORTUNE, 1))
	);

	/**
	 * The enchantments an unlock also adds to the player's enchanting-table
	 * offers. Mirrors data/minecraft/tags/enchantment/in_enchanting_table.json —
	 * the tag is what the table actually reads; this set is what the skill
	 * screen promises. Capstones are deliberately absent from both: they are
	 * earned in the tree, never rolled.
	 */
	public static final Set<ResourceKey<Enchantment>> TABLE_POOL =
		Set.of(DIG_RANGE, SMELT, LOGIC, RICH_VEIN, ENVIRONMENT);

	private ModEnchantments() {
	}

	private static ResourceKey<Enchantment> key(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, path));
	}

	@Nullable
	public static Holder<Enchantment> holder(RegistryAccess registries, ResourceKey<Enchantment> enchantmentKey) {
		return registries.lookupOrThrow(Registries.ENCHANTMENT).get(enchantmentKey).orElse(null);
	}

	@Nullable
	public static Holder<Enchantment> holder(Player player, ResourceKey<Enchantment> enchantmentKey) {
		return holder(player.level().registryAccess(), enchantmentKey);
	}

	/** Level of one of our enchantments on a stack (0 when absent). Works on both sides. */
	public static int level(Player player, ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
		Holder<Enchantment> holder = holder(player, enchantmentKey);
		return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}

	/**
	 * Applies (or upgrades) an enchantment on the given stack when the item
	 * supports it and nothing already on the stack conflicts with it.
	 * Returns true when the stack changed.
	 */
	public static boolean apply(Player player, ItemStack stack, ResourceKey<Enchantment> enchantmentKey, int level) {
		Holder<Enchantment> holder = holder(player, enchantmentKey);
		if (holder == null || EnchantCompat.problem(stack, holder, level) != null) {
			return false;
		}
		EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(holder, level));
		return true;
	}
}
