package forestry.core.platform.render;

import forestry.api.core.fluids.FluidVolume;
import forestry.core.platform.fluids.StandardTank;

public class TankRenderInfo {
	public static final TankRenderInfo EMPTY = new TankRenderInfo(FluidVolume.EMPTY, EnumTankLevel.EMPTY);

	private final FluidVolume fluidStack;
	private final EnumTankLevel level;

	public TankRenderInfo(StandardTank fluidTank) {
		this(fluidTank.getFluid(), EnumTankLevel.rateTankLevel(fluidTank));
	}

	public TankRenderInfo(FluidVolume fluidStack, EnumTankLevel level) {
		this.fluidStack = fluidStack;
		this.level = level;
	}

	public FluidVolume getFluidVolume() {
		return this.fluidStack;
	}

	public EnumTankLevel getLevel() {
		return this.level;
	}
}
