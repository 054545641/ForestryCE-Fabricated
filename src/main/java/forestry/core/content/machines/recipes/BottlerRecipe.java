package forestry.core.content.machines.recipes;

import net.minecraft.world.SimpleContainer;

import net.minecraft.world.item.ItemStack;
import forestry.api.core.fluids.FluidVolume;

import javax.annotation.Nullable;

public class BottlerRecipe {
	@Nullable
	public static BottlerRecipe createEmptyingRecipe(ItemStack filled) {
		if (filled.isEmpty()) return null;
		var inventory = new SimpleContainer(filled.copyWithCount(1));
		var handler = net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.ofSingleSlot(
            net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(inventory, null).getSlot(0)).find(net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM);
		if (handler == null) return null;
		FluidVolume drained = forestry.core.platform.fluids.FluidHelper.drain(handler, Long.MAX_VALUE);
		return drained.isEmpty() ? null : new BottlerRecipe(inventory.getItem(0).copy(), drained, filled.copyWithCount(1), false);
	}

	@Nullable
	public static BottlerRecipe createFillingRecipe(FluidVolume fluid, ItemStack empty) {
		if (empty.isEmpty() || fluid.isEmpty()) return null;
		var inventory = new SimpleContainer(empty.copyWithCount(1));
		var handler = net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.ofSingleSlot(
            net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(inventory, null).getSlot(0)).find(net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM);
		if (handler == null) return null;
		long amount = forestry.core.platform.fluids.FluidHelper.fill(handler, fluid.copyWithAmount(Long.MAX_VALUE));
		return amount == 0 ? null : new BottlerRecipe(empty.copyWithCount(1), fluid.copyWithAmount(amount), inventory.getItem(0).copy(), true);
	}

	public final FluidVolume fluid;
	public final ItemStack inputStack;
	public final ItemStack outputStack;
	public final boolean fillRecipe;

	public BottlerRecipe(ItemStack inputStack, FluidVolume fluid, ItemStack outputStack, boolean fillRecipe) {
		this.fluid = fluid;
		this.inputStack = inputStack;
		this.outputStack = outputStack;
		this.fillRecipe = fillRecipe;
	}

	public boolean matchEmpty(ItemStack emptyCan, FluidVolume resource) {
		return !emptyCan.isEmpty() && ItemStack.isSameItemSameComponents(emptyCan, this.inputStack) && FluidVolume.isSameFluidSameComponents(resource, this.fluid) && this.fillRecipe;
	}

	public boolean matchFilled(ItemStack filledCan) {
		return !this.outputStack.isEmpty() && !this.fillRecipe && ItemStack.isSameItemSameComponents(this.outputStack, filledCan);
	}
}
