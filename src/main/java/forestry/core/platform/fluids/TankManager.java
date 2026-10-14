package forestry.core.platform.fluids;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.serialization.MapCodec;
import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import forestry.core.platform.network.IStreamable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TankManager implements ITankManager, ITankUpdateHandler, IStreamable, INbtWritable, INbtReadable {
	private final List<StandardTank> tanks = new ArrayList<>();
	private final Table<AbstractContainerMenu, Integer, StandardTank.Contents> prevFluidStacks = HashBasedTable.create();
	@Nullable
	private final Listener listener;

	public interface Listener extends ITankUpdateHandler {
		void sendTankUpdate(ServerPlayer player, int tankIndex, FluidStack fluid, int capacity);
	}

	public TankManager() {
		this(null);
	}

	public TankManager(@Nullable Listener listener, StandardTank... tanks) {
		this.listener = listener;
		for (StandardTank tank : tanks) add(tank);
	}

	public void add(StandardTank tank) {
		if (this.tanks.contains(tank)) throw new IllegalArgumentException("Duplicate tank");
		tank.setTankIndex(this.tanks.size());
		this.tanks.add(tank);
		tank.setTankUpdateHandler(this);
	}

	@Override
	public CompoundTag write(CompoundTag data, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (int slot = 0; slot < size(); slot++) {
			StandardTank tank = this.tanks.get(slot);
			if (!tank.isEmpty() || tank.hasVariableCapacity()) {
				CompoundTag tag = tank.writeToNBT(registries, new CompoundTag());
				tag.putInt("tank", slot);
				list.add(tag);
			}
		}
		data.put("tanks", list);
		return data;
	}

	public void write(ValueOutput output) {
		var list = output.childrenList("tanks");
		for (int slot = 0; slot < size(); slot++) {
			StandardTank tank = this.tanks.get(slot);
			if (!tank.isEmpty() || tank.hasVariableCapacity()) {
				var entry = list.addChild();
				entry.store(MapCodec.assumeMapUnsafe(FluidStack.OPTIONAL_CODEC), tank.getFluid());
				entry.putInt("tank", slot);
				if (tank.hasVariableCapacity()) entry.putInt("capacity", tank.getCapacity());
			}
		}
	}

	@Override
	public void read(CompoundTag data, HolderLookup.Provider registries) {
		List<StandardTank.Contents> decoded = new ArrayList<>(size());
		for (StandardTank tank : this.tanks) decoded.add(tank.emptyContents());
		if (data.contains("tanks")) {
			ListTag list = data.getList("tanks").orElseThrow(() -> new IllegalArgumentException("Invalid tanks list"));
			var seen = new HashSet<Integer>();
			for (Tag tag : list) {
				if (!(tag instanceof CompoundTag entry)) throw new IllegalArgumentException("Invalid tank entry");
				int slot = entry.getInt("tank").orElseThrow(() -> new IllegalArgumentException("Missing tank index"));
				if (slot < 0 || slot >= size() || !seen.add(slot)) throw new IllegalArgumentException("Invalid or duplicate tank index: " + slot);
				StandardTank tank = this.tanks.get(slot);
				if (!entry.contains("id") && !(tank.hasVariableCapacity() && entry.contains("capacity"))) throw new IllegalArgumentException("Missing fluid in tank entry");
				decoded.set(slot, tank.decodeContents(registries, entry));
			}
		}
		restore(decoded);
	}

	private void restore(List<StandardTank.Contents> fluids) {
		for (int slot = 0; slot < size(); slot++) this.tanks.get(slot).restoreContents(fluids.get(slot));
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		for (StandardTank tank : this.tanks) tank.writeData(data);
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		List<StandardTank.Contents> decoded = new ArrayList<>(size());
		for (StandardTank tank : this.tanks) decoded.add(tank.decodeData(data));
		restore(decoded);
	}

	@Override
	public void sendAllTanks(AbstractContainerMenu container, ServerPlayer player) {
		for (StandardTank tank : this.tanks) sendTankUpdate(container, player, tank);
	}

	@Override
	public void onClosed(AbstractContainerMenu container) {
		this.prevFluidStacks.row(container).clear();
	}

	@Override
	public void broadcastChanges(AbstractContainerMenu container, ServerPlayer player) {
		for (StandardTank tank : this.tanks) {
			var prev = this.prevFluidStacks.get(container, tank.getTankIndex());
			if (prev == null || tank.getCapacity() != prev.capacity() || !FluidStack.matches(tank.getFluid(), prev.fluid())) sendTankUpdate(container, player, tank);
		}
	}

	private void sendTankUpdate(AbstractContainerMenu container, ServerPlayer player, StandardTank tank) {
		if (this.listener != null) {
			FluidStack fluid = tank.getFluid();
			this.listener.sendTankUpdate(player, tank.getTankIndex(), fluid, tank.getCapacity());
			this.prevFluidStacks.put(container, tank.getTankIndex(), tank.createSnapshot());
		}
	}

	@Override
	public void updateTankLevels(StandardTank tank) {
		if (this.listener != null) this.listener.updateTankLevels(tank);
	}

	@Override
	public void processTankUpdate(int tankIndex, @Nullable FluidStack contents) {
		StandardTank tank = getTank(tankIndex);
		if (tank != null) tank.restoreFluid(contents == null ? FluidStack.EMPTY : contents);
	}

	@Override
	public void processTankUpdate(int tankIndex, FluidStack contents, int capacity) {
		StandardTank tank = getTank(tankIndex);
		if (tank != null) tank.restoreContents(tank.validateContents(contents, capacity));
	}

	@Nullable
	@Override
	public StandardTank getTank(int tankIndex) {
		return tankIndex >= 0 && tankIndex < size() ? this.tanks.get(tankIndex) : null;
	}

	@Override
	public int size() { return this.tanks.size(); }
	@Override
	public FluidResource getResource(int index) { return this.tanks.get(index).getResource(0); }
	@Override
	public long getAmountAsLong(int index) { return this.tanks.get(index).getAmountAsLong(0); }
	@Override
	public long getCapacityAsLong(int index, FluidResource resource) { return this.tanks.get(index).getCapacityAsLong(0, resource); }
	@Override
	public boolean isValid(int index, FluidResource resource) { return this.tanks.get(index).isValid(0, resource); }
	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		return this.tanks.get(index).insert(0, resource, amount, transaction);
	}
	@Override
	public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
		return this.tanks.get(index).extract(0, resource, amount, transaction);
	}

	public FluidStack getFluid(int index) { return this.tanks.get(index).getFluid(); }

	@Override
	public boolean canFillFluidType(FluidStack fluidStack) {
		return this.tanks.stream().anyMatch(tank -> tank.canFill() && tank.isFluidValid(fluidStack));
	}
}
