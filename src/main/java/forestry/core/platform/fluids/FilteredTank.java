package forestry.core.platform.fluids;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import forestry.api.core.fluids.FluidVolume;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import forestry.api.core.data.DataMapKey;

public class FilteredTank extends StandardTank {
	private Supplier<Set<Identifier>> filters = Suppliers.ofInstance(Set.of());

	public FilteredTank(int capacity) {
		super(capacity);
		setValidator(this::fluidMatchesFilter);
	}

	public FilteredTank(int capacity, boolean canFill, boolean canDrain) {
		super(capacity, canFill, canDrain);
		setValidator(this::fluidMatchesFilter);
	}

	public FilteredTank setFilter(Supplier<Set<Identifier>> filters) {
		this.filters = Preconditions.checkNotNull(filters);
		setValidator(this::fluidMatchesFilter);
		return this;
	}

	public FilteredTank setFilter(DataMapKey<Fluid, ?> dataMap) {
        this.filters = () -> forestry.api.ForestryDataMaps.current().values().getOrDefault(dataMap.id(), java.util.Map.of()).keySet();
        setValidator(stack -> !stack.isEmpty() && forestry.api.ForestryDataMaps.current().get(dataMap, BuiltInRegistries.FLUID.getKey(stack.variant().getFluid())) != null);
		return this;
	}

	public FilteredTank setFilters(Collection<Fluid> filters) {
		Set<Identifier> set = new HashSet<>();
		this.filters = () -> set;
		setValidator(this::fluidMatchesFilter);
		for (Fluid fluid : filters) {
			set.add(BuiltInRegistries.FLUID.getKey(fluid));
		}
		return this;
	}

	private boolean fluidMatchesFilter(FluidVolume resource) {
		return resource.variant().getFluid() != Fluids.EMPTY && this.filters.get().contains(BuiltInRegistries.FLUID.getKey(resource.variant().getFluid()));
	}

}
