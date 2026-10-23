package forestry.agriculture.farmlogic;

import forestry.api.agriculture.IFarmType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FarmLogicSoil extends FarmLogic {
	public FarmLogicSoil(IFarmType properties, boolean isManual) {
		super(properties, isManual);
	}

	protected boolean isAcceptedSoil(BlockState blockState) {
		return this.type.isAcceptedSoil(blockState);
	}
}
