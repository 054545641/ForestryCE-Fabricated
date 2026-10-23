package forestry.apiculture.compat;


import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.core.IProductProducer;
import forestry.api.core.ISpecialtyProducer;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.apiary.CreativeHiveFrameItem;
import forestry.core.platform.util.JeiUtil;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.platform.data.GeneticDataLoader;
import forestry.core.platform.data.LogicalData;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class ApicultureJeiPlugin implements IModPlugin {
	static final Identifier BACKGROUND = ForestryConstants.forestry("textures/gui/jei/recipes.png");
	static final List<ProductsRecipeCategory> productsCategories = new ArrayList<>();
	static final List<MutationsRecipeCategory> mutationsCategories = new ArrayList<>();
	private static IJeiRuntime runtime;

	static {
		GeneticDataLoader.afterApply(ApicultureJeiPlugin::refreshGeneticIngredients);
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime available) {
		runtime = available;
		refreshGeneticIngredients();
	}

	@Override
	public void onRuntimeUnavailable() {
		runtime = null;
	}

	private static void refreshGeneticIngredients() {
		if (runtime == null || LogicalData.isServer() || SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 0) return;
		var types = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes().stream()
			.filter(type -> type.id().equals(ForestrySpeciesTypes.BEE) || type.id().equals(ForestrySpeciesTypes.TREE)
				|| type.id().equals(ForestrySpeciesTypes.BUTTERFLY)).toList();
		var items = types.stream().flatMap(type -> type.getLifeStages().stream()).map(stage -> stage.getItemForm()).collect(java.util.stream.Collectors.toSet());
		var ingredients = runtime.getIngredientManager();
		var previous = ingredients.getAllItemStacks().stream().filter(stack -> items.contains(stack.getItem())).toList();
		var current = new ArrayList<ItemStack>();
		for (var type : types) {
			for (var stage : type.getLifeStages()) {
				for (var species : type.getAllSpecies()) current.add(species.createStack(stage));
			}
		}
		ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, previous);
		ingredients.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, current);
	}

	@Override
	public Identifier getPluginUid() {
		return ForestryModuleIds.APICULTURE;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		productsCategories.clear();
		mutationsCategories.clear();
		IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
		IDrawable productsBackground = helper.createDrawable(BACKGROUND, 0, 61 + 30, 162, 61);
		IDrawable mutationsBackground = helper.createDrawable(BACKGROUND, 0, 30, 162, 61);

		for (ISpeciesType<?, ?> type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			IDrawable icon = helper.createDrawableItemStack(type.createDefaultStack());

			// products
			if (type.getDefaultSpecies() instanceof IProductProducer) {
				ProductsRecipeCategory category = new ProductsRecipeCategory(type, productsBackground, icon);
				productsCategories.add(category);
				registration.addRecipeCategories(category);
			}
			// mutations
			MutationsRecipeCategory category = new MutationsRecipeCategory(type, mutationsBackground, icon);
			mutationsCategories.add(category);
			registration.addRecipeCategories(category);
		}
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registry) {
		JeiUtil.registerItemSubtypes(registry, BeeChromosomes.SPECIES, SpeciesUtil.BEE_TYPE.get());
		// show both creative frames in JEI
		registry.registerSubtypeInterpreter(JeiUtil.item("creative_frame"), new ISubtypeInterpreter<>() {
			@Override
			public Object getSubtypeData(ItemStack stack, UidContext context) {
				return CreativeHiveFrameItem.hasForceMutations(stack);
			}

		});
	}

	@Override
	public void registerRecipes(IRecipeRegistration registry) {
		JeiUtil.addDescription(registry, "frames",
			JeiUtil.item("impregnated_frame"),
			JeiUtil.item("proven_frame"),
			JeiUtil.item("untreated_frame")
		);

		JeiUtil.addDescription(registry, "apiarist.suit",
			JeiUtil.item("apiarists_shoes"),
			JeiUtil.item("apiarists_shirt"),
			JeiUtil.item("apiarists_hat"),
			JeiUtil.item("apiarists_pants")
		);

		JeiUtil.addDescription(registry, JeiUtil.item("scoop"));
		JeiUtil.addDescription(registry, JeiUtil.item("proven_scoop"));
		JeiUtil.addDescription(registry, JeiUtil.item("experience_drop"));

		for (MutationsRecipeCategory category : mutationsCategories) {
			registry.addRecipes(category.getRecipeType(), category.speciesType.getMutations().getAllMutations().stream().map(MutationDisplay::new).toList());
		}
		for (ProductsRecipeCategory category : productsCategories) {
			registry.addRecipes(category.getRecipeType(), category.speciesType.getAllSpecies().stream()
				.filter(species -> {
					// filter out species with no products or specialties
					return (species instanceof IProductProducer producer && !producer.getProducts().isEmpty())
						|| (species instanceof ISpecialtyProducer specialtyProducer && !specialtyProducer.getSpecialties().isEmpty());
				})
				.map(species -> new ProductRecipe(species.cast()))
				.toList());
		}
		mutationsCategories.clear();
		productsCategories.clear();
	}
}
