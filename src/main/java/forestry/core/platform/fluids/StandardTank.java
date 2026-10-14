package forestry.core.platform.fluids;

import forestry.api.core.tooltips.ToolTip;
import forestry.core.platform.network.IStreamable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Predicate;

public class StandardTank extends SnapshotJournal<StandardTank.Contents> implements ResourceHandler<FluidResource>, IStreamable {
	private ITankUpdateHandler tankUpdateHandler = tank -> {};
	private int tankIndex;
	private int capacity;
	private final int initialCapacity;
	private boolean variableCapacity;
	private final boolean canFill;
	private final boolean canDrain;
	private FluidStack fluid = FluidStack.EMPTY;
	private Predicate<FluidStack> validator = stack -> true;
	private final ResourceHandler<FluidResource> internal = new InternalHandler();
	@Nullable
	protected ToolTip toolTip;

	public StandardTank(int capacity, boolean canFill, boolean canDrain) {
		TransferPreconditions.checkNonNegative(capacity);
		this.capacity = capacity;
		this.initialCapacity = capacity;
		this.canFill = canFill;
		this.canDrain = canDrain;
	}

	public StandardTank(int capacity) {
		this(capacity, true, true);
	}

	public static StandardTank variableCapacity(int capacity) {
		StandardTank tank = new StandardTank(capacity, false, false);
		tank.variableCapacity = true;
		return tank;
	}

	public boolean hasVariableCapacity() { return this.variableCapacity; }

	public void setCapacity(int capacity) {
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			setCapacity(capacity, transaction);
			transaction.commit();
		}
	}

	public void setCapacity(int capacity, TransactionContext transaction) {
		validateContents(this.fluid, capacity);
		if (capacity != this.capacity) {
			updateSnapshots(transaction);
			this.capacity = capacity;
		}
	}

	public void setTankIndex(int index) {
		this.tankIndex = index;
	}

	public void setTankUpdateHandler(ITankUpdateHandler handler) {
		this.tankUpdateHandler = Objects.requireNonNull(handler);
	}

	public int getTankIndex() { return this.tankIndex; }
	public int getCapacity() { return this.capacity; }
	public int getFluidAmount() { return this.fluid.getAmount(); }
	public FluidStack getFluid() { return this.fluid.copy(); }
	public boolean isEmpty() { return this.fluid.isEmpty(); }
	public boolean isFull() { return !isEmpty() && getFluidAmount() >= this.capacity; }
	public int getRemainingSpace() { return Math.max(0, this.capacity - getFluidAmount()); }
	public boolean canFill() { return this.canFill; }
	public boolean canDrain() { return this.canDrain; }
	protected boolean hasFluid() { return !isEmpty(); }

	@Nullable
	public Fluid getFluidType() {
		return isEmpty() ? null : this.fluid.getFluid();
	}

	public StandardTank setValidator(Predicate<FluidStack> validator) {
		this.validator = Objects.requireNonNull(validator);
		return this;
	}

	public boolean isFluidValid(FluidStack stack) {
		return !stack.isEmpty() && this.validator.test(stack);
	}

	public ResourceHandler<FluidResource> internal() {
		return this.internal;
	}

	@Override
	public int size() { return 1; }

	@Override
	public FluidResource getResource(int index) {
		Objects.checkIndex(index, 1);
		return FluidResource.of(this.fluid);
	}

	@Override
	public long getAmountAsLong(int index) {
		Objects.checkIndex(index, 1);
		return getFluidAmount();
	}

	@Override
	public long getCapacityAsLong(int index, FluidResource resource) {
		Objects.checkIndex(index, 1);
		return resource.isEmpty() || isValid(index, resource) ? this.capacity : 0;
	}

	@Override
	public boolean isValid(int index, FluidResource resource) {
		Objects.checkIndex(index, 1);
		TransferPreconditions.checkNonEmpty(resource);
		return isFluidValid(resource.toStack(1));
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, 1);
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return this.canFill && isValid(index, resource) ? insertInternal(resource, amount, transaction) : 0;
	}

	public int insertInternal(FluidResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (!isEmpty() && !resource.matches(this.fluid)) return 0;
		int inserted = Math.min(amount, getRemainingSpace());
		if (inserted > 0) {
			updateSnapshots(transaction);
			this.fluid = resource.toStack(getFluidAmount() + inserted);
		}
		return inserted;
	}

	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, 1);
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return this.canDrain ? extractInternal(resource, amount, transaction) : 0;
	}

	public int extractInternal(FluidResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (!resource.matches(this.fluid)) return 0;
		int extracted = Math.min(amount, getFluidAmount());
		if (extracted > 0) {
			updateSnapshots(transaction);
			this.fluid = resource.toStack(getFluidAmount() - extracted);
		}
		return extracted;
	}

	record Contents(FluidStack fluid, int capacity) {
	}

	@Override
	protected Contents createSnapshot() { return new Contents(this.fluid.copy(), this.capacity); }

	@Override
	protected void revertToSnapshot(Contents snapshot) {
		this.fluid = snapshot.fluid;
		this.capacity = snapshot.capacity;
	}

	@Override
	protected void onRootCommit(Contents original) {
		if (original.capacity != this.capacity || !FluidStack.matches(original.fluid, this.fluid)) this.tankUpdateHandler.updateTankLevels(this);
	}

	public void setFluid(FluidStack fluid) {
		int capacity = this.variableCapacity ? Math.max(this.capacity, fluid.getAmount()) : this.capacity;
		Contents contents = validateContents(fluid, capacity);
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			updateSnapshots(transaction);
			restoreContents(contents);
			transaction.commit();
		}
	}

	void restoreFluid(FluidStack fluid) {
		int capacity = this.variableCapacity ? Math.max(this.capacity, fluid.getAmount()) : this.capacity;
		restoreContents(validateContents(fluid, capacity));
	}

	void restoreContents(Contents contents) {
		this.fluid = contents.fluid.copy();
		this.capacity = contents.capacity;
	}

	Contents emptyContents() { return new Contents(FluidStack.EMPTY, this.initialCapacity); }

	Contents validateContents(FluidStack fluid, int capacity) {
		if (capacity < 0 || (!this.variableCapacity && capacity != this.initialCapacity) || fluid.getAmount() < 0 || fluid.getAmount() > capacity) {
			throw new IllegalArgumentException("Invalid tank contents or capacity: " + fluid.getAmount() + "/" + capacity);
		}
		return new Contents(fluid, capacity);
	}

	Contents decodeContents(HolderLookup.Provider registries, CompoundTag tag) {
		CompoundTag fluidData = tag.copy();
		fluidData.remove("tank");
		fluidData.remove("capacity");
		FluidStack fluid = FluidStack.OPTIONAL_CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), fluidData).getOrThrow();
		int capacity = this.initialCapacity;
		if (this.variableCapacity) {
			capacity = tag.contains("capacity")
				? tag.getInt("capacity").orElseThrow(() -> new IllegalArgumentException("Invalid tank capacity"))
				: Math.max(this.initialCapacity, fluid.getAmount());
		}
		return validateContents(fluid, capacity);
	}

	Contents decodeData(RegistryFriendlyByteBuf data) {
		FluidStack fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode(data);
		return validateContents(fluid, this.variableCapacity ? data.readVarInt() : this.initialCapacity);
	}

	@Override
	public String toString() {
		return "Tank: %s, %d/%d".formatted(isEmpty() ? "Empty" : BuiltInRegistries.FLUID.getKey(this.fluid.getFluid()), getFluidAmount(), this.capacity);
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		FluidStack.OPTIONAL_STREAM_CODEC.encode(data, this.fluid);
		if (this.variableCapacity) data.writeVarInt(this.capacity);
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		restoreContents(decodeData(data));
	}

	public CompoundTag writeToNBT(HolderLookup.Provider registries, CompoundTag tag) {
		CompoundTag encoded = (CompoundTag) FluidStack.OPTIONAL_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), this.fluid).getOrThrow();
		tag.remove("id");
		tag.remove("amount");
		tag.remove("components");
		tag.merge(encoded);
		if (this.variableCapacity) tag.putInt("capacity", this.capacity);
		return tag;
	}

	public void readFromNBT(HolderLookup.Provider registries, CompoundTag tag) {
		restoreContents(decodeContents(registries, tag));
	}

	private class InternalHandler implements ResourceHandler<FluidResource> {
		@Override
		public int size() { return 1; }
		@Override
		public FluidResource getResource(int index) { return StandardTank.this.getResource(index); }
		@Override
		public long getAmountAsLong(int index) { return StandardTank.this.getAmountAsLong(index); }
		@Override
		public long getCapacityAsLong(int index, FluidResource resource) {
			Objects.checkIndex(index, 1);
			return capacity;
		}
		@Override
		public boolean isValid(int index, FluidResource resource) {
			Objects.checkIndex(index, 1);
			TransferPreconditions.checkNonEmpty(resource);
			return true;
		}
		@Override
		public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
			Objects.checkIndex(index, 1);
			return insertInternal(resource, amount, transaction);
		}
		@Override
		public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
			Objects.checkIndex(index, 1);
			return extractInternal(resource, amount, transaction);
		}
	}

	public ToolTip getToolTip() {
		if (this.toolTip == null) {
			this.toolTip = new TankToolTip(this);
		}
		return this.toolTip;
	}

	protected void refreshTooltip() {
		ToolTip toolTip = getToolTip();
		toolTip.clear();
		int amount = 0;
		FluidStack fluidStack = getFluid();
		if (!fluidStack.isEmpty()) {
			Fluid fluidType = fluidStack.getFluid();
			FluidType attributes = fluidType.getFluidType();
			Rarity rarity = attributes.getRarity();
			if (rarity == null) {
				rarity = Rarity.COMMON;
			}
			toolTip.add(fluidStack.getHoverName().copy().withStyle(rarity.getStyleModifier()));
			amount = getFluid().getAmount();
		}
		Component liquidAmount = Component.translatable("for.gui.tooltip.liquid.amount", amount, getCapacity());
		toolTip.add(liquidAmount);
	}

	private static class TankToolTip extends ToolTip {
		private final StandardTank standardTank;

		public TankToolTip(StandardTank standardTank) {
			this.standardTank = standardTank;
		}

		@Override
		public void refresh() {
			this.standardTank.refreshTooltip();
		}
	}
}
