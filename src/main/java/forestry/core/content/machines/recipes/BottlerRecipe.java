package forestry.core.content.machines.recipes;

import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.world.SimpleContainer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

public class BottlerRecipe {
	@Nullable
	public static BottlerRecipe createEmptyingRecipe(ItemStack filled) {
		if (filled.isEmpty()) return null;
		var inventory = new SimpleContainer(filled.copyWithCount(1));
		var handler = ItemAccess.forHandlerIndexStrict(
			VanillaContainerWrapper.of(inventory), 0).getCapability(Capabilities.Fluid.ITEM);
		if (handler == null) return null;
		FluidStack drained = forestry.core.platform.fluids.FluidHelper.drain(handler, Integer.MAX_VALUE);
		return drained.isEmpty() ? null : new BottlerRecipe(inventory.getItem(0).copy(), drained, filled.copyWithCount(1), false);
	}

	@Nullable
	public static BottlerRecipe createFillingRecipe(FluidStack fluid, ItemStack empty) {
		if (empty.isEmpty() || fluid.isEmpty()) return null;
		var inventory = new SimpleContainer(empty.copyWithCount(1));
		var handler = ItemAccess.forHandlerIndexStrict(
			VanillaContainerWrapper.of(inventory), 0).getCapability(Capabilities.Fluid.ITEM);
		if (handler == null) return null;
		int amount = forestry.core.platform.fluids.FluidHelper.fill(handler, fluid.copyWithAmount(Integer.MAX_VALUE));
		return amount == 0 ? null : new BottlerRecipe(empty.copyWithCount(1), fluid.copyWithAmount(amount), inventory.getItem(0).copy(), true);
	}

	public final FluidStack fluid;
	public final ItemStack inputStack;
	public final ItemStack outputStack;
	public final boolean fillRecipe;

	public BottlerRecipe(ItemStack inputStack, FluidStack fluid, ItemStack outputStack, boolean fillRecipe) {
		this.fluid = fluid;
		this.inputStack = inputStack;
		this.outputStack = outputStack;
		this.fillRecipe = fillRecipe;
	}

	public boolean matchEmpty(ItemStack emptyCan, FluidStack resource) {
		return !emptyCan.isEmpty() && ItemStack.isSameItemSameComponents(emptyCan, this.inputStack) && FluidStack.isSameFluidSameComponents(resource, this.fluid) && this.fillRecipe;
	}

	public boolean matchFilled(ItemStack filledCan) {
		return !this.outputStack.isEmpty() && !this.fillRecipe && ItemStack.isSameItemSameComponents(this.outputStack, filledCan);
	}
}
