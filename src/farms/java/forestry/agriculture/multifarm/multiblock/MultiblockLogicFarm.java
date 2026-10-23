package forestry.agriculture.multifarm.multiblock;

import forestry.api.core.multiblock.IMultiblockLogicFarm;
import forestry.core.platform.multiblock.MultiblockController;
import forestry.core.platform.multiblock.MultiblockLogicBase;

public class MultiblockLogicFarm extends MultiblockLogicBase implements IMultiblockLogicFarm {
	public MultiblockLogicFarm() {
	}

	@Override
	public IFarmControllerInternal getController() {
		MultiblockController controller = resolveController();
		if (controller instanceof IFarmControllerInternal internal && controller.isAssembled()) {
			return internal;
		}
		return FakeFarmController.INSTANCE;
	}
}
