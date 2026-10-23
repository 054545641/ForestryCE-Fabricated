package forestry.arboriculture.wood;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

public class ForestrySlabBlockItem extends BlockItem {
	public ForestrySlabBlockItem(ForestrySlabBlock block, Properties properties) {
		super(block, properties);
	}

	public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType, net.minecraft.world.level.block.entity.FuelValues fuelValues) {
		ForestrySlabBlock forestrySlab = (ForestrySlabBlock) getBlock();

		if (forestrySlab.isFireproof()) {
			return 0;
		} else {
			return 150;
		}
	}
}
