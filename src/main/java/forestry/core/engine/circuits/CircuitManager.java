package forestry.core.engine.circuits;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import forestry.api.core.circuits.*;
import forestry.core.platform.util.NBTUtilForestry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class CircuitManager implements ICircuitManager {
	private final ImmutableMultimap<ICircuitLayout, CircuitHolder> circuitHolders;
	private final ImmutableMap<String, ICircuitLayout> layoutsById;
	private final ImmutableMap<String, ICircuit> circuitsById;

	public CircuitManager(ImmutableMultimap<ICircuitLayout, CircuitHolder> circuitHolders, ImmutableMap<String, ICircuitLayout> layoutsById, ImmutableMap<String, ICircuit> circuitsById) {
		this.circuitHolders = circuitHolders;
		this.layoutsById = layoutsById;
		this.circuitsById = circuitsById;
	}

	@Override
	public List<ICircuitLayout> getLayouts() {
		return this.layoutsById.values().asList();
	}

	@Nullable
	@Override
	public ICircuit getCircuit(ICircuitLayout layout, ItemStack stack) {
		for (CircuitHolder holder : this.circuitHolders.get(layout)) {
			if (ItemStack.isSameItem(holder.stack(), stack)) {
				return holder.circuit();
			}
		}
		return null;
	}

	@Nullable
	@Override
	public ICircuit getCircuit(String circuitId) {
		return this.circuitsById.get(circuitId);
	}

	@Nullable
	@Override
	public ICircuitLayout getLayout(String layoutId) {
		return this.layoutsById.get(layoutId);
	}

	@Nullable
	@Override
	public ICircuitBoard getCircuitBoard(ItemStack stack) {
		CompoundTag tag = NBTUtilForestry.getItemStackTag(stack);
		return tag == null ? null : new CircuitBoard(tag);
	}

	@Override
	public boolean isCircuitBoard(ItemStack stack) {
		return stack.getItem() instanceof ItemCircuitBoard;
	}

	@Override
	public Collection<CircuitHolder> getCircuitHolders() {
		return this.circuitHolders.values();
	}
}
