package forestry.core.platform.util;

import forestry.Forestry;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.platform.registration.FeatureItem;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class JeiUtil {
    public static Item item(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry(name));
    }

    public static void addDescription(IRecipeRegistration registry, String key, Item... items) {
        registry.addIngredientInfo(java.util.Arrays.stream(items).map(ItemStack::new).toList(), VanillaTypes.ITEM_STACK,
            Component.translatable(DESCRIPTION_KEY + key));
    }
	public static final String DESCRIPTION_KEY = "for.jei.description.";
	// From Ex Deorum: https://github.com/thedarkcolour/ExDeorum/blob/8ea02fd490dbfa106bdfe31af8b7a88f65b2abdc/src/main/java/thedarkcolour/exdeorum/compat/jei/ClientJeiUtil.java#L221
	static final DecimalFormat FORMATTER = new DecimalFormat();

	static {
		FORMATTER.setMinimumFractionDigits(0);
		FORMATTER.setMaximumFractionDigits(3);
	}

	public static void addDescription(IRecipeRegistration registry, String itemKey, FeatureItem<?>... items) {
		List<ItemStack> itemStacks = new ArrayList<>();
		for (FeatureItem<?> item : items) {
			itemStacks.add(item.stack());
		}

		registry.addIngredientInfo(itemStacks, VanillaTypes.ITEM_STACK, Component.translatable(DESCRIPTION_KEY + itemKey));
	}

	public static void addDescription(IRecipeRegistration registry, Block... blocks) {
		for (Block block : blocks) {
			Item item = block.asItem();
			if (item != Items.AIR) {
				addDescription(registry, item);
			} else {
				Forestry.LOGGER.error("No item for block {}", block);
			}
		}
	}

	public static void addDescription(IRecipeRegistration registry, FeatureItem<?>... items) {
		for (FeatureItem<?> item : items) {
			addDescription(registry, item.get());
		}
	}

	public static void addDescription(IRecipeRegistration registry, Item... items) {
		for (Item item : items) {
			addDescription(registry, item);
		}
	}

	public static void addDescription(IRecipeRegistration registry, Item item) {
		Identifier registryName = ModUtil.getRegistryName(item);
		String resourcePath = registryName.getPath();
		addDescription(registry, item, resourcePath);
	}

	public static void addDescription(IRecipeRegistration registry, Item item, String itemKey) {
		ItemStack itemStack = new ItemStack(item);
		registry.addIngredientInfo(itemStack, VanillaTypes.ITEM_STACK, Component.translatable(DESCRIPTION_KEY + itemKey));
	}

	public static List<IRecipeSlotBuilder> layoutSlotGrid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int width, int height, int xOffset, int yOffset, int slotSpacing) {
		List<IRecipeSlotBuilder> craftingSlots = new ArrayList<>();
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				IRecipeSlotBuilder slot = builder.addSlot(role, xOffset + x * slotSpacing, yOffset + y * slotSpacing);
				slot.setSlotName("forestry_crafting_" + (y * width + x));
				craftingSlots.add(slot);
			}
		}
		return craftingSlots;
	}

	public static void setCraftingItems(List<IRecipeSlotBuilder> craftingSlots, CraftingRecipe craftingGridRecipe, ICraftingGridHelper craftingGridHelper) {
		var context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(Minecraft.getInstance().level);
		for (var display : craftingGridRecipe.display()) {
			if (display instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped) {
				craftingGridHelper.setInputs(craftingSlots, VanillaTypes.ITEM_STACK,
					shaped.ingredients().stream().map(slot -> slot.resolveForStacks(context)).toList(), shaped.width(), shaped.height());
				return;
			} else if (display instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless) {
				craftingGridHelper.setInputs(craftingSlots, VanillaTypes.ITEM_STACK,
					shapeless.ingredients().stream().map(slot -> slot.resolveForStacks(context)).toList(), 0, 0);
				return;
			}
		}
		throw new IllegalArgumentException("Unsupported crafting display: " + craftingGridRecipe);
	}

	public static List<ItemStack> ingredientStacks(Ingredient ingredient) {
		return ingredient.display().resolveForStacks(net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(Minecraft.getInstance().level));
	}

	public static void setCraftingItems(List<IRecipeSlotBuilder> craftingSlots, List<Ingredient> ingredients, int width, int height, ICraftingGridHelper craftingGridHelper) {
		craftingGridHelper.setInputs(craftingSlots, VanillaTypes.ITEM_STACK,
			ingredients.stream().map(JeiUtil::ingredientStacks).toList(), width, height);
	}

	/** Only the ghost grid belongs in the transfer packet, not tanks, boxes or plans. */
	public static NonNullList<ItemStack> getCraftingItems(IRecipeSlotsView recipeSlots) {
		NonNullList<ItemStack> result = NonNullList.withSize(9, ItemStack.EMPTY);
		for (int i = 0; i < 9; i++) {
			String name = "forestry_crafting_" + i;
			var slot = recipeSlots.getSlotViews().stream().filter(view -> view.getSlotName().filter(name::equals).isPresent()).findFirst().orElseThrow();
			result.set(i, slot.getDisplayedItemStack().map(stack -> stack.copyWithCount(1)).orElse(ItemStack.EMPTY));
		}
		return result;
	}

	public static NonNullList<ItemStack> getFirstItemStacks(IRecipeSlotsView recipeSlots) {
		List<IRecipeSlotView> slotViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
		NonNullList<ItemStack> result = NonNullList.create();

		for (IRecipeSlotView slot : slotViews) {
			if (slot.isEmpty()) {
				result.add(ItemStack.EMPTY);
			} else {
				slot.getDisplayedIngredient()
					.filter(ingredient -> ingredient.getType() == VanillaTypes.ITEM_STACK)
					.ifPresent(ingredient -> result.add(((ITypedIngredient<ItemStack>) ingredient).getIngredient().copy()));
			}
		}
		return result;
	}

	public static <S extends ISpecies<?>> void registerItemSubtypes(ISubtypeRegistration registry, IChromosome<Identifier> species, ISpeciesType<S, ?> type) {
		ISubtypeInterpreter<ItemStack> interpreter = new ISubtypeInterpreter<>() {
			@Override
			public Object getSubtypeData(ItemStack stack, UidContext context) {
				var genome = IIndividualHandlerItem.getGenome(stack);
				return (genome != null && genome.getKaryotype() == type.getKaryotype()
					? genome.getActiveValue(species) : type.getKaryotype().getDefaultSpecies()).toString();
			}

		};

		for (ILifeStage stage : type.getLifeStages()) {
			registry.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, stage.getItemForm(), interpreter);
		}
	}

	// From JEI Bees
	public static void drawCenteredMulti(GuiGraphicsExtractor graphics, Component component, float x, float y, int color) {
		Font font = Minecraft.getInstance().font;
		String[] split = component.getString().split(" ");

		for (int i = 0; i < split.length; i++) {
			String line = split[i];
			drawCentered(graphics, font, line, x, y + i * font.lineHeight, color);
		}
	}

	private static void drawCentered(GuiGraphicsExtractor graphics, Font font, String line, float x, float y, int color) {
		int width = font.width(line);

		graphics.text(font, line, Math.round(x - width / 2f), Math.round(y), color | 0xff000000, false);
	}
	// End of JEI Bees

	// From Ex Deorum
	// Takes a decimal probability and returns a user-friendly percentage value
	public static Component formatChance(double probability) {
		return Component.translatable("for.jei.chance", formatPercentage(probability)).withStyle(ChatFormatting.GRAY);
	}

	public static String formatPercentage(double probability) {
		return FORMATTER.format(probability * 100);
	}
	// End of Ex Deorum
}
