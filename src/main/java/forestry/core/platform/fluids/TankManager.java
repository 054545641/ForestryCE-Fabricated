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
import forestry.api.core.fluids.FluidVolume;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TankManager implements ITankManager, ITankUpdateHandler, IStreamable, INbtWritable, INbtReadable {
	private final List<StandardTank> tanks = new ArrayList<>();
	private final Table<AbstractContainerMenu, Integer, StandardTank.Contents> prevFluidVolumes = HashBasedTable.create();
	@Nullable
	private final Listener listener;

	public interface Listener extends ITankUpdateHandler {
		void sendTankUpdate(ServerPlayer player, int tankIndex, FluidVolume fluid, long capacity);
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
				entry.store(MapCodec.assumeMapUnsafe(FluidVolume.CODEC), tank.getFluid());
				entry.putInt("tank", slot);
				if (tank.hasVariableCapacity()) entry.putLong("capacity", tank.getCapacity());
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
		this.prevFluidVolumes.row(container).clear();
	}

	@Override
	public void broadcastChanges(AbstractContainerMenu container, ServerPlayer player) {
		for (StandardTank tank : this.tanks) {
			var prev = this.prevFluidVolumes.get(container, tank.getTankIndex());
			if (prev == null || tank.getCapacity() != prev.capacity() || !tank.getFluid().equals(prev.fluid())) sendTankUpdate(container, player, tank);
		}
	}

	private void sendTankUpdate(AbstractContainerMenu container, ServerPlayer player, StandardTank tank) {
		if (this.listener != null) {
			FluidVolume fluid = tank.getFluid();
			this.listener.sendTankUpdate(player, tank.getTankIndex(), fluid, tank.getCapacity());
			this.prevFluidVolumes.put(container, tank.getTankIndex(), tank.createSnapshot());
		}
	}

	@Override
	public void updateTankLevels(StandardTank tank) {
		if (this.listener != null) this.listener.updateTankLevels(tank);
	}

	@Override
	public void processTankUpdate(int tankIndex, @Nullable FluidVolume contents) {
		StandardTank tank = getTank(tankIndex);
		if (tank != null) tank.restoreContents(tank.validateContents(contents == null ? FluidVolume.EMPTY : contents, tank.getCapacity()));
	}

	@Override
	public void processTankUpdate(int tankIndex, FluidVolume contents, long capacity) {
		StandardTank tank = getTank(tankIndex);
		if (tank != null) tank.restoreContents(tank.validateContents(contents, capacity));
	}

	@Nullable
	@Override
	public StandardTank getTank(int tankIndex) {
		return tankIndex >= 0 && tankIndex < size() ? this.tanks.get(tankIndex) : null;
	}

    public int size() { return this.tanks.size(); }
    @Override public int getSlotCount() { return size(); }
    @Override public StandardTank getSlot(int index) { return this.tanks.get(index); }
    @Override public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>> iterator() {
        return new java.util.ArrayList<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<FluidVariant>>(this.tanks).iterator();
    }
    @Override public long insert(FluidVariant resource, long amount, TransactionContext transaction) {
        net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions.notBlankNotNegative(resource, amount);
        long moved = 0;
        for (var tank : this.tanks) moved += tank.insert(resource, amount - moved, transaction);
        return moved;
    }
    @Override public long extract(FluidVariant resource, long amount, TransactionContext transaction) {
        net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions.notBlankNotNegative(resource, amount);
        long moved = 0;
        for (var tank : this.tanks) moved += tank.extract(resource, amount - moved, transaction);
        return moved;
    }

	public FluidVolume getFluid(int index) { return this.tanks.get(index).getFluid(); }

	@Override
	public boolean canFillFluidType(FluidVolume fluidStack) {
		return this.tanks.stream().anyMatch(tank -> tank.supportsInsertion() && tank.isFluidValid(fluidStack));
	}
}
