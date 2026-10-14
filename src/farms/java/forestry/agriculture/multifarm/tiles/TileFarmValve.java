package forestry.agriculture.multifarm.tiles;

import forestry.core.platform.fluids.ITankManager;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.agriculture.features.MultifarmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class TileFarmValve extends TileFarm implements ILiquidTankTile {
	public TileFarmValve(BlockPos pos, BlockState state) {
		super(MultifarmBlockEntities.VALVE.tileType(), pos, state);
	}

	@Override
	public ITankManager getTankManager() {
		return getMultiblockLogic().getController().getTankManager();
	}

	public ResourceHandler<FluidResource> getFluidHandler(net.minecraft.core.Direction facing) {
		return getTankManager();
	}
}
