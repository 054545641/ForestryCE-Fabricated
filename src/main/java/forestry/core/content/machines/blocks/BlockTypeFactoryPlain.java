package forestry.core.content.machines.blocks;

import forestry.core.platform.block.IBlockType;
import forestry.core.platform.block.IMachineProperties;
import forestry.core.platform.block.MachineProperties;
import forestry.core.platform.tile.IForestryTicker;
import forestry.core.platform.tile.TileForestry;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.tiles.*;
import forestry.core.platform.registration.FeatureTileType;

public enum BlockTypeFactoryPlain implements IBlockType {
	FABRICATOR(FactoryTiles.FABRICATOR, "thermionic_fabricator", IMachineProperties.TankLayout.NONE, TileFabricator::serverTick),
	SMELTER(FactoryTiles.SMELTER, "smelter", IMachineProperties.TankLayout.NONE, TileSmelter::serverTick),
	BOTTLER(FactoryTiles.BOTTLER, "bottler", IMachineProperties.TankLayout.RESOURCE, TileBottler::serverTick),
	CARPENTER(FactoryTiles.CARPENTER, "carpenter", IMachineProperties.TankLayout.RESOURCE, TileCarpenter::serverTick),
	CENTRIFUGE(FactoryTiles.CENTRIFUGE, "centrifuge", IMachineProperties.TankLayout.NONE, TileCentrifuge::serverTick),
	FERMENTER(FactoryTiles.FERMENTER, "fermenter", IMachineProperties.TankLayout.BOTH, TileFermenter::serverTick),
	MOISTENER(FactoryTiles.MOISTENER, "moistener", IMachineProperties.TankLayout.RESOURCE, TileMoistener::serverTick),
	SQUEEZER(FactoryTiles.SQUEEZER, "squeezer", IMachineProperties.TankLayout.PRODUCT, TileSqueezer::serverTick),
	STILL(FactoryTiles.STILL, "still", IMachineProperties.TankLayout.BOTH, TileStill::serverTick);

	private final IMachineProperties<?> machineProperties;

	<T extends TileForestry> BlockTypeFactoryPlain(FeatureTileType<T> teClass, String name, IMachineProperties.TankLayout layout, IForestryTicker<T> serverTicker) {
		this.machineProperties = new MachineProperties.Builder<>(teClass, name)
			.setServerTicker(serverTicker)
			.setTankLayout(layout)
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
