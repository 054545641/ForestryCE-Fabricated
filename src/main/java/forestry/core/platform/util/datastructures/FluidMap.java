package forestry.core.platform.util.datastructures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.Serial;

public class FluidMap<T> extends StackMap<Fluid, T> {
	@Serial
	private static final long serialVersionUID = 15891293315299994L;

	@Override
	protected boolean areEqual(Fluid a, Fluid b) {
		return a.equals(b);
	}

	@Override
	protected boolean isValidKey(Object key) {
		return key instanceof FluidStack || key instanceof Fluid || key instanceof String || key instanceof Identifier;
	}

	@Override
	protected Fluid getStack(Object key) {
		if (key instanceof FluidStack) {
			return ((FluidStack) key).getFluid();
		}
		if (key instanceof Fluid) {
			return (Fluid) key;
		}
		if (key instanceof String) {
			return BuiltInRegistries.FLUID.getValue(Identifier.parse((String) key));
		}
		if (key instanceof Identifier) {
			return BuiltInRegistries.FLUID.getValue((Identifier) key);
		}
		return null;
	}
}
