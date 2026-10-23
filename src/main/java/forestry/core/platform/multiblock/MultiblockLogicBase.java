package forestry.core.platform.multiblock;

import javax.annotation.Nullable;

import forestry.api.core.multiblock.IMultiblockLogic;

/**
 * Thin replacement for the deleted {@code MultiblockLogic} save-delegate object (plan Task 2.2 and 2.3). It
 * no longer drives validation, persistence or networking. It only resolves the owning block's controller
 * through the {@link MultiblockIndex}, via the owning BE's {@code anchorPos} (spec 6.1). Machine-typed
 * subclasses {@code MultiblockLogicAlveary} and {@code MultiblockLogicFarm} supply the {@code Fake}
 * controller for the unassembled or anchor-missing case.
 */
public abstract class MultiblockLogicBase implements IMultiblockLogic {
	@Nullable
	private MultiblockTileEntityForestry<?> tile;

	/** Injected by {@code MultiblockTileEntityForestry}'s constructor, the owning member BE. */
	public void setTile(MultiblockTileEntityForestry<?> tile) {
		this.tile = tile;
	}

	/** Resolves the live controller hosted at the owning member's anchor, or {@code null} if unassembled. */
	@Nullable
	protected MultiblockController resolveController() {
		return this.tile == null ? null : this.tile.getController();
	}

	@Override
	public boolean isConnected() {
		MultiblockController controller = resolveController();
		return controller != null && controller.isAssembled();
	}
}
