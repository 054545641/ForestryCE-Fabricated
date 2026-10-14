package forestry.core.platform.models;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import forestry.core.platform.models.baker.ModelBakerModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class ModelBlockCached<B extends Block, K> extends ModelBlockDefault<B, K> {
	private static final Set<ModelBlockCached<?, ?>> CACHE_PROVIDERS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	private record CacheKey(Object owner, Object value) {}

	// todo test performance of this. from my experience, Cache is slow.
	private final Cache<Object, ModelBakerModel> inventoryCache;
	private final Cache<Object, ModelBakerModel> worldCache;

	public static synchronized void clear() {
		for (ModelBlockCached<?, ?> modelBlockCached : CACHE_PROVIDERS) {
			modelBlockCached.worldCache.invalidateAll();
			modelBlockCached.inventoryCache.invalidateAll();
		}
	}

	protected ModelBlockCached(Class<B> blockClass) {
		super(blockClass);

        this.worldCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();
        this.inventoryCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

		synchronized (ModelBlockCached.class) { CACHE_PROVIDERS.add(this); }
	}

	@Override
	protected ModelBakerModel getModel(BlockState state, ModelData extraData) {
		return getOrBake(this.worldCache, new CacheKey(state, getWorldKey(state, extraData)), () -> super.getModel(state, extraData));
	}

	@Override
	protected ModelBakerModel getModel(ItemStack stack, Level world) {
		K key = getInventoryKey(stack);

		return getOrBake(this.inventoryCache, new CacheKey(stack.getItem(), key), () -> bakeModel(stack, world, key));
	}

	/**
	 * Used to bake a model at most once per cache key.
	 *
	 * @param cache The cache to read the model from, and to store a newly baked model in
	 * @param key   The cache key of the model
	 * @param baker The fallback that bakes the model when the cache does not have it
	 * @return The cached model, baked by this call if it was absent
	 */
	private static <K> ModelBakerModel getOrBake(Cache<K, ModelBakerModel> cache, K key, Callable<ModelBakerModel> baker) {
		try {
			// get(key, baker) rather than getIfPresent + put: chunk meshing runs on several worker threads, which
			// all miss the same key at once after a cache clear and bake redundant copies of the same model
			return cache.get(key, baker);
		} catch (ExecutionException e) {
			// the bakers declare no checked exceptions, so this is unreachable
			throw new RuntimeException("Failed to bake a Forestry block model", e.getCause());
		}
	}
}
