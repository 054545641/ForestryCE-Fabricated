package forestry.core.platform.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import forestry.api.core.fluids.FluidVolume;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import javax.annotation.Nullable;

public final class FluidHelper {
    public static FluidVolume getFluidContained(ItemStack stack) {
        var storage = getFluidHandler(stack);
        if (storage != null) for (var view : storage) if (!view.isResourceBlank()) return new FluidVolume(view.getResource(), view.getAmount());
        return FluidVolume.EMPTY;
    }
	private static boolean isEmptyStorage(Storage<FluidVariant> storage) {
        for (var view : storage) if (!view.isResourceBlank() && view.getAmount() > 0) return false;
        return true;
    }
    private FluidHelper() {
	}

	public static boolean areFluidVolumesEqual(@Nullable FluidVolume first, @Nullable FluidVolume second) {
		return first == null ? second == null : second != null && first.equals(second);
	}

	public static boolean canAcceptFluid(Level world, BlockPos pos, Direction facing, FluidVolume fluid, boolean checkSpace) {
		if (!world.hasChunkAt(pos)) return false;
		var handler = FluidStorage.SIDED.find(world, pos, facing);
		if (handler == null || fluid.isEmpty()) return false;
		long inserted = simulateFill(handler, fluid);
		return inserted > 0 && (!checkSpace || inserted == fluid.amount());
	}

	public static boolean canAcceptFluid(Level world, BlockPos pos, Direction facing, FluidVolume fluid) {
		return canAcceptFluid(world, pos, facing, fluid, false);
	}

	public enum FillStatus {
		SUCCESS, INVALID_INPUT, NO_FLUID, NO_SPACE, NO_SPACE_FLUID
	}

	public static FillStatus fillContainers(Storage<FluidVariant> fluids, Container inventory, int input, int output, Fluid fluid, boolean execute) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			FillStatus result = fillContainers(fluids, inventory, input, output, fluid, tx);
			if (execute && result == FillStatus.SUCCESS) tx.commit();
			return result;
		}
	}

	public static FillStatus fillContainers(Storage<FluidVariant> fluids, Container inventory, int input, int output, Fluid fluid, TransactionContext parent) {
		ItemStack original = inventory.getItem(input).copy();
		if (original.isEmpty()) return FillStatus.INVALID_INPUT;
		SimpleContainer working = new SimpleContainer(original.copyWithCount(1));
		var container = containerHandler(working);
		if (container == null) return FillStatus.INVALID_INPUT;
		try (var tx = Transaction.openNested(parent)) {
			boolean hasFluid = false;
			boolean acceptsFluid = false;
			for (var view : fluids) {
				var resource = view.getResource();
				if (resource.isBlank() || !resource.isOf(fluid)) continue;
				hasFluid = true;
				try (var probe = Transaction.openNested(tx)) {
					if (container.insert(resource, Long.MAX_VALUE, probe) == 0) continue;
				}
				acceptsFluid = true;
				long moved = StorageUtil.move(fluids, container, resource::equals, Long.MAX_VALUE, tx);
				if (moved == 0) continue;
				boolean full;
				try (var probe = Transaction.openNested(tx)) {
					var updated = containerHandler(working);
					full = updated == null || updated.insert(resource, Long.MAX_VALUE, probe) == 0;
				}
				FillStatus result = replaceContainer(inventory, input, full ? output : input, original, working.getItem(0), tx);
				if (result == FillStatus.SUCCESS) tx.commit();
				return result;
			}
			return hasFluid && !acceptsFluid ? FillStatus.INVALID_INPUT : FillStatus.NO_FLUID;
		}
	}

	public static boolean drainContainers(Storage<FluidVariant> fluids, Container inventory, int input) {
		return drainContainers(fluids, inventory, input, input, true) == FillStatus.SUCCESS;
	}

	public static FillStatus drainContainers(Storage<FluidVariant> fluids, Container inventory, int input, int output, boolean execute) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			FillStatus result = drainContainers(fluids, inventory, input, output, tx);
			if (execute && result == FillStatus.SUCCESS) tx.commit();
			return result;
		}
	}

	public static FillStatus drainContainers(Storage<FluidVariant> fluids, Container inventory, int input, int output, TransactionContext parent) {
		ItemStack original = inventory.getItem(input).copy();
		if (original.isEmpty()) return FillStatus.INVALID_INPUT;
		SimpleContainer working = new SimpleContainer(original.copyWithCount(1));
		var container = containerHandler(working);
		if (container == null || isEmptyStorage(container)) return FillStatus.INVALID_INPUT;
		try (var tx = Transaction.openNested(parent)) {
			long moved = StorageUtil.move(container, fluids, resource -> true, FluidConstants.BUCKET, tx);
			if (moved == 0) return FillStatus.NO_SPACE_FLUID;
			var remainder = containerHandler(working);
			boolean empty = remainder == null || isEmptyStorage(remainder);
			FillStatus result = replaceContainer(inventory, input, empty ? output : input, original, working.getItem(0), tx);
			if (result == FillStatus.SUCCESS) tx.commit();
			return result;
		}
	}

	private static FillStatus replaceContainer(Container inventory, int input, int output, ItemStack original, ItemStack result, TransactionContext tx) {
		var items = forestry.core.platform.inventory.InventoryTransferView.internal(inventory);
		if (!ItemStack.matches(original, inventory.getItem(input))) return FillStatus.INVALID_INPUT;
		if (items.getSlot(input).extract(ItemVariant.of(original), 1, tx) != 1) return FillStatus.INVALID_INPUT;
		if (!result.isEmpty() && items.getSlot(output).insert(ItemVariant.of(result), 1, tx) != 1) return FillStatus.NO_SPACE;
		return FillStatus.SUCCESS;
	}

	@Nullable
	private static Storage<FluidVariant> containerHandler(SimpleContainer inventory) {
		return ContainerItemContext.ofSingleSlot(ContainerStorage.of(inventory, null).getSlot(0)).find(FluidStorage.ITEM);
	}

	@Nullable
	private static Storage<FluidVariant> getFluidHandler(ItemStack stack) {
		if (stack.isEmpty()) return null;
		return containerHandler(new SimpleContainer(stack.copyWithCount(1)));
	}

	public static boolean isFillableContainer(ItemStack container, FluidVolume content) {
		var handler = getFluidHandler(container);
		return handler != null && !content.isEmpty() && simulateFill(handler, content.withAmount(Long.MAX_VALUE)) > 0;
	}

	public static boolean isFillableContainerAndEmpty(ItemStack container, FluidVolume content) {
		return isEmpty(container) && isFillableContainer(container, content);
	}

	public static ItemStack getEmptyContainer(ItemStack stack) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		SimpleContainer inventory = new SimpleContainer(stack.copyWithCount(1));
		var handler = containerHandler(inventory);
		if (handler == null) return ItemStack.EMPTY;
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			for (var view : handler) {
				var resource = view.getResource();
				if (!resource.isBlank() && view.extract(resource, Long.MAX_VALUE, tx) == 0) return ItemStack.EMPTY;
			}
			tx.commit();
		}
		return inventory.getItem(0).copy();
	}

	public static boolean isFillableContainerWithRoom(ItemStack container) {
		var handler = getFluidHandler(container);
		if (handler == null) return false;
		for (var view : handler) {
			if (view.getAmount() < view.getCapacity()) return true;
		}
		return false;
	}

	public static boolean isFillableEmptyContainer(ItemStack container) {
		return isEmpty(container) && isFillableContainerWithRoom(container);
	}

	public static boolean isDrainableFilledContainer(ItemStack container) {
		var handler = getFluidHandler(container);
		if (handler == null || isEmptyStorage(handler)) return false;
		for (var view : handler) {
			if (view.isResourceBlank() || view.getAmount() < view.getCapacity()) return false;
		}
		return !simulateDrain(handler, Long.MAX_VALUE).isEmpty();
	}

	public static boolean isDrainableContainer(ItemStack container) {
		var handler = getFluidHandler(container);
		return handler != null && !simulateDrain(handler, Long.MAX_VALUE).isEmpty();
	}

	public static boolean isEmpty(ItemStack container) {
		var handler = getFluidHandler(container);
		return handler != null && isEmptyStorage(handler);
	}

	public static long fill(Storage<FluidVariant> handler, FluidVolume stack) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			long inserted = stack.isEmpty() ? 0 : handler.insert(stack.variant(), stack.amount(), tx);
			tx.commit();
			return inserted;
		}
	}

	public static long simulateFill(Storage<FluidVariant> handler, FluidVolume stack) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			return stack.isEmpty() ? 0 : handler.insert(stack.variant(), stack.amount(), tx);
		}
	}

	public static FluidVolume drain(Storage<FluidVariant> handler, FluidVolume stack, TransactionContext tx) {
		return stack.isEmpty() ? FluidVolume.EMPTY : stack.withAmount(handler.extract(stack.variant(), stack.amount(), tx));
	}

	public static FluidVolume drain(Storage<FluidVariant> handler, long amount, TransactionContext tx) {
        net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions.notNegative(amount);
        for (var view : handler) {
            var variant = view.getResource();
            if (variant.isBlank()) continue;
            long extracted = handler.extract(variant, amount, tx);
            if (extracted > 0) return new FluidVolume(variant, extracted);
        }
        return FluidVolume.EMPTY;
	}

	public static FluidVolume drain(Storage<FluidVariant> handler, FluidVolume stack) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			FluidVolume drained = drain(handler, stack, tx);
			tx.commit();
			return drained;
		}
	}

	public static FluidVolume drain(Storage<FluidVariant> handler, long amount) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			FluidVolume drained = drain(handler, amount, tx);
			tx.commit();
			return drained;
		}
	}

	public static FluidVolume simulateDrain(Storage<FluidVariant> handler, FluidVolume stack) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) { return drain(handler, stack, tx); }
	}

	public static FluidVolume simulateDrain(Storage<FluidVariant> handler, long amount) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) { return drain(handler, amount, tx); }
	}
}
