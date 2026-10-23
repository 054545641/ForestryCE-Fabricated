package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.block.NaturalistChestBlockType;
import forestry.core.platform.block.BlockTypeCoreTesr;
import forestry.core.platform.tile.TileApiaristChest;
import forestry.core.platform.tile.TileArboristChest;
import forestry.core.platform.tile.TileLepidopteristChest;
import forestry.core.platform.tile.*;
import forestry.core.content.machines.*;
import forestry.core.content.escritoire.*;
import forestry.core.content.analyzer.*;
import forestry.core.content.burnbarrel.TileBurnBarrel;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class CoreTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final FeatureTileType<TileAnalyzer> ANALYZER = REGISTRY.tile(TileAnalyzer::new, "analyzer", () -> CoreUtilityBlocks.BASE.get(BlockTypeCoreTesr.ANALYZER).collect());
	public static final FeatureTileType<TileEscritoire> ESCRITOIRE = REGISTRY.tile(TileEscritoire::new, "escritoire", () -> CoreUtilityBlocks.BASE.get(BlockTypeCoreTesr.ESCRITOIRE).collect());
	public static final FeatureTileType<TileApiaristChest> APIARIST_CHEST = REGISTRY.tile(TileApiaristChest::new, "apiarists_chest", () -> CoreUtilityBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.APIARIST_CHEST).collect());
	public static final FeatureTileType<TileArboristChest> ARBORIST_CHEST = REGISTRY.tile(TileArboristChest::new, "arborists_chest", () -> CoreUtilityBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.ARBORIST_CHEST).collect());
	public static final FeatureTileType<TileLepidopteristChest> LEPIDOPTERIST_CHEST = REGISTRY.tile(TileLepidopteristChest::new, "lepidopterists_chest", () -> CoreUtilityBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.LEPIDOPTERIST_CHEST).collect());
	public static final FeatureTileType<TileBurnBarrel> BURN_BARREL = REGISTRY.tile(TileBurnBarrel::new, "burn_barrel", () -> CoreUtilityBlocks.BURN_BARREL.collect());

}
