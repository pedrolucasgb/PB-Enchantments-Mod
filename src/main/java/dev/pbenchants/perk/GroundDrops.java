package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The shovel passives that act on what a dug block drops, in one end-of-tick pass
 * for the same reason {@link AxeHarvest} and {@link HoeHarvest} have theirs:
 *
 *   Sifter           — gravel always gives its flint, and clay gives a fifth ball
 *   Concrete Setter  — concrete powder comes up as the hardened block
 *   Soul Digger      — 10% for soul sand or soul soil to drop twice
 *   Digger's Magnet  — whatever is left goes straight into the inventory
 *
 * <p>Sifter and Concrete Setter both <em>replace</em> a drop rather than adding
 * one, which is why they live here and not in a loot table: the swap has to
 * happen after vanilla has rolled, and it has to compose with a Flat Earth swing
 * that broke nine blocks at once. Each block fires its own break event, so each
 * one is queued and settled on its own.
 */
public final class GroundDrops {
	private static final double DROP_RADIUS = 1.5;
	private static final int SOUL_DIGGER_PERCENT = 10;

	/** Vanilla drops four clay balls; Sifter makes it five. */
	private static final int SIFTER_CLAY_BONUS = 1;

	/**
	 * Concrete powder to the hardened block of the same colour. Built by zipping
	 * the two colour collections rather than listing sixteen pairs, so it cannot
	 * drift out of order.
	 */
	private static final Map<Block, Item> POWDER_TO_CONCRETE = buildPowderMap();

	private record Pending(ServerLevel level, UUID playerId, BlockPos pos, Block dug,
	                       boolean sifter, boolean concreteSetter, boolean soulDigger, boolean magnet) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private GroundDrops() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!serverPlayer.getMainHandItem().is(ItemTags.SHOVELS)) {
			return;
		}
		Block dug = state.getBlock();
		boolean sifter = (dug == Blocks.GRAVEL || dug == Blocks.CLAY)
			&& PerkAccess.owns(serverPlayer, SkillTrees.GROUND, "sifter");
		boolean concreteSetter = POWDER_TO_CONCRETE.containsKey(dug)
			&& PerkAccess.owns(serverPlayer, SkillTrees.GROUND, "concrete_setter");
		boolean soulDigger = (dug == Blocks.SOUL_SAND || dug == Blocks.SOUL_SOIL)
			&& PerkAccess.owns(serverPlayer, SkillTrees.GROUND, "soul_digger");
		boolean magnet = PerkAccess.owns(serverPlayer, SkillTrees.GROUND, "diggers_magnet");

		if (!sifter && !concreteSetter && !soulDigger && !magnet) {
			return;
		}
		PENDING.add(new Pending(serverLevel, serverPlayer.getUUID(), pos, dug,
			sifter, concreteSetter, soulDigger, magnet));
	}

	/** Called at the end of every server tick, once every drop of the tick has spawned. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		Set<Integer> claimed = new HashSet<>();

		for (Pending pending : PENDING) {
			RandomSource random = pending.level().getRandom();
			List<ItemEntity> drops = new ArrayList<>();
			for (ItemEntity candidate : pending.level().getEntitiesOfClass(
				ItemEntity.class, new AABB(pending.pos()).inflate(DROP_RADIUS), entity -> entity.tickCount <= 1)) {
				if (claimed.add(candidate.getId())) {
					drops.add(candidate);
				}
			}

			List<ItemEntity> bonus = new ArrayList<>();
			for (ItemEntity drop : drops) {
				ItemStack stack = drop.getItem();
				if (pending.sifter()) {
					// Gravel that rolled gravel becomes the flint it was hiding;
					// gravel that already rolled flint is left alone, so the perk
					// is "always flint", not "flint twice".
					if (stack.is(Items.GRAVEL) && pending.dug() == Blocks.GRAVEL) {
						drop.setItem(new ItemStack(Items.FLINT, stack.getCount()));
						continue;
					}
					if (stack.is(Items.CLAY_BALL) && pending.dug() == Blocks.CLAY) {
						drop.setItem(stack.copyWithCount(stack.getCount() + SIFTER_CLAY_BONUS));
						continue;
					}
				}
				if (pending.concreteSetter()) {
					Item hardened = POWDER_TO_CONCRETE.get(pending.dug());
					if (hardened != null && stack.is(pending.dug().asItem())) {
						drop.setItem(new ItemStack(hardened, stack.getCount()));
						continue;
					}
				}
				if (pending.soulDigger() && stack.is(pending.dug().asItem())
					&& random.nextInt(100) < SOUL_DIGGER_PERCENT) {
					bonus.add(spawn(pending.level(), drop.blockPosition(), stack.copyWithCount(stack.getCount())));
				}
			}
			for (ItemEntity extraDrop : bonus) {
				claimed.add(extraDrop.getId());
			}
			drops.addAll(bonus);

			if (pending.magnet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
				if (player != null) {
					for (ItemEntity drop : drops) {
						if (!drop.isRemoved()) {
							drop.setNoPickUpDelay();
							drop.playerTouch(player);
						}
					}
				}
			}
		}
		PENDING.clear();
	}

	/** True for anything the shovel half counts as its own work. */
	public static boolean shovelMineable(BlockState state) {
		return state.is(BlockTags.MINEABLE_WITH_SHOVEL);
	}

	private static Map<Block, Item> buildPowderMap() {
		Map<Block, Item> map = new HashMap<>();
		ColorCollection.zipApply(Blocks.CONCRETE_POWDER, Items.CONCRETE, map::put);
		return Map.copyOf(map);
	}

	private static ItemEntity spawn(ServerLevel level, BlockPos pos, ItemStack stack) {
		ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
		entity.setDefaultPickUpDelay();
		level.addFreshEntity(entity);
		return entity;
	}
}
