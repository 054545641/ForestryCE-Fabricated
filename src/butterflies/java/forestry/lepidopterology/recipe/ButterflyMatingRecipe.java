package forestry.lepidopterology.recipe;

import forestry.Forestry;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import forestry.api.lepidopterology.genetics.IButterfly;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.lepidopterology.features.LepidopterologyRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ButterflyMatingRecipe extends CustomRecipe {
	private final CraftingBookCategory category;
	public ButterflyMatingRecipe(CraftingBookCategory category) {
		this.category = category;
	}

	@Override
	public boolean matches(CraftingInput grid, Level level) {
		int containerSize = grid.size();
		boolean hasButterfly = false;
		boolean hasSerum = false;

		for (int i = 0; i < containerSize; ++i) {
			ItemStack stack = grid.getItem(i);

			if (!stack.isEmpty()) {
				ILifeStage stage = IIndividualHandlerItem.getLifeStage(stack);
				if (stage == null) {
					return false;
				} else {
					if (stage == ButterflyLifeStage.BUTTERFLY) {
						if (hasButterfly) {
							return false;
						} else {
							hasButterfly = true;
						}
					} else if (stage == ButterflyLifeStage.SERUM) {
						if (hasSerum) {
							return false;
						} else {
							hasSerum = true;
						}
					} else {
						return false;
					}
				}
			}
		}

		return hasButterfly && hasSerum;
	}

	@Override
	public ItemStack assemble(CraftingInput grid) {
		IButterfly butterfly = null;
		IIndividual serum = null;
		int containerSize = grid.size();

		for (int i = 0; i < containerSize; i++) {
			ItemStack stack = grid.getItem(i);
			IIndividual individual = IIndividualHandlerItem.getIndividual(stack);

			if (individual != null) {
				ILifeStage stage = IIndividualHandlerItem.getLifeStage(stack);

				if (stage == ButterflyLifeStage.BUTTERFLY) {
					butterfly = (IButterfly) individual;
				} else if (stage == ButterflyLifeStage.SERUM) {
					serum = individual;
				}
			}
		}

		if (butterfly != null && serum != null) {
			IButterfly copy = butterfly.copy();
			copy.setMate(serum.getGenome());
			return copy.createStack(ButterflyLifeStage.BUTTERFLY);
		}

		Forestry.LOGGER.warn("Failed to craft butterfly");
		return ItemStack.EMPTY;
	}

	@Override
	public CraftingBookCategory category() {
		return this.category;
	}

	@Override
	public RecipeSerializer<ButterflyMatingRecipe> getSerializer() {
		return LepidopterologyRecipes.MATING_SERIALIZER.get();
	}
}
