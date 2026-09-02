package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Smelt — chance to smelt ore drops on the spot:
 *   I: 25% · II: 50% · III: 100% (per item)
 *
 * Breaks are queued and the drops are converted at the END of the same server
 * tick, after every drop entity has actually spawned — scanning during the
 * break event misses drops that spawn late.
 *
 * <p>Every drop is rolled <b>once per tick</b>, no matter how many queued breaks
 * it sits next to. The scan is positional — a sphere around each broken block —
 * and Rich Vein or Dig Range queue one break per block, so without the claim set
 * below a single raw-iron drop in the middle of a vein was rolled once for every
 * neighbour that was broken with it. Eight rolls at 50% is 99.6%, which is why
 * Smelt II looked like Smelt III on anything but a lone ore.
 *
 * <p>Smelting is a furnace, so it pays like one: an ingot that came out of a
 * swing hands over the same experience the recipe would have. Fractions are
 * carried the way {@code AbstractFurnaceBlockEntity} carries them — a 0.7 that
 * lands 70% of the time — so a hundred raw iron is worth 70 points either way.
 */
public final class SmeltHandler {
	private static final Map<Item, Item> ORE_SMELTS = Map.of(
		Items.RAW_COPPER, Items.COPPER_INGOT,
		Items.RAW_IRON, Items.IRON_INGOT,
		Items.RAW_GOLD, Items.GOLD_INGOT
	);

	/** What each recipe pays per item in a real furnace. */
	private static final Map<Item, Float> SMELT_XP = Map.of(
		Items.RAW_COPPER, 0.7F,
		Items.RAW_IRON, 0.7F,
		Items.RAW_GOLD, 1.0F
	);

	private record Pending(ServerLevel level, BlockPos pos, int chancePercent) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private SmeltHandler() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		ItemStack pickaxe = serverPlayer.getMainHandItem();
		if (!pickaxe.is(ItemTags.PICKAXES)) {
			return;
		}
		int smeltLevel = ItemAuthority.effectiveLevel(serverPlayer, pickaxe, ModEnchantments.SMELT);
		if (smeltLevel <= 0) {
			return;
		}
		int chance = switch (smeltLevel) {
			case 1 -> 25;
			case 2 -> 50;
			default -> smeltLevel >= 3 ? 100 : 0;
		};
		PENDING.add(new Pending(serverLevel, pos, chance));
	}

	/** Called at the end of every server tick: converts the queued drops. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		// Entity ids already rolled this tick — see the class note.
		Set<Integer> claimed = new HashSet<>();
		for (Pending pending : PENDING) {
			RandomSource random = pending.level().getRandom();
			int chance = pending.chancePercent();
			if (chance <= 0) {
				continue;
			}
			for (ItemEntity drop : pending.level().getEntitiesOfClass(
				ItemEntity.class, new AABB(pending.pos()).inflate(1.5), entity -> entity.tickCount <= 1)) {

				ItemStack stack = drop.getItem();
				Item raw = stack.getItem();
				Item result = ORE_SMELTS.get(raw);
				if (result == null || !claimed.add(drop.getId())) {
					continue;
				}

				// Roll per item so partial stacks smelt partially.
				int count = stack.getCount();
				int smelted = 0;
				for (int i = 0; i < count; i++) {
					if (random.nextInt(100) < chance) {
						smelted++;
					}
				}
				if (smelted == 0) {
					continue;
				}
				if (smelted == count) {
					drop.setItem(new ItemStack(result, count));
				} else {
					drop.setItem(new ItemStack(raw, count - smelted));
					pending.level().addFreshEntity(new ItemEntity(
						pending.level(), drop.getX(), drop.getY(), drop.getZ(),
						new ItemStack(result, smelted)));
				}
				awardSmeltXp(pending.level(), drop.position(), raw, smelted, random);
			}
		}
		PENDING.clear();
	}

	/**
	 * The experience a furnace would have paid for the same items, dropped as
	 * orbs where the ingots are rather than straight into the bar — the ore
	 * blocks that do carry XP drop it the same way, and Miner's Magnet only
	 * moves items.
	 */
	private static void awardSmeltXp(ServerLevel level, Vec3 where, Item raw, int smelted, RandomSource random) {
		Float perItem = SMELT_XP.get(raw);
		if (perItem == null || smelted <= 0) {
			return;
		}
		float total = perItem * smelted;
		int points = Mth.floor(total);
		float fraction = Mth.frac(total);
		if (fraction != 0.0F && random.nextFloat() < fraction) {
			points++;
		}
		if (points > 0) {
			ExperienceOrb.award(level, where, points);
		}
	}
}
