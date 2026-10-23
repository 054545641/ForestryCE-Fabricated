package forestry.apiculture.apiary;

import forestry.apiculture.features.ApicultureTiles;
import forestry.apiculture.bees.AbstractBeeHousingBlockEntity;
import forestry.core.platform.block.IBlockType;
import forestry.core.platform.block.IMachineProperties;
import forestry.core.platform.block.MachineProperties;
import forestry.core.platform.registration.FeatureTileType;

public enum ApicultureBlockType implements IBlockType {
	BEE_HOUSE(ApicultureTiles.BEE_HOUSE, "bee_house"),
	APIARY(ApicultureTiles.APIARY, "apiary");

	private final IMachineProperties<?> machineProperties;

	<T extends AbstractBeeHousingBlockEntity> ApicultureBlockType(FeatureTileType<? extends T> teClass, String name) {
		this.machineProperties = new MachineProperties.Builder<>(teClass, name)
			.setClientTicker(AbstractBeeHousingBlockEntity::clientTick)
			.setServerTicker(AbstractBeeHousingBlockEntity::serverTick)
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
