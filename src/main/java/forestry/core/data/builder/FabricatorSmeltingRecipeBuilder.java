package forestry.core.data.builder;

import forestry.core.content.machines.recipes.FabricatorSmeltingRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

public class FabricatorSmeltingRecipeBuilder {

	private int meltingPoint;
	private Ingredient resource;
	private FluidStack product;

	public FabricatorSmeltingRecipeBuilder setMeltingPoint(int meltingPoint) {
		this.meltingPoint = meltingPoint;
		return this;
	}

	public FabricatorSmeltingRecipeBuilder setResource(Ingredient resource) {
		this.resource = resource;
		return this;
	}

	public FabricatorSmeltingRecipeBuilder setProduct(FluidStack product) {
		this.product = product;
		return this;
	}

	public void build(RecipeOutput output, Identifier id) {
		output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), new FabricatorSmeltingRecipe(id, this.resource, this.product, this.meltingPoint), null);
	}
}
