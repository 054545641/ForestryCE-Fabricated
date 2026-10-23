package forestry.arboriculture.wood;

import forestry.core.platform.item.ItemBlockForestry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

public class ForestryWoodBlockItem<B extends Block & IWoodTyped> extends ItemBlockForestry<B> {
	private final IWoodTyped wood;

	public ForestryWoodBlockItem(B block, Properties properties) {
		super(block, properties);

		// Safeguard against Diagonal Fence's registry replacements causing crashes
		this.wood = block;
	}

	public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType, net.minecraft.world.level.block.entity.FuelValues fuelValues) {
		if (this.wood.isFireproof()) {
			return 0;
		} else {
			return 300;
		}
	}
}
