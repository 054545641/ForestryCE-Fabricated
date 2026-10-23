package forestry.core.platform.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;
import java.util.function.IntFunction;

/** Crafting matching shared by inventory transactions and stack utilities. */
public final class InventoryCrafting {
	private static final int[] EMPTY_CONSUME = new int[0];
	public static boolean isCraftingEquivalent(ItemStack base, ItemStack comparison) {
		if (base.isEmpty() || comparison.isEmpty()) {
			return false;
		}

		if (base.getItem() != comparison.getItem()) {
			return false;
		}

		CustomData baseData = base.get(DataComponents.CUSTOM_DATA);
		if (baseData == null || baseData.isEmpty()) {
			return true;
		}
		CustomData comparisonData = comparison.get(DataComponents.CUSTOM_DATA);
		return baseData.equals(comparisonData);
	}

	/**
	 * Compare two item stacks for crafting equivalency.
	 */
	public static boolean isCraftingEquivalent(ItemStack base, ItemStack comparison, boolean craftingTools) {
		if (base.isEmpty() || comparison.isEmpty()) {
			return false;
		}

		if (craftingTools && isCraftingToolEquivalent(base, comparison)) {
			return true;
		}

		return isCraftingEquivalent(base, comparison);
	}

	public static boolean isCraftingToolEquivalent(ItemStack base, ItemStack comparison) {
		if (base.isEmpty() || comparison.isEmpty()) {
			return false;
		}

		Item baseItem = base.getItem();

		if (baseItem != comparison.getItem()) {
			return false;
		}

		CustomData baseData = base.get(DataComponents.CUSTOM_DATA);
		return baseData == null || baseData.isEmpty();
	}

	public static int[] createConsume(List<Ingredient> set, Container inventory, boolean craftingTools) {
		return createConsume(set, inventory.getContainerSize(), inventory::getItem, craftingTools);
	}

	public static int[] createConsume(List<Ingredient> set, int stockCount, IntFunction<ItemStack> stock, boolean craftingTools) {
		//A array that contains the amount of items that is needed from this stack
		int[] reqAmounts = new int[stockCount];
		int found = 0;
		for (Ingredient ing : set) {
			if (ing.isEmpty()) {
				found++;
				continue;
			}
			for (int i = 0; i < reqAmounts.length; i++) {
				ItemStack offer = stock.apply(i);

				if (offer.getCount() > reqAmounts[i] && ing.test(offer)) {
					reqAmounts[i] = reqAmounts[i] + 1;
					found++;
					break;
				}
			}
		}
		if (found < set.size()) {
			return EMPTY_CONSUME;
		}

		return reqAmounts;
	}
}
