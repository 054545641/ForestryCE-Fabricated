package forestry.apiimpl.fake;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import forestry.api.agriculture.IFarmType;
import forestry.api.agriculture.IFarmingManager;

/**
 * The farming manager used when the agriculture module is absent. Farms find no types and no
 * fertilizer has any value, so a farm block installed by another mod idles instead of crashing.
 */
public enum FakeFarmingManager implements IFarmingManager {
	INSTANCE;

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public int getFertilizeValue(ItemStack stack) {
		return 0;
	}

	@Nullable
	@Override
	public IFarmType getFarmType(Identifier id) {
		return null;
	}
}
