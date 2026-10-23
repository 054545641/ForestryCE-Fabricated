package forestry.core.content.machines.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.machines.blocks.BlockTypeFactoryPlain;
import forestry.core.content.machines.blocks.BlockTypeFactoryTesr;
import forestry.core.content.machines.tiles.*;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class FactoryTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FACTORY);

	public static final FeatureTileType<TileBottler> BOTTLER = REGISTRY.tile(TileBottler::new, "bottler", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.BOTTLER).collect());
	public static final FeatureTileType<TileCarpenter> CARPENTER = REGISTRY.tile(TileCarpenter::new, "carpenter", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.CARPENTER).collect());
	public static final FeatureTileType<TileCentrifuge> CENTRIFUGE = REGISTRY.tile(TileCentrifuge::new, "centrifuge", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.CENTRIFUGE).collect());
	public static final FeatureTileType<TileFabricator> FABRICATOR = REGISTRY.tile(TileFabricator::new, "thermionic_fabricator", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.FABRICATOR).collect());
	public static final FeatureTileType<TileFermenter> FERMENTER = REGISTRY.tile(TileFermenter::new, "fermenter", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.FERMENTER).collect());
	public static final FeatureTileType<TileMillRainmaker> RAINMAKER = REGISTRY.tile(TileMillRainmaker::new, "rainmaker", () -> FactoryBlocks.TESR.get(BlockTypeFactoryTesr.RAINMAKER).collect());
	public static final FeatureTileType<TileMoistener> MOISTENER = REGISTRY.tile(TileMoistener::new, "moistener", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.MOISTENER).collect());
	public static final FeatureTileType<TileSmelter> SMELTER = REGISTRY.tile(TileSmelter::new, "smelter", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.SMELTER).collect());
	public static final FeatureTileType<TileSqueezer> SQUEEZER = REGISTRY.tile(TileSqueezer::new, "squeezer", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.SQUEEZER).collect());
	public static final FeatureTileType<TileStill> STILL = REGISTRY.tile(TileStill::new, "still", () -> FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.STILL).collect());
}
