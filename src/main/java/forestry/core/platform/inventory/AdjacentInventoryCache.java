package forestry.core.platform.inventory;

import forestry.core.platform.tile.AdjacentTileCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import javax.annotation.Nullable;
import java.util.*;

/** Native Lookup caches observe replacement, provider invalidation and chunk unload without polling delays. */
public final class AdjacentInventoryCache implements AdjacentTileCache.ICacheListener {
    public interface ITileFilter { boolean matches(BlockEntity tile); }
    private final BlockEntity source;
    private final EnumMap<Direction, BlockApiCache<Storage<ItemVariant>, Direction>> caches = new EnumMap<>(Direction.class);
    @Nullable private final ITileFilter filter;
    @Nullable private final Comparator<Storage<ItemVariant>> sorter;
    public AdjacentInventoryCache(BlockEntity source, AdjacentTileCache cache) { this(source, cache, null, null); }
    public AdjacentInventoryCache(BlockEntity source, AdjacentTileCache cache, @Nullable ITileFilter filter) { this(source, cache, filter, null); }
    public AdjacentInventoryCache(BlockEntity source, AdjacentTileCache cache, @Nullable ITileFilter filter, @Nullable Comparator<Storage<ItemVariant>> sorter) {
        this.source = source; this.filter = filter; this.sorter = sorter;
        cache.addListener(this);
    }
    @Nullable public Storage<ItemVariant> getAdjacentInventory(Direction side) {
        if (source.isRemoved() || !(source.getLevel() instanceof ServerLevel level)) return null;
        var pos = source.getBlockPos().relative(side);
        if (!level.hasChunkAt(pos)) return null;
        var cache = caches.computeIfAbsent(side, ignored -> BlockApiCache.create(ItemStorage.SIDED, level, pos));
        // find refreshes the native block-entity cache before the optional domain filter is evaluated.
        var result = cache.find(side.getOpposite());
        if (filter != null && (cache.getBlockEntity() == null || !filter.matches(cache.getBlockEntity()))) return null;
        return result;
    }
    public Collection<Storage<ItemVariant>> getAdjacentInventories() {
        List<Storage<ItemVariant>> result = new ArrayList<>();
        for (Direction side : Direction.values()) { var storage = getAdjacentInventory(side); if (storage != null) result.add(storage); }
        if (sorter != null) result.sort(sorter);
        return List.copyOf(result);
    }
    public Collection<Storage<ItemVariant>> getAdjacentInventoriesOtherThan(Direction side) {
        var result = new ArrayList<>(getAdjacentInventories()); result.remove(getAdjacentInventory(side)); return result;
    }
    @Override public void changed() { caches.clear(); }
    @Override public void purge() { caches.clear(); }
}
