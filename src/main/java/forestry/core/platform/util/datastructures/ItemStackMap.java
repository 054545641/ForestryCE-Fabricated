package forestry.core.platform.util.datastructures;

import forestry.core.platform.util.ItemStackUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.Serial;

// todo  remove
public class ItemStackMap<T> extends StackMap<ItemStack, T> {
	@Serial
	private static final long serialVersionUID = -8511966739130702305L;

	@Override
	protected boolean areEqual(ItemStack a, ItemStack b) {
		return ItemStackUtil.isCraftingEquivalent(b, a);
	}

	@Override
	protected boolean isValidKey(Object key) {
		return key instanceof ItemStack || key instanceof Item || key instanceof String || key instanceof Identifier;
	}

	@Override
	protected ItemStack getStack(Object key) {
		if (key instanceof ItemStack) {
			return (ItemStack) key;
		}
		return ItemStack.EMPTY;
	}

}
