package forestry.core.data.builder;

import forestry.core.content.machines.recipes.SqueezerContainerRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class SqueezerContainerRecipeBuilder {
	private ItemStack emptyContainer;
	private int processingTime;
	private ItemStack remnants;
	private float remnantsChance;

	public SqueezerContainerRecipeBuilder setEmptyContainer(ItemStack emptyContainer) {
		this.emptyContainer = emptyContainer;
		return this;
	}

	public SqueezerContainerRecipeBuilder setProcessingTime(int processingTime) {
		this.processingTime = processingTime;
		return this;
	}

	public SqueezerContainerRecipeBuilder setRemnants(ItemStack remnants) {
		this.remnants = remnants;
		return this;
	}

	public SqueezerContainerRecipeBuilder setRemnantsChance(float remnantsChance) {
		this.remnantsChance = remnantsChance;
		return this;
	}

	public void build(RecipeOutput output, Identifier id) {
		output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), new SqueezerContainerRecipe(id, this.emptyContainer, this.processingTime, this.remnants, this.remnantsChance), null);
	}
}
