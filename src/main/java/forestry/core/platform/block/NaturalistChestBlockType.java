package forestry.core.platform.block;

import forestry.core.features.CoreTiles;
import forestry.core.platform.tile.TileNaturalistChest;
import forestry.core.platform.registration.FeatureTileType;

public enum NaturalistChestBlockType implements IBlockType {
	APIARIST_CHEST("apiarists_chest", CoreTiles.APIARIST_CHEST),
	ARBORIST_CHEST("arborists_chest", CoreTiles.ARBORIST_CHEST),
	LEPIDOPTERIST_CHEST("lepidopterists_chest", CoreTiles.LEPIDOPTERIST_CHEST);

	private final MachineProperties<?> machineProperties;

	NaturalistChestBlockType(String name, FeatureTileType<? extends TileNaturalistChest> tileType) {
		this.machineProperties = new MachineProperties.Builder<>(tileType, name)
			.setClientTicker(TileNaturalistChest::clientTick)
			.setShape(TileNaturalistChest.CHEST_SHAPE)
			.create();
	}

	@Override
	public IMachineProperties<?> getMachineProperties() {
		return this.machineProperties;
	}

	@Override
	public String getSerializedName() {
		return getMachineProperties().getSerializedName();
	}
}
