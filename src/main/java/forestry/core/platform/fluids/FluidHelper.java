package forestry.core.platform.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;

public final class FluidHelper {
	private FluidHelper() {
	}

	public static boolean areFluidStacksEqual(@Nullable FluidStack first, @Nullable FluidStack second) {
		return first == null ? second == null : second != null && FluidStack.matches(first, second);
	}

	public static boolean canAcceptFluid(Level world, BlockPos pos, Direction facing, FluidStack fluid, boolean checkSpace) {
		var handler = world.getCapability(Capabilities.Fluid.BLOCK, pos, facing);
		if (handler == null || fluid.isEmpty()) return false;
		int inserted = simulateFill(handler, fluid);
		return inserted > 0 && (!checkSpace || inserted == fluid.getAmount());
	}

	public static boolean canAcceptFluid(Level world, BlockPos pos, Direction facing, FluidStack fluid) {
		return canAcceptFluid(world, pos, facing, fluid, false);
	}

	public enum FillStatus {
		SUCCESS, INVALID_INPUT, NO_FLUID, NO_SPACE, NO_SPACE_FLUID
	}

	public static FillStatus fillContainers(ResourceHandler<FluidResource> fluids, Container inventory, int input, int output, Fluid fluid, boolean execute) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			FillStatus result = fillContainers(fluids, inventory, input, output, fluid, tx);
			if (execute && result == FillStatus.SUCCESS) tx.commit();
			return result;
		}
	}

	public static FillStatus fillContainers(ResourceHandler<FluidResource> fluids, Container inventory, int input, int output, Fluid fluid, TransactionContext parent) {
		ItemStack original = inventory.getItem(input).copy();
		if (original.isEmpty()) return FillStatus.INVALID_INPUT;
		SimpleContainer working = new SimpleContainer(original.copyWithCount(1));
		var container = containerHandler(working);
		if (container == null) return FillStatus.INVALID_INPUT;
		try (var tx = Transaction.open(parent)) {
			boolean hasFluid = false;
			boolean acceptsFluid = false;
			for (int index = 0; index < fluids.size(); index++) {
				var resource = fluids.getResource(index);
				if (resource.isEmpty() || !resource.is(fluid)) continue;
				hasFluid = true;
				try (var probe = Transaction.open(tx)) {
					if (container.insert(resource, Integer.MAX_VALUE, probe) == 0) continue;
				}
				acceptsFluid = true;
				int moved = ResourceHandlerUtil.move(fluids, container, resource::equals, Integer.MAX_VALUE, tx);
				if (moved == 0) continue;
				boolean full;
				try (var probe = Transaction.open(tx)) {
					var updated = containerHandler(working);
					full = updated == null || updated.insert(resource, Integer.MAX_VALUE, probe) == 0;
				}
				FillStatus result = replaceContainer(inventory, input, full ? output : input, original, working.getItem(0), tx);
				if (result == FillStatus.SUCCESS) tx.commit();
				return result;
			}
			return hasFluid && !acceptsFluid ? FillStatus.INVALID_INPUT : FillStatus.NO_FLUID;
		}
	}

	public static boolean drainContainers(ResourceHandler<FluidResource> fluids, Container inventory, int input) {
		return drainContainers(fluids, inventory, input, input, true) == FillStatus.SUCCESS;
	}

	public static FillStatus drainContainers(ResourceHandler<FluidResource> fluids, Container inventory, int input, int output, boolean execute) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			FillStatus result = drainContainers(fluids, inventory, input, output, tx);
			if (execute && result == FillStatus.SUCCESS) tx.commit();
			return result;
		}
	}

	public static FillStatus drainContainers(ResourceHandler<FluidResource> fluids, Container inventory, int input, int output, TransactionContext parent) {
		ItemStack original = inventory.getItem(input).copy();
		if (original.isEmpty()) return FillStatus.INVALID_INPUT;
		SimpleContainer working = new SimpleContainer(original.copyWithCount(1));
		var container = containerHandler(working);
		if (container == null || ResourceHandlerUtil.isEmpty(container)) return FillStatus.INVALID_INPUT;
		try (var tx = Transaction.open(parent)) {
			int moved = ResourceHandlerUtil.move(container, fluids, resource -> true, FluidType.BUCKET_VOLUME, tx);
			if (moved == 0) return FillStatus.NO_SPACE_FLUID;
			var remainder = containerHandler(working);
			boolean empty = remainder == null || ResourceHandlerUtil.isEmpty(remainder);
			FillStatus result = replaceContainer(inventory, input, empty ? output : input, original, working.getItem(0), tx);
			if (result == FillStatus.SUCCESS) tx.commit();
			return result;
		}
	}

	private static FillStatus replaceContainer(Container inventory, int input, int output, ItemStack original, ItemStack result, TransactionContext tx) {
		var items = forestry.core.platform.inventory.InventoryTransferView.internal(inventory);
		if (!ItemStack.matches(original, inventory.getItem(input))) return FillStatus.INVALID_INPUT;
		if (items.extract(input, ItemResource.of(original), 1, tx) != 1) return FillStatus.INVALID_INPUT;
		if (!result.isEmpty() && items.insert(output, ItemResource.of(result), 1, tx) != 1) return FillStatus.NO_SPACE;
		return FillStatus.SUCCESS;
	}

	@Nullable
	private static ResourceHandler<FluidResource> containerHandler(SimpleContainer inventory) {
		return ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(inventory), 0).getCapability(Capabilities.Fluid.ITEM);
	}

	@Nullable
	private static ResourceHandler<FluidResource> getFluidHandler(ItemStack stack) {
		if (stack.isEmpty()) return null;
		return containerHandler(new SimpleContainer(stack.copyWithCount(1)));
	}

	public static boolean isFillableContainer(ItemStack container, FluidStack content) {
		var handler = getFluidHandler(container);
		return handler != null && !content.isEmpty() && simulateFill(handler, content.copyWithAmount(Integer.MAX_VALUE)) > 0;
	}

	public static boolean isFillableContainerAndEmpty(ItemStack container, FluidStack content) {
		return isEmpty(container) && isFillableContainer(container, content);
	}

	public static ItemStack getEmptyContainer(ItemStack stack) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		SimpleContainer inventory = new SimpleContainer(stack.copyWithCount(1));
		var handler = containerHandler(inventory);
		if (handler == null) return ItemStack.EMPTY;
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			for (int index = 0; index < handler.size(); index++) {
				var resource = handler.getResource(index);
				if (!resource.isEmpty() && handler.extract(index, resource, Integer.MAX_VALUE, tx) == 0) return ItemStack.EMPTY;
			}
			tx.commit();
		}
		return inventory.getItem(0).copy();
	}

	public static boolean isFillableContainerWithRoom(ItemStack container) {
		var handler = getFluidHandler(container);
		if (handler == null) return false;
		for (int index = 0; index < handler.size(); index++) {
			if (handler.getAmountAsLong(index) < handler.getCapacityAsLong(index, handler.getResource(index))) return true;
		}
		return false;
	}

	public static boolean isFillableEmptyContainer(ItemStack container) {
		return isEmpty(container) && isFillableContainerWithRoom(container);
	}

	public static boolean isDrainableFilledContainer(ItemStack container) {
		var handler = getFluidHandler(container);
		if (handler == null || ResourceHandlerUtil.isEmpty(handler)) return false;
		for (int index = 0; index < handler.size(); index++) {
			var resource = handler.getResource(index);
			if (resource.isEmpty() || handler.getAmountAsLong(index) < handler.getCapacityAsLong(index, resource)) return false;
		}
		return !simulateDrain(handler, Integer.MAX_VALUE).isEmpty();
	}

	public static boolean isDrainableContainer(ItemStack container) {
		var handler = getFluidHandler(container);
		return handler != null && !simulateDrain(handler, Integer.MAX_VALUE).isEmpty();
	}

	public static boolean isEmpty(ItemStack container) {
		var handler = getFluidHandler(container);
		return handler != null && ResourceHandlerUtil.isEmpty(handler);
	}

	public static int fill(ResourceHandler<FluidResource> handler, FluidStack stack) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			int inserted = stack.isEmpty() ? 0 : handler.insert(FluidResource.of(stack), stack.getAmount(), tx);
			tx.commit();
			return inserted;
		}
	}

	public static int simulateFill(ResourceHandler<FluidResource> handler, FluidStack stack) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			return stack.isEmpty() ? 0 : handler.insert(FluidResource.of(stack), stack.getAmount(), tx);
		}
	}

	public static FluidStack drain(ResourceHandler<FluidResource> handler, FluidStack stack, TransactionContext tx) {
		return stack.isEmpty() ? FluidStack.EMPTY : stack.copyWithAmount(handler.extract(FluidResource.of(stack), stack.getAmount(), tx));
	}

	public static FluidStack drain(ResourceHandler<FluidResource> handler, int amount, TransactionContext tx) {
		var extracted = ResourceHandlerUtil.extractFirst(handler, resource -> true, amount, tx);
		return extracted == null ? FluidStack.EMPTY : extracted.resource().toStack(extracted.amount());
	}

	public static FluidStack drain(ResourceHandler<FluidResource> handler, FluidStack stack) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			FluidStack drained = drain(handler, stack, tx);
			tx.commit();
			return drained;
		}
	}

	public static FluidStack drain(ResourceHandler<FluidResource> handler, int amount) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			FluidStack drained = drain(handler, amount, tx);
			tx.commit();
			return drained;
		}
	}

	public static FluidStack simulateDrain(ResourceHandler<FluidResource> handler, FluidStack stack) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) { return drain(handler, stack, tx); }
	}

	public static FluidStack simulateDrain(ResourceHandler<FluidResource> handler, int amount) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) { return drain(handler, amount, tx); }
	}
}
