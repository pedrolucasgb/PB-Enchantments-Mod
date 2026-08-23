package dev.toolmastery.enchant;

import dev.toolmastery.ToolMastery;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Real data-driven enchantments (data/toolmastery/enchantment/*.json).
 * They exist in the registry like any vanilla enchantment — /enchant works,
 * anvils work, tooltips work. Skill nodes grant them to the player's tool.
 */
public final class ModEnchantments {
	public static final ResourceKey<Enchantment> DIG_RANGE = key("dig_range");
	public static final ResourceKey<Enchantment> MELT = key("melt");
	public static final ResourceKey<Enchantment> LOGIC = key("logic");
	public static final ResourceKey<Enchantment> RICH_VEIN = key("rich_vein");
	public static final ResourceKey<Enchantment> ENVIRONMENT = key("environment");
	public static final ResourceKey<Enchantment> MAGMA_TOUCH = key("magma_touch");

	/** Which skill node grants which enchantment level. */
	public record Grant(ResourceKey<Enchantment> enchantment, int level) {
	}

	public static final Map<String, Grant> NODE_GRANTS = Map.ofEntries(
		Map.entry("dig_range_1", new Grant(DIG_RANGE, 1)),
		Map.entry("dig_range_2", new Grant(DIG_RANGE, 2)),
		Map.entry("dig_range_3", new Grant(DIG_RANGE, 3)),
		Map.entry("melt_1", new Grant(MELT, 1)),
		Map.entry("melt_2", new Grant(MELT, 2)),
		Map.entry("melt_3", new Grant(MELT, 3)),
		Map.entry("logic_1", new Grant(LOGIC, 1)),
		Map.entry("logic_2", new Grant(LOGIC, 2)),
		Map.entry("logic_3", new Grant(LOGIC, 3)),
		Map.entry("rich_vein_1", new Grant(RICH_VEIN, 1)),
		Map.entry("rich_vein_2", new Grant(RICH_VEIN, 2)),
		Map.entry("environment", new Grant(ENVIRONMENT, 1)),
		Map.entry("magma_touch", new Grant(MAGMA_TOUCH, 1))
	);

	private ModEnchantments() {
	}

	private static ResourceKey<Enchantment> key(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, path));
	}

	@Nullable
	public static Holder<Enchantment> holder(Player player, ResourceKey<Enchantment> enchantmentKey) {
		return player.level().registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(enchantmentKey)
			.orElse(null);
	}

	/** Level of one of our enchantments on a stack (0 when absent). Works on both sides. */
	public static int level(Player player, ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
		Holder<Enchantment> holder = holder(player, enchantmentKey);
		return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}

	/** Applies (or upgrades) an enchantment on the given stack when compatible. */
	public static void apply(ServerPlayer player, ItemStack stack, ResourceKey<Enchantment> enchantmentKey, int level) {
		Holder<Enchantment> holder = holder(player, enchantmentKey);
		if (stack.isEmpty() || holder == null || !holder.value().canEnchant(stack)) {
			return;
		}
		if (EnchantmentHelper.getItemEnchantmentLevel(holder, stack) < level) {
			EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(holder, level));
		}
	}

	/**
	 * Applies (or upgrades) an enchantment on the player's main-hand item when
	 * compatible. Returns true if the tool was enchanted.
	 */
	public static boolean applyToMainHand(ServerPlayer player, Grant grant) {
		ItemStack stack = player.getMainHandItem();
		Holder<Enchantment> holder = holder(player, grant.enchantment());
		if (stack.isEmpty() || holder == null || !holder.value().canEnchant(stack)) {
			return false;
		}
		int current = EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
		if (current >= grant.level()) {
			return false;
		}
		EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(holder, grant.level()));
		return true;
	}
}
