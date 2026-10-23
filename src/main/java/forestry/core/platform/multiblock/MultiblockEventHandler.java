package forestry.core.platform.multiblock;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.world.level.LevelAccessor;

/** Native lifecycle callbacks preserve payloads across temporary chunk unloads. */
public final class MultiblockEventHandler {
    public static void initialize() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((tile, level) -> {
            if (tile instanceof MultiblockTileEntityForestry<?> member) member.onLoad();
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            for (var tile : java.util.List.copyOf(chunk.getBlockEntities().values()))
                if (tile instanceof MultiblockTileEntityForestry<?> member) member.onChunkUnloaded();
        });
        ServerLevelEvents.UNLOAD.register((server, level) -> clear(level));
        ServerTickEvents.END_LEVEL_TICK.register(DeferredValidation::retryLoaded);
    }
    public static void clear(LevelAccessor level) {
        MultiblockIndex.clear(level);
        DeferredValidation.clear(level);
    }
}
