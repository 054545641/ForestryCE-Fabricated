package forestry.core.platform.gui;

import forestry.core.platform.fluids.StandardTank;

import javax.annotation.Nullable;

/**
 * A menu that exposes fluid tanks by slot index. Split out of {@link IContainerLiquidTanks} so a menu
 * can be drawn by {@code TankWidget} without also implementing pipette handling.
 */
public interface IContainerTank {
	/**
	 * @return The tank in the given slot, or null if the slot holds none
	 */
	@Nullable
	StandardTank getTank(int slot);
}
