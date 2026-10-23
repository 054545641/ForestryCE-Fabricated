package forestry.apiculture.alveary.multiblock;

import forestry.api.core.IInventoryAdapter;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.multiblock.IAlvearyController;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.owner.IOwnedTile;

/**
 * Machine-specific surface of the alveary controller.
 * <p>
 * After the engine rewrite this extends the public {@link IAlvearyController}, which extends the public
 * {@link forestry.api.core.multiblock.IMultiblockController}, rather than the deleted engine-internal
 * {@code IMultiblockControllerInternal}. Only the alveary-specific accessors remain (climate, bee listeners/modifiers,
 * beekeeping logic, shared inventory, GUI streaming).
 */
public interface IAlvearyControllerInternal extends IAlvearyController, IClimateProvider, IOwnedTile, IStreamableGui {
	IInventoryAdapter getInternalInventory();

	int getHealthScaled(int i);
}
