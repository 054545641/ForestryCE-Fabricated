package forestry.apiculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.alveary.AlvearyBlock;
import forestry.apiculture.apiary.ApicultureBlockType;
import forestry.apiculture.alveary.multiblock.*;
import forestry.apiculture.apiary.ApiaryBlockEntity;
import forestry.apiculture.beehouse.BeeHouseBlockEntity;
import forestry.apiculture.hives.HiveBlockEntity;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@SuppressWarnings("Convert2MethodRef")
@FeatureProvider
public class ApicultureTiles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureTileType<HiveBlockEntity> HIVE = REGISTRY.tile(HiveBlockEntity::new, "hive", () -> ApicultureBlocks.HIVE.getList());
	public static final FeatureTileType<ApiaryBlockEntity> APIARY = REGISTRY.tile(ApiaryBlockEntity::new, "apiary", () -> ApicultureBlocks.BASE.get(ApicultureBlockType.APIARY).collect());
	public static final FeatureTileType<BeeHouseBlockEntity> BEE_HOUSE = REGISTRY.tile(BeeHouseBlockEntity::new, "bee_house", () -> ApicultureBlocks.BASE.get(ApicultureBlockType.BEE_HOUSE).collect());
	public static final FeatureTileType<AlvearyBlockEntity> ALVEARY_PLAIN = REGISTRY.tile(AlvearyBlockEntity::new, "alveary", () -> ApicultureBlocks.ALVEARY.get(AlvearyBlock.Type.PLAIN).collect());
	public static final FeatureTileType<AlvearySieveBlockEntity> ALVEARY_SIEVE = REGISTRY.tile(AlvearySieveBlockEntity::new, "alveary_sieve", () -> ApicultureBlocks.ALVEARY.get(AlvearyBlock.Type.SIEVE).collect());
	public static final FeatureTileType<AlvearySwarmerBlockEntity> ALVEARY_SWARMER = REGISTRY.tile(AlvearySwarmerBlockEntity::new, "alveary_swarmer", () -> ApicultureBlocks.ALVEARY.get(AlvearyBlock.Type.SWARMER).collect());
	public static final FeatureTileType<AlvearyHygroregulatorBlockEntity> ALVEARY_HYGROREGULATOR = REGISTRY.tile(AlvearyHygroregulatorBlockEntity::new, "alveary_hygroregulator", () -> ApicultureBlocks.ALVEARY.get(AlvearyBlock.Type.HYGROREGULATOR).collect());
	public static final FeatureTileType<AlvearyStabilizerBlockEntity> ALVEARY_STABILISER = REGISTRY.tile(AlvearyStabilizerBlockEntity::new, "alveary_stabilizer", () -> ApicultureBlocks.ALVEARY.get(AlvearyBlock.Type.STABILIZER).collect());
	public static final FeatureTileType<AlvearyFanBlockEntity> ALVEARY_FAN = REGISTRY.tile(AlvearyFanBlockEntity::new, "alveary_fan", () -> ApicultureBlocks.ALVEARY.get(AlvearyBlock.Type.FAN).collect());
	public static final FeatureTileType<AlvearyHeaterBlockEntity> ALVEARY_HEATER = REGISTRY.tile(AlvearyHeaterBlockEntity::new, "alveary_heater", () -> ApicultureBlocks.ALVEARY.get(AlvearyBlock.Type.HEATER).collect());

}
