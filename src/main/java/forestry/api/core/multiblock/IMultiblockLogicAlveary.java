package forestry.api.core.multiblock;

public interface IMultiblockLogicAlveary extends IMultiblockLogic {
	/**
	 * @return the multiblock controller for this logic
	 */
	@Override
	IAlvearyController getController();
}
