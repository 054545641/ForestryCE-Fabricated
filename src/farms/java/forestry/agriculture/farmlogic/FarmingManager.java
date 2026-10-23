package forestry.agriculture.farmlogic;

import com.google.common.collect.ImmutableMap;
import forestry.api.ForestryDataMaps;
import forestry.api.agriculture.IFarmType;
import forestry.api.agriculture.IFarmingManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class FarmingManager implements IFarmingManager {
	private final ImmutableMap<Identifier, IFarmType> farmTypes;

	public FarmingManager(ImmutableMap<Identifier, IFarmType> farmTypes) {
		this.farmTypes = farmTypes;
	}

	@Override
	public int getFertilizeValue(ItemStack stack) {
		Integer value = ForestryDataMaps.current().get(ForestryDataMaps.FARM_FERTILIZERS, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
		return value != null ? value : 0;
	}

	@Nullable
	@Override
	public IFarmType getFarmType(Identifier id) {
		return this.farmTypes.get(id);
	}
}
