package dev.pbenchants.enchant;

import dev.pbenchants.PBEnchants;
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
 * Real data-driven enchantments (data/PBEnchants/enchantment/*.json).
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
	public static final ResourceKey<Enchantment> INDESTRUCTIBLE = key("indestructible");
	public static final ResourceKey<Enchantment> SLIPSTREAM = key("slipstream");

	// Sword — Path of the Blade. Eight of them, because the class covers every
	// weapon that hits: sword, trident, mace, axe-as-weapon and spear.
	public static final ResourceKey<Enchantment> KEEN_EDGE = key("keen_edge");
	public static final ResourceKey<Enchantment> SWEEPING_ARC = key("sweeping_arc");
	public static final ResourceKey<Enchantment> EXECUTIONER = key("executioner");
	public static final ResourceKey<Enchantment> TIDECALLER = key("tidecaller");
	public static final ResourceKey<Enchantment> GRAVITY_WELL = key("gravity_well");
	public static final ResourceKey<Enchantment> PHALANX = key("phalanx");
	public static final ResourceKey<Enchantment> NOSTALGY = key("nostalgy");
	public static final ResourceKey<Enchantment> SUNDERING_BLOW = key("sundering_blow");

	// Armor — Path of the Bulwark. Thermal Weave, Ablative Plating and Thorned
	// Plate carry their whole effect in the data file, using vanilla's own
	// damage_protection and post_attack slots; Bulwark and Kinetic Plating are
	// Java, because shield durability and a fall grace distance have no
	// data-driven effect to hang off.
	public static final ResourceKey<Enchantment> THERMAL_WEAVE = key("thermal_weave");
	public static final ResourceKey<Enchantment> ABLATIVE_PLATING = key("ablative_plating");
	public static final ResourceKey<Enchantment> THORNED_PLATE = key("thorned_plate");
	public static final ResourceKey<Enchantment> BULWARK = key("bulwark");
	public static final ResourceKey<Enchantment> KINETIC_PLATING = key("kinetic_plating");

	// Bow — Path of the Arrow. All four are Java-effect enchantments: distance
	// at impact, arrow gravity, a kill that bounces and a hit that roots have
	// no data-driven slot to hang off, so the data files only carry identity,
	// price and which items accept them.
	public static final ResourceKey<Enchantment> LONG_SHOT = key("long_shot");
	public static final ResourceKey<Enchantment> GALE = key("gale");
	public static final ResourceKey<Enchantment> RICOCHET = key("ricochet");
	public static final ResourceKey<Enchantment> PINNING_SHOT = key("pinning_shot");

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
		Map.entry("indestructible", new Grant(INDESTRUCTIBLE, 1)),
		Map.entry("slipstream_1", new Grant(SLIPSTREAM, 1)),
		Map.entry("slipstream_2", new Grant(SLIPSTREAM, 2)),
		Map.entry("slipstream_3", new Grant(SLIPSTREAM, 3)),
		Map.entry("keen_edge_1", new Grant(KEEN_EDGE, 1)),
		Map.entry("keen_edge_2", new Grant(KEEN_EDGE, 2)),
		Map.entry("keen_edge_3", new Grant(KEEN_EDGE, 3)),
		Map.entry("sweeping_arc_1", new Grant(SWEEPING_ARC, 1)),
		Map.entry("sweeping_arc_2", new Grant(SWEEPING_ARC, 2)),
		Map.entry("executioner_1", new Grant(EXECUTIONER, 1)),
		Map.entry("executioner_2", new Grant(EXECUTIONER, 2)),
		Map.entry("executioner_3", new Grant(EXECUTIONER, 3)),
		Map.entry("tidecaller_1", new Grant(TIDECALLER, 1)),
		Map.entry("tidecaller_2", new Grant(TIDECALLER, 2)),
		Map.entry("gravity_well_1", new Grant(GRAVITY_WELL, 1)),
		Map.entry("gravity_well_2", new Grant(GRAVITY_WELL, 2)),
		Map.entry("phalanx_1", new Grant(PHALANX, 1)),
		Map.entry("phalanx_2", new Grant(PHALANX, 2)),
		Map.entry("nostalgy_1", new Grant(NOSTALGY, 1)),
		Map.entry("nostalgy_2", new Grant(NOSTALGY, 2)),
		Map.entry("nostalgy_3", new Grant(NOSTALGY, 3)),
		Map.entry("nostalgy_4", new Grant(NOSTALGY, 4)),
		Map.entry("sundering_blow_1", new Grant(SUNDERING_BLOW, 1)),
		Map.entry("sundering_blow_2", new Grant(SUNDERING_BLOW, 2)),
		Map.entry("thermal_weave_1", new Grant(THERMAL_WEAVE, 1)),
		Map.entry("thermal_weave_2", new Grant(THERMAL_WEAVE, 2)),
		Map.entry("ablative_plating_1", new Grant(ABLATIVE_PLATING, 1)),
		Map.entry("ablative_plating_2", new Grant(ABLATIVE_PLATING, 2)),
		Map.entry("thorned_plate_1", new Grant(THORNED_PLATE, 1)),
		Map.entry("thorned_plate_2", new Grant(THORNED_PLATE, 2)),
		Map.entry("bulwark_1", new Grant(BULWARK, 1)),
		Map.entry("bulwark_2", new Grant(BULWARK, 2)),
		Map.entry("bulwark_3", new Grant(BULWARK, 3)),
		Map.entry("kinetic_plating", new Grant(KINETIC_PLATING, 1)),
		Map.entry("long_shot_1", new Grant(LONG_SHOT, 1)),
		Map.entry("long_shot_2", new Grant(LONG_SHOT, 2)),
		Map.entry("long_shot_3", new Grant(LONG_SHOT, 3)),
		Map.entry("gale_1", new Grant(GALE, 1)),
		Map.entry("gale_2", new Grant(GALE, 2)),
		Map.entry("ricochet_1", new Grant(RICOCHET, 1)),
		Map.entry("ricochet_2", new Grant(RICOCHET, 2)),
		Map.entry("pinning_shot", new Grant(PINNING_SHOT, 1))
	);

	/**
	 * The enchantments an unlock also adds to the player's enchanting-table
	 * offers. Mirrors data/minecraft/tags/enchantment/in_enchanting_table.json —
	 * the tag is what the table actually reads; this set is what the skill
	 * screen promises.
	 */
	public static final Set<ResourceKey<Enchantment>> TABLE_POOL =
		Set.of(DIG_RANGE, SMELT, LOGIC, RICH_VEIN, ENVIRONMENT, INDESTRUCTIBLE, SLIPSTREAM,
			KEEN_EDGE, SWEEPING_ARC, EXECUTIONER, TIDECALLER, GRAVITY_WELL, PHALANX, NOSTALGY,
			SUNDERING_BLOW, THERMAL_WEAVE, ABLATIVE_PLATING, THORNED_PLATE, BULWARK,
			KINETIC_PLATING, LONG_SHOT, GALE, RICOCHET, PINNING_SHOT);

	/**
	 * Every enchantment the tree hands out, derived from {@link #NODE_GRANTS}
	 * so that a new grant joins it for free. This is the set the per-holder
	 * gates ask about — "is this one of ours?" — and it is deliberately not
	 * {@link #TABLE_POOL}, which is about the enchanting table specifically.
	 */
	public static final Set<ResourceKey<Enchantment>> ALL = NODE_GRANTS.values().stream()
		.map(Grant::enchantment)
		.collect(java.util.stream.Collectors.toUnmodifiableSet());

	private ModEnchantments() {
	}

	private static ResourceKey<Enchantment> key(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(PBEnchants.DATA_NS, path));
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
