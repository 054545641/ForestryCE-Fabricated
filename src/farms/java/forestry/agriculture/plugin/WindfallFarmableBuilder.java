package forestry.agriculture.plugin;

import com.google.common.collect.ImmutableSet;
import forestry.api.agriculture.IFarmable;
import forestry.api.agriculture.IFarmableFactory;
import forestry.api.plugin.IWindfallFarmableBuilder;
import net.minecraft.world.item.Item;

public class WindfallFarmableBuilder implements IWindfallFarmableBuilder {
	private final ImmutableSet.Builder<Item> windfalls = new ImmutableSet.Builder<>();
	private final IFarmableFactory factory;

	public WindfallFarmableBuilder(IFarmableFactory factory) {
		this.factory = factory;
	}

	@Override
	public IWindfallFarmableBuilder addWindfall(Item windfall) {
		this.windfalls.add(windfall);
		return this;
	}

	@Override
	public IWindfallFarmableBuilder addWindfall(Iterable<? extends Item> windfall) {
		this.windfalls.addAll(windfall);
		return this;
	}

	public IFarmable build(Item germling) {
		return this.factory.create(germling, this.windfalls.build());
	}
}
