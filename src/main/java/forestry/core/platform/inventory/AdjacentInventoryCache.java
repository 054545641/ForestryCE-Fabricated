package forestry.core.platform.inventory;

import forestry.core.platform.tile.AdjacentTileCache;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public final class AdjacentInventoryCache implements AdjacentTileCache.ICacheListener {

	public interface ITileFilter {
		boolean matches(BlockEntity tile);
	}

	private final AdjacentTileCache cache;
	private boolean changed = true;
	private final List<ResourceHandler<ItemResource>> invs = new LinkedList<>();
	private final ResourceHandler<ItemResource>[] sides = new ResourceHandler[6];
	@Nullable
	private final Comparator<ResourceHandler<ItemResource>> sorter;
	@Nullable
	private final ITileFilter filter;

	public AdjacentInventoryCache(BlockEntity tile, AdjacentTileCache cache) {
		this(tile, cache, null, null);
	}

	public AdjacentInventoryCache(BlockEntity tile, AdjacentTileCache cache, @Nullable ITileFilter filter) {
		this(tile, cache, filter, null);
	}

	public AdjacentInventoryCache(BlockEntity tile, AdjacentTileCache cache, @Nullable ITileFilter filter, @Nullable Comparator<ResourceHandler<ItemResource>> sorter) {
		this.cache = cache;
		this.filter = filter;
		this.sorter = sorter;
		cache.addListener(this);
	}

	@Nullable
	public ResourceHandler<ItemResource> getAdjacentInventory(Direction side) {
		checkChanged();
		return this.sides[side.ordinal()];
	}

	public Collection<ResourceHandler<ItemResource>> getAdjacentInventories() {
		checkChanged();
		return this.invs;
	}

	public Collection<ResourceHandler<ItemResource>> getAdjacentInventoriesOtherThan(Direction side) {
		checkChanged();
		Collection<ResourceHandler<ItemResource>> ret = new ArrayList<>(getAdjacentInventories());
		ret.remove(getAdjacentInventory(side));
		return ret;
	}

	@Override
	public void changed() {
        this.changed = true;
	}

	@Override
	public void purge() {
        this.invs.clear();
		Arrays.fill(this.sides, null);
	}

	private void checkChanged() {
        this.cache.refresh();
		if (this.changed) {
            this.changed = false;
			purge();
			for (Direction side : Direction.values()) {
				BlockEntity tile = this.cache.getTileOnSide(side);
				if (tile != null && (this.filter == null || this.filter.matches(tile))) {
					ResourceHandler<ItemResource> inv = TileUtil.getInventoryFromTile(tile, side.getOpposite());
					if (inv != null) {
                        this.sides[side.ordinal()] = inv;
                        this.invs.add(inv);
					}
				}
			}
			if (this.sorter != null) {
                this.invs.sort(this.sorter);
			}
		}
	}

}
