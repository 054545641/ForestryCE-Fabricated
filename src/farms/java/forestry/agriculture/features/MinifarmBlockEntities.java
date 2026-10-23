package forestry.agriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.agriculture.minifarm.blocks.MinifarmBlockType;
import forestry.agriculture.minifarm.tiles.*;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.Supplier;

@FeatureProvider
public class MinifarmBlockEntities {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CULTIVATION);

	public static final FeatureTileType<ArboretumBlockEntity> ARBORETUM = createTile("arboretum", () -> MinifarmBlockType.ARBORETUM, ArboretumBlockEntity::new);
	public static final FeatureTileType<PeatBogBlockEntity> BOG = createTile("bog", () -> MinifarmBlockType.PEAT_POG, PeatBogBlockEntity::new);
	public static final FeatureTileType<CropFarmBlockEntity> CROPS = createTile("crops", () -> MinifarmBlockType.FARM_CROPS, CropFarmBlockEntity::new);
	public static final FeatureTileType<EnderFarmBlockEntity> ENDER = createTile("ender", () -> MinifarmBlockType.FARM_ENDER, EnderFarmBlockEntity::new);
	public static final FeatureTileType<GoardFarmBlockEntity> GOURD = createTile("gourd", () -> MinifarmBlockType.FARM_GOURD, GoardFarmBlockEntity::new);
	public static final FeatureTileType<MushroomFarmBlockEntity> MUSHROOM = createTile("mushroom", () -> MinifarmBlockType.FARM_MUSHROOM, MushroomFarmBlockEntity::new);
	public static final FeatureTileType<InfernalFarmBlockEntity> NETHER = createTile("nether", () -> MinifarmBlockType.FARM_NETHER, InfernalFarmBlockEntity::new);

	private static <T extends BlockEntity> FeatureTileType<T> createTile(String id, Supplier<MinifarmBlockType> type, BlockEntityType.BlockEntitySupplier<T> supplier) {
		return REGISTRY.tile(supplier, id, () -> {
			MinifarmBlockType t = type.get();
			return List.of(MinifarmBlocks.MANAGED_PLANTER.get(t).block(), MinifarmBlocks.MANUAL_PLANTER.get(t).block());
		});
	}
}
