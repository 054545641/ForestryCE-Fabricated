package forestry.agriculture.compat;

import forestry.api.IForestryApi;
import forestry.api.core.circuits.CircuitHolder;
import forestry.api.agriculture.IFarmCircuit;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.engine.circuits.EnumCircuitBoardType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class FarmingJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return ForestryModuleIds.FARMING;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new FarmingInfoRecipeCategory(guiHelper));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(FarmingInfoRecipeCategory.TYPE, getRecipes());
	}

	public static List<FarmingInfoRecipe> getRecipes() {
		ArrayList<FarmingInfoRecipe> info = new ArrayList<>();

		for (CircuitHolder holder : IForestryApi.INSTANCE.getCircuitManager().getCircuitHolders()) {
			if (holder.circuit() instanceof IFarmCircuit circuit) {
				if (circuit.isManual()) {
					info.add(new FarmingInfoRecipe(holder.stack(), circuit.getProperties(), circuit));
				}
			}
		}

		return info;
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(forestry.core.platform.util.JeiUtil.item("intricate_circuit_board")), FarmingInfoRecipeCategory.TYPE);
	}
}
