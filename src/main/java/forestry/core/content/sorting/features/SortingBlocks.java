package forestry.core.content.sorting.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.registration.FeatureBlock;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.sorting.blocks.BlockGeneticFilter;

@FeatureProvider
public class SortingBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.SORTING);

	public static final FeatureBlock<BlockGeneticFilter, ItemBlockForestry<?>> FILTER = REGISTRY.block(BlockGeneticFilter::new, ItemBlockForestry::new, "genetic_filter");
}
