package forestry.core.platform.fluids;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import forestry.api.core.tooltips.ToolTip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

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

	public FilteredTank setFilter(DataMapType<Fluid, ?> dataMap) {
		this.filters = () -> BuiltInRegistries.FLUID.getDataMap(dataMap).keySet().stream()
				.map(ResourceKey::identifier)
				.collect(Collectors.toSet());
		setValidator(stack -> !stack.isEmpty() && stack.typeHolder().getData(dataMap) != null);
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

	private boolean fluidMatchesFilter(FluidStack resource) {
		return resource.getFluid() != Fluids.EMPTY && this.filters.get().contains(BuiltInRegistries.FLUID.getKey(resource.getFluid()));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	protected void refreshTooltip() {
		if (hasFluid()) {
			super.refreshTooltip();
			return;
		}

		ToolTip toolTip = getToolTip();
		toolTip.clear();
		Set<Identifier> filters = this.filters.get();

		if (Minecraft.getInstance().hasShiftDown() || filters.size() < 5) {
			for (Identifier filterName : filters) {
				Fluid fluidFilter = BuiltInRegistries.FLUID.getValue(filterName);
				if (fluidFilter == null || fluidFilter == Fluids.EMPTY) {
					continue;
				}
				FluidType attributes = fluidFilter.getFluidType();
				Rarity rarity = attributes.getRarity();
				if (rarity == null) {
					rarity = Rarity.COMMON;
				}
				FluidStack filterFluidStack = new FluidStack(fluidFilter, 1);
				toolTip.add(filterFluidStack.getHoverName().copy().withStyle(rarity.getStyleModifier()));
			}
		} else {
			Component tmiComponent = Component.literal("<")
				.append(Component.translatable("for.gui.tooltip.tmi"))
				.append(Component.literal(">"));
			toolTip.add(tmiComponent, ChatFormatting.ITALIC);
		}
		toolTip.add(Component.translatable("for.gui.tooltip.liquid.amount", getFluidAmount(), getCapacity()));
	}
}
