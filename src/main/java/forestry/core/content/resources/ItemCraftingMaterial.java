package forestry.core.content.resources;

import forestry.core.content.resources.EnumCraftingMaterial;
import net.minecraft.world.item.Item;
import forestry.core.platform.item.ItemForestry;

public class ItemCraftingMaterial extends ItemForestry {
	private final EnumCraftingMaterial type;

	public ItemCraftingMaterial(EnumCraftingMaterial type, Properties properties) {
		super(properties);
		this.type = type;
	}

	public EnumCraftingMaterial getType() {
		return this.type;
	}
}
