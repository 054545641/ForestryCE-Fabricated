package forestry.core.platform.block;

import forestry.core.platform.network.packets.PacketTileStream;
import forestry.core.platform.tile.TileForestry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Based on https://github.com/thedarkcolour/ExDeorum/blob/1.21.1/src/main/java/thedarkcolour/exdeorum/network/VisualUpdateTracker.java
public class TileStreamUpdateTracker {
	public static void clear() { UPDATES.clear(); }
	private static final Map<ResourceKey<Level>, Map<ChunkPos, Set<BlockPos>>> UPDATES = new HashMap<>();

	public static void sendVisualUpdate(TileForestry tile) {
		var level = tile.getLevel();

		if (level != null && !level.isClientSide()) {
			Map<ChunkPos, Set<BlockPos>> chunkUpdates = UPDATES.computeIfAbsent(level.dimension(), key -> new HashMap<>());
			chunkUpdates.computeIfAbsent(ChunkPos.containing(tile.getBlockPos()), key -> new HashSet<>()).add(tile.getBlockPos());
		}
	}

	public static void syncVisualUpdates(MinecraftServer server) {
		for (var levelUpdates : UPDATES.entrySet()) {
			var level = server.getLevel(levelUpdates.getKey());

			if (level != null) {
				var pendingUpdates = levelUpdates.getValue();

				for (var entry : pendingUpdates.entrySet()) {
					var chunkPos = entry.getKey();

					for (var updatePos : entry.getValue()) {
						if (level.hasChunkAt(updatePos) && level.getBlockEntity(updatePos) instanceof TileForestry blockEntity) {
							forestry.core.platform.util.NetworkUtil.sendToPlayersTrackingPos(new PacketTileStream(blockEntity), updatePos, level);
						}
					}
				}

				pendingUpdates.clear();
			}
		}
	}
}
