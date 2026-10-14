package forestry.core.platform.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Predicate;

/**
 * This interface is used with several of the functions in IItemTransfer to
 * provide a convenient means of dealing with entire classes of items without
 * having to specify each item individually.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public enum StandardStackFilters implements Predicate<ItemStack> {

	ALL {
		@Override
		public boolean test(ItemStack stack) {
			return true;
		}
	},
	FUEL {
		@Override
		public boolean test(ItemStack stack) {
			return stack.typeHolder().getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.FURNACE_FUELS) != null;
		}
	},
	//TODO - where is this used?
	FEED {
		@Override
		public boolean test(ItemStack stack) {
			//TODO - tag on seeds?
			return stack.has(DataComponents.FOOD) || stack.getItem() == Items.WHEAT || stack.getItem() == Items.WHEAT_SEEDS;
		}
	}
}
