package forestry.apiculture.bees;

import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.item.ITintedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemHoneyComb extends ItemForestry implements ITintedItem {
	private final EnumHoneyComb type;

	public ItemHoneyComb(EnumHoneyComb type, Properties properties) {
		super(properties);

		this.type = type;
	}

	@Override
	public int getColorFromItemStack(ItemStack itemstack, int tintIndex) {
		EnumHoneyComb honeyComb = this.type;
		if (tintIndex == 1) {
			return honeyComb.primaryColor;
		} else {
			return honeyComb.secondaryColor;
		}
	}
}
