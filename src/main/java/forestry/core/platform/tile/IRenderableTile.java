package forestry.core.platform.tile;

import forestry.core.platform.render.TankRenderInfo;

public interface IRenderableTile {
	TankRenderInfo getResourceTankInfo();

	TankRenderInfo getProductTankInfo();
}
