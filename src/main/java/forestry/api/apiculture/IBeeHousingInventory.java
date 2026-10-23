package forestry.api.apiculture;

import net.minecraft.world.item.ItemStack;

public interface IBeeHousingInventory {
	ItemStack getQueen();

	ItemStack getDrone();

	void setQueen(ItemStack stack);

	void setDrone(ItemStack stack);

	/**
	 * Adds products to the housing's inventory.
	 *
	 * @param product ItemStack with the product to add.
	 * @param all     if true, success requires that all products are added
	 * @return boolean indicating success or failure.
	 */
	boolean addProduct(ItemStack product, boolean all);
}
