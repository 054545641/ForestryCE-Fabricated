package forestry.core.data.builder;

import forestry.core.content.machines.recipes.MoistenerRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class MoistenerRecipeBuilder {

	private int timePerItem;
	private Ingredient resource;
	private ItemStack product;

	public MoistenerRecipeBuilder setTimePerItem(int timePerItem) {
		this.timePerItem = timePerItem;
		return this;
	}

	public MoistenerRecipeBuilder setResource(Ingredient resource) {
		this.resource = resource;
		return this;
	}

	public MoistenerRecipeBuilder setProduct(ItemStack product) {
		this.product = product;
		return this;
	}

	public void build(RecipeOutput output, Identifier id) {
		output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), new MoistenerRecipe(id, this.resource, this.product, this.timePerItem), null);
	}
}
