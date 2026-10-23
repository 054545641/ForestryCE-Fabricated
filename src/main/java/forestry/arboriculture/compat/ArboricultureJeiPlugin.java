package forestry.arboriculture.compat;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ICharcoalPileWall;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.charcoal.jei.CharcoalPileWallCategory;
import forestry.arboriculture.features.CharcoalBlocks;
import forestry.core.platform.util.JeiUtil;
import forestry.core.platform.util.SpeciesUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class ArboricultureJeiPlugin implements IModPlugin {
	public static final IRecipeType<ICharcoalPileWall> CHARCOAL_PILE_TYPE = IRecipeType.create(ForestryConstants.MOD_ID, "charcoal.pile", ICharcoalPileWall.class);

	@Override
	public Identifier getPluginUid() {
		return ForestryModuleIds.ARBORICULTURE;
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registry) {
		JeiUtil.registerItemSubtypes(registry, TreeChromosomes.SPECIES, SpeciesUtil.TREE_TYPE.get());
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry) {
		IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
		registry.addRecipeCategories(new CharcoalPileWallCategory(guiHelper));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		JeiUtil.addDescription(registration, JeiUtil.item("grafter"), JeiUtil.item("proven_grafter"));

		ICharcoalManager charcoalManager = IForestryApi.INSTANCE.getTreeManager().getCharcoalManager();
		registration.addRecipes(CHARCOAL_PILE_TYPE, charcoalManager.getWalls());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(CharcoalBlocks.LOG_PILE.stack(), CHARCOAL_PILE_TYPE);
	}
}
