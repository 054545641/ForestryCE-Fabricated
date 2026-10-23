package forestry.api.core.multiblock;

import forestry.api.agriculture.IFarmListener;

/**
 * Needs to be implemented by TileEntities that want to be part of a farm.
 * The sub-interfaces can be implemented to alter the operation of the farm.
 * They are automatically detected and handled by the farm when they join its structure.
 */
public interface IFarmComponent<T extends IMultiblockLogicFarm> extends IMultiblockComponent {

	/**
	 * @return the multiblock logic for this component
	 */
	@Override
	T getMultiblockLogic();

	/**
	 * Implemented by farm parts to apply a farmListener to the completed structure.
	 */
	interface Listener extends IFarmComponent {
		IFarmListener getFarmListener();
	}

	/**
	 * Implemented by farm parts to receive ticks from the completed structure.
	 */
	interface Active extends IFarmComponent {
		void updateServer(int tickCount);

		void updateClient(int tickCount);
	}
}
