package forestry.core.platform.util;

import forestry.api.core.IProduct;
import it.unimi.dsi.fastutil.Hash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

public abstract class ItemStackUtil {
	public static final Hash.Strategy<ItemStack> ITEM_STACK_STRATEGY = new Hash.Strategy<>() {
		@Override
		public int hashCode(ItemStack o) {
			if (o.isEmpty()) {
				return 0;
			}
			int result = 1;
			result = 31 * result + o.getCount();
			result = 31 * result + o.getItem().hashCode();
			if (!o.getComponents().isEmpty()) {
				result = 31 * result + o.getComponents().hashCode();
			}

			return result;
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			if (a == null || b == null) {
				return a == b;
			}
			return ItemStack.matches(a, b);
		}
	};

	private static final int[] EMPTY_CONSUME = new int[0];

	/**
	 * Compares item id, damage and NBT. Accepts wildcard damage.
	 */
	public static boolean isIdenticalItem(ItemStack lhs, ItemStack rhs) {
		return lhs.getItem() == rhs.getItem() && lhs.getComponents().equals(rhs.getComponents());
	}

	/**
	 * Merges the giving stack into the receiving stack as far as possible
	 */
	public static void mergeStacks(ItemStack giver, ItemStack receptor) {
		int maxInsert = receptor.getMaxStackSize() - receptor.getCount();
		int maxExtract = giver.getCount();
		int canTransfer = Math.min(maxInsert, maxExtract);

		giver.shrink(canTransfer);
		receptor.grow(canTransfer);
	}

	public static Map<ItemStack, Integer> condensedStackCounts(List<ItemStack> stacks) {
		Map<ItemStack, Integer> map = new HashMap<>();

		for (ItemStack stack : stacks) {
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack copy = stack.copy();
			copy.setCount(1);

			// TODO This is terrible stuff, please find another way to solve this :)
			// The hash map is actually useless as the hash of an ItemStack isn't always the same
			boolean found = false;
			for (Map.Entry<ItemStack, Integer> entry : map.entrySet()) {
				if (isIdenticalItem(entry.getKey(), copy)) {
					map.put(entry.getKey(), map.get(entry.getKey()) + stack.getCount());
					found = true;
					break;
				}
			}
			if (!found) {
				map.put(copy, stack.getCount());
			}
		}

		return map;
	}

	public static List<ItemStack> realizeStacks(Map<ItemStack, Integer> map) {
		ArrayList<ItemStack> condensed = new ArrayList<>(map.size());

		for (Map.Entry<ItemStack, Integer> entry : map.entrySet()) {
			ItemStack stack = entry.getKey();
			int count = entry.getValue();

			while (count > 0) {
				int transfer = Math.min(count, stack.getMaxStackSize());
				count -= transfer;

				ItemStack copy = stack.copy();
				copy.setCount(transfer);
				condensed.add(copy);
			}
		}

		return condensed;
	}

	public static List<ItemStack> condenseStacks(List<ItemStack> stacks) {
		return realizeStacks(condensedStackCounts(stacks));
	}

	public static boolean containsItemStack(Iterable<ItemStack> list, ItemStack itemStack) {
		for (ItemStack listStack : list) {
			if (isIdenticalItem(listStack, itemStack)) {
				return true;
			}
		}
		return false;
	}

	public static int[] createConsume(List<Ingredient> set, Container inventory, boolean craftingTools) { return InventoryCrafting.createConsume(set, inventory, craftingTools); }
	public static int[] createConsume(List<Ingredient> set, int count, IntFunction<ItemStack> stock, boolean craftingTools) { return InventoryCrafting.createConsume(set, count, stock, craftingTools); }

	/**
	 * Counts how many full sets are contained in the passed stock
	 */
	public static int containsSets(List<ItemStack> set, List<ItemStack> stock) {
		return containsSets(set, stock, false);
	}

	/**
	 * Counts how many full sets are contained in the passed stock
	 */
	public static int containsSets(List<ItemStack> set, List<ItemStack> stock, boolean craftingTools) {
		int totalSets = 0;

		Map<ItemStack, Integer> condensedRequired = ItemStackUtil.condensedStackCounts(set);
		Map<ItemStack, Integer> condensedOffered = ItemStackUtil.condensedStackCounts(stock);

		for (Map.Entry<ItemStack, Integer> req : condensedRequired.entrySet()) {
			if (req.getValue() <= 0) {
				continue;
			}
			int offerCount = 0;

			for (Map.Entry<ItemStack, Integer> offer : condensedOffered.entrySet()) {
				if (isCraftingEquivalent(req.getKey(), offer.getKey(), craftingTools)) {
					offerCount += offer.getValue();
				}
			}

			int reqCount = offerCount / req.getValue();

			if (reqCount == 0) {
				return 0;
			} else if (totalSets == 0) {
				totalSets = reqCount;
			} else if (totalSets > reqCount) {
				totalSets = reqCount;
			}
		}

		return totalSets;
	}

	public static boolean equalSets(NonNullList<ItemStack> set1, NonNullList<ItemStack> set2) {
		if (set1 == set2) {
			return true;
		}

		int count = set1.size();

		if (count != set2.size()) {
			return false;
		}

		for (int i = 0; i < count; i++) {
			if (!isIdenticalItem(set1.get(i), set2.get(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Compare two item stacks for crafting equivalency without oreDictionary or craftingTools
	 */
	public static boolean isCraftingEquivalent(ItemStack base, ItemStack comparison) { return InventoryCrafting.isCraftingEquivalent(base, comparison); }
	public static boolean isCraftingEquivalent(ItemStack base, ItemStack comparison, boolean craftingTools) { return InventoryCrafting.isCraftingEquivalent(base, comparison, craftingTools); }
	public static boolean isCraftingToolEquivalent(ItemStack base, ItemStack comparison) { return InventoryCrafting.isCraftingToolEquivalent(base, comparison); }

	public static void dropItemStackAsEntity(ItemStack items, Level world, double x, double y, double z) {
		dropItemStackAsEntity(items, world, x, y, z, 10);
	}

	public static void dropItemStackAsEntity(ItemStack items, Level world, BlockPos pos) {
		dropItemStackAsEntity(items, world, pos.getX(), pos.getY(), pos.getZ(), 10);
	}

	public static void dropItemStackAsEntity(ItemStack items, Level world, double x, double y, double z, int delayForPickup) {
		if (items.isEmpty() || world.isClientSide()) {
			return;
		}

		float f1 = 0.4F;
		double d = (world.getRandom().nextFloat() * f1 + (1.0F - f1)) * 0.5D;
		double d1 = (world.getRandom().nextFloat() * f1 + (1.0F - f1)) * 0.5D;
		double d2 = (world.getRandom().nextFloat() * f1 + (1.0F - f1)) * 0.5D;
		ItemEntity entityitem = new ItemEntity(world, x + d, y + d1, z + d2, items);
		entityitem.setPickUpDelay(delayForPickup);

		world.addFreshEntity(entityitem);
	}

	public static ItemStack copyWithRandomSize(IProduct template, int max, RandomSource rand) {
		int size = max <= 0 ? 1 : rand.nextInt(max);
		ItemStack copy = template.createRandomStack(rand);
		copy.setCount(Math.min(size, copy.getMaxStackSize()));
		return copy;
	}

	/**
	 * Checks if two items are exactly the same, ignoring counts
	 */
	public static boolean areItemStacksEqualIgnoreCount(ItemStack a, ItemStack b) {
		int countB = b.getCount();
		b.setCount(a.getCount());
		boolean equals = ItemStack.matches(a, b);
		b.setCount(countB);
		return equals;
	}
}
