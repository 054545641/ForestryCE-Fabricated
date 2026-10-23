package forestry.core.content.sorting.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.sorting.tiles.TileGeneticFilter;

@FeatureProvider
public class SortingTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.SORTING);

	public static final FeatureTileType<TileGeneticFilter> GENETIC_FILTER = REGISTRY.tile(TileGeneticFilter::new, "genetic_filter", SortingBlocks.FILTER::collect);
}
