package forestry.core.platform.registration;

import forestry.core.platform.fluids.BlockForestryFluid;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.material.FlowingFluid;
import forestry.api.core.fluids.FluidVolume;
import forestry.api.core.fluids.FluidUnits;

public interface IFluidFeature extends IModFeature {
	IBlockFeature<BlockForestryFluid, BlockItem> fluidBlock();

	FluidProperties properties();

	FlowingFluid fluid();

	FlowingFluid flowing();

	default FluidVolume fluidStack(int amount) {
		return new FluidVolume(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid()), FluidUnits.fromMilliBuckets(amount));
	}

	default FluidVolume fluidStack() {
		return fluidStack(1000);
	}
}
