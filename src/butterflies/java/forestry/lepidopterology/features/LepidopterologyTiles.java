package forestry.lepidopterology.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.lepidopterology.cocoons.TileCocoon;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class LepidopterologyTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.LEPIDOPTEROLOGY);

	public static final FeatureTileType<TileCocoon> SOLID_COCOON = REGISTRY.tile((pos, state) -> new TileCocoon(pos, state, true), "solid_cocoon", LepidopterologyBlocks.COCOON_SOLID::collect);
	public static final FeatureTileType<TileCocoon> COCOON = REGISTRY.tile((pos, state) -> new TileCocoon(pos, state, false), "cocoon", LepidopterologyBlocks.COCOON::collect);
}
