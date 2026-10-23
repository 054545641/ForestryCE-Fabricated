package forestry.apiculture.bees;

import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.IErrorLogicSource;
import forestry.core.platform.owner.IOwnedTile;
import forestry.core.platform.tile.ITitled;

public interface IGuiBeeHousingDelegate extends ITitled, IErrorLogicSource, IOwnedTile, IClimateProvider {
	/**
	 * Returns scaled queen health or breeding progress
	 */
	int getHealthScaled(int i);

	String getHintKey();
}
