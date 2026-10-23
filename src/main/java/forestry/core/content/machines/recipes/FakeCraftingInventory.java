package forestry.core.content.machines.recipes;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.ArrayList;
import java.util.List;

class FakeCraftingInventory {
	public static CraftingInput of(Container backing) {
		List<ItemStack> items = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			items.add(backing.getItem(i));
		}
		return CraftingInput.of(3, 3, items);
	}
}
