package forestry.api.core.multiblock;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * IMultiblockController is used to handle the assembly of Multiblocks.
 * This class is created and updated by IMultiblockLogic and Forestry.
 * This is a minimal interface to hide the ugly details from the multiblock tile entities.
 */
public interface IMultiblockController {
	/**
	 * @return True if this multiblock machine is considered assembled and ready to go
	 */
	boolean isAssembled();

	/**
	 * Used to force the controller to check the multiblock's validity.
	 * Call this when important conditions around the multiblock change
	 * (ex. Alveary slabs are removed and the alveary block detects its neighbor changed)
	 * Changes to multiblock components are handled automatically and should not call this.
	 */
	void reassemble();

	/**
	 * @return A string representing the last error encountered when trying to assemble this
	 * multiblock, or null if there is no error
	 */
	@Nullable
	String getLastValidationError();

	/**
	 * @return all the multiblock components attached to this controller
	 */
	Collection<IMultiblockComponent> getComponents();

	/**
	 * Called when all blocks in this multiblock are destroyed and no new reference position can be found.
	 * This method is typically used to drop stored items.
	 *
	 * @param lastPos The last block position in this multiblock before being destroyed. Any stored items should
	 *                be dropped at this position.
	 */
	default void onDestroyed(BlockPos lastPos) {
	}
}
