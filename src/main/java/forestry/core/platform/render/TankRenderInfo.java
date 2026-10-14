package forestry.core.platform.render;

import net.neoforged.neoforge.fluids.FluidStack;
import forestry.core.platform.fluids.StandardTank;

public class TankRenderInfo {
	public static final TankRenderInfo EMPTY = new TankRenderInfo(FluidStack.EMPTY, EnumTankLevel.EMPTY);

	private final FluidStack fluidStack;
	private final EnumTankLevel level;

	public TankRenderInfo(StandardTank fluidTank) {
		this(fluidTank.getFluid(), EnumTankLevel.rateTankLevel(fluidTank));
	}

	public TankRenderInfo(FluidStack fluidStack, EnumTankLevel level) {
		this.fluidStack = fluidStack;
		this.level = level;
	}

	public FluidStack getFluidStack() {
		return this.fluidStack;
	}

	public EnumTankLevel getLevel() {
		return this.level;
	}
}
