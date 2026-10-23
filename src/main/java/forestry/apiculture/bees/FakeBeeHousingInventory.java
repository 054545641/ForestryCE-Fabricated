package forestry.apiculture.bees;

import forestry.api.apiculture.IBeeHousingInventory;
import net.minecraft.world.item.ItemStack;

public enum FakeBeeHousingInventory implements IBeeHousingInventory {
	INSTANCE;

	@Override
	public ItemStack getQueen() {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getDrone() {
		return ItemStack.EMPTY;
	}

	@Override
	public void setQueen(ItemStack stack) {
	}

	@Override
	public void setDrone(ItemStack stack) {
	}

	@Override
	public boolean addProduct(ItemStack product, boolean all) {
		return false;
	}
}
