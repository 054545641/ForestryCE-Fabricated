package forestry.agriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.agriculture.multifarm.blocks.MultifarmBlockType;
import forestry.agriculture.multifarm.tiles.*;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class MultifarmBlockEntities {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FARMING);

	public static final FeatureTileType<TileFarmControl> CONTROL = REGISTRY.tile(TileFarmControl::new, "control", () -> MultifarmBlocks.FARM.getRowBlocks(MultifarmBlockType.CONTROL));
	public static final FeatureTileType<TileFarmGearbox> GEARBOX = REGISTRY.tile(TileFarmGearbox::new, "gearbox", () -> MultifarmBlocks.FARM.getRowBlocks(MultifarmBlockType.GEARBOX));
	public static final FeatureTileType<TileFarmHatch> HATCH = REGISTRY.tile(TileFarmHatch::new, "hatch", () -> MultifarmBlocks.FARM.getRowBlocks(MultifarmBlockType.HATCH));
	public static final FeatureTileType<TileFarmPlain> PLAIN = REGISTRY.tile(TileFarmPlain::new, "plain", () -> MultifarmBlocks.FARM.getRowBlocks(MultifarmBlockType.PLAIN));
	public static final FeatureTileType<TileFarmValve> VALVE = REGISTRY.tile(TileFarmValve::new, "valve", () -> MultifarmBlocks.FARM.getRowBlocks(MultifarmBlockType.VALVE));
}
