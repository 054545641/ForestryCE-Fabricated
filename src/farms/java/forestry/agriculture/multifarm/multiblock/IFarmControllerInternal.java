package forestry.agriculture.multifarm.multiblock;

import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.multiblock.IFarmController;
import forestry.core.engine.circuits.ISocketable;
import forestry.core.platform.fluids.ITankManager;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.owner.IOwnedTile;
import forestry.agriculture.farmlogic.IFarmHousingInternal;
import forestry.agriculture.multifarm.gui.IFarmLedgerDelegate;

/**
 * Machine-specific surface of the farm controller.
 *
 * After the engine rewrite (plan Task 2.3) this extends the <b>public</b> {@link IFarmController},
 * which extends the public {@code forestry.api.core.multiblock.IMultiblockController}, rather than the
 * deleted engine-internal {@code IMultiblockControllerInternal}. Only the farm-specific accessors
 * remain (sockets, tank, farm-logic/inventory, GUI streaming).
 */
public interface IFarmControllerInternal extends IFarmController, ISocketable, IClimateProvider, IOwnedTile, IStreamableGui, IFarmHousingInternal {
	IFarmLedgerDelegate getFarmLedgerDelegate();

	IInventoryAdapter getInternalInventory();

	@Override
	ITankManager getTankManager();
}
