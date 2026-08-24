package dev.toolmastery.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which containers around a player the Artisan perks are allowed to touch, and
 * how to find them cheaply.
 *
 * <p>The rules are deliberately narrow, because everything downstream —
 * searching, Quick Stack, Restock, the Ledger — inherits them:
 *
 * <ul>
 *   <li><b>Storage only.</b> Chests (a double chest counts as one), trapped
 *       chests, barrels and placed shulker boxes. Furnaces, hoppers, droppers,
 *       dispensers and brewing stands are excluded: accidentally filling a
 *       hopper is a griefing machine, not a convenience. Ender chests are out
 *       too — they are a shared void, and a capstone that quietly empties your
 *       pockets into one would be a trap.</li>
 *   <li><b>Openable by this player.</b> The scan goes through the same
 *       "could you right-click this" checks a real interaction uses — a locked
 *       container without the key, or a chest blocked by a solid block or a
 *       cat, is simply not there. Claim mods that hook that path therefore work
 *       by construction.</li>
 *   <li><b>Loaded and bounded.</b> Only chunks already in memory, at most
 *       {@link #MAX_CONTAINERS} results, nearest first — somebody will stand in
 *       a 400-chest storage room and hold the button down.</li>
 * </ul>
 *
 * <p>The sweep walks the block entities of the chunks in range rather than
 * every block position: a 16-block radius is 35 937 positions but only nine
 * chunks.
 */
public final class ContainerScan {
	/** Hard cap on how many containers one action may touch. */
	public static final int MAX_CONTAINERS = 32;

	/** One container in reach: where it is, what is in it, what to call it. */
	public record Found(BlockPos pos, Container container, Component name, double distanceSq) {
	}

	private ContainerScan() {
	}

	/** True for a block whose block entity this class would accept. */
	public static boolean isStorageBlock(BlockState state) {
		return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof ShulkerBoxBlock
			|| state.is(net.minecraft.world.level.block.Blocks.BARREL);
	}

	/**
	 * Every container the player may use within {@code radius} blocks, nearest
	 * first and capped at {@link #MAX_CONTAINERS}.
	 */
	public static List<Found> nearby(ServerPlayer player, int radius) {
		ServerLevel level = player.level();
		BlockPos origin = player.blockPosition();
		double radiusSq = (double) radius * radius;

		List<Found> found = new ArrayList<>();
		Set<BlockPos> claimed = new HashSet<>();

		int minChunkX = SectionPos.blockToSectionCoord(origin.getX() - radius);
		int maxChunkX = SectionPos.blockToSectionCoord(origin.getX() + radius);
		int minChunkZ = SectionPos.blockToSectionCoord(origin.getZ() - radius);
		int maxChunkZ = SectionPos.blockToSectionCoord(origin.getZ() + radius);

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					continue; // not loaded: out of reach by definition
				}
				for (BlockEntity blockEntity : List.copyOf(chunk.getBlockEntities().values())) {
					BlockPos pos = blockEntity.getBlockPos();
					double distanceSq = pos.distToCenterSqr(player.position());
					if (distanceSq > radiusSq || claimed.contains(pos)) {
						continue;
					}
					Found entry = accept(player, level, blockEntity, pos, distanceSq);
					if (entry == null) {
						continue;
					}
					// A double chest answers to both halves; claim the partner
					// so it is not scanned, listed or filled twice.
					claimed.add(pos);
					for (BlockPos neighbour : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
						if (level.getBlockEntity(neighbour) instanceof ChestBlockEntity) {
							claimed.add(neighbour);
						}
					}
					found.add(entry);
				}
			}
		}

		found.sort(Comparator.comparingDouble(Found::distanceSq));
		return found.size() > MAX_CONTAINERS ? found.subList(0, MAX_CONTAINERS) : found;
	}

	/** One block entity, vetted against the rules above. Null when it does not qualify. */
	private static Found accept(ServerPlayer player, ServerLevel level, BlockEntity blockEntity,
			BlockPos pos, double distanceSq) {
		if (!(blockEntity instanceof BaseContainerBlockEntity base) || !base.canOpen(player)) {
			return null;
		}
		BlockState state = blockEntity.getBlockState();
		if (blockEntity instanceof ChestBlockEntity && state.getBlock() instanceof ChestBlock chestBlock) {
			// false = do not override the blocked check, so a chest under a
			// solid block or a sitting cat stays shut, exactly as it would to a
			// right-click. This also merges the two halves of a double chest.
			Container container = ChestBlock.getContainer(chestBlock, state, level, pos, false);
			return container == null ? null : new Found(pos, container, base.getDisplayName(), distanceSq);
		}
		if (blockEntity instanceof BarrelBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {
			return new Found(pos, base, base.getDisplayName(), distanceSq);
		}
		return null;
	}
}
