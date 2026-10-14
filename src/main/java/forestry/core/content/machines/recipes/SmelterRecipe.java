package forestry.core.content.machines.recipes;

import forestry.api.core.machines.IForestryRecipe;
import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.ISmelterRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

public class SmelterRecipe implements ISmelterRecipe {
	// Deviation from 1.20.1: forestry's own IngredientStack is replaced by NeoForge's SizedIngredient,
	// which carries the same ingredient plus count and ships the codecs 1.21.1 recipe serialization needs.
	// The JSON shape is unchanged: an "ingredient" object next to a "count"
	private static final MapCodec<SmelterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(SmelterRecipe::getId),
		SizedIngredient.NESTED_CODEC.listOf().fieldOf("inputs").forGetter(SmelterRecipe::getInputs),
		SizedIngredient.NESTED_CODEC.fieldOf("output").forGetter(recipe -> recipe.output),
		Codec.INT.fieldOf("processingTime").forGetter(SmelterRecipe::getProcessingTime)
	).apply(instance, SmelterRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, SmelterRecipe> STREAM_CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, SmelterRecipe::getId,
		SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), SmelterRecipe::getInputs,
		SizedIngredient.STREAM_CODEC, recipe -> recipe.output,
		ByteBufCodecs.VAR_INT, SmelterRecipe::getProcessingTime,
		SmelterRecipe::new
	);

	private final Identifier id;
	private final List<SizedIngredient> inputs;
	private final SizedIngredient output;
	private final int processingTime;

	public SmelterRecipe(Identifier id, List<SizedIngredient> inputs, SizedIngredient output, int processingTime) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(inputs);
		Preconditions.checkArgument(!inputs.isEmpty());
		Preconditions.checkNotNull(output);

		this.id = id;
		this.inputs = inputs;
		this.output = output;
		this.processingTime = processingTime;
	}

	@Override
	public List<SizedIngredient> getInputs() {
		return this.inputs;
	}

	@Override
	public ItemStack getOutput() {
		// The output is an ingredient so a recipe can name an alloy by tag and let whichever mod
		// fills that tag supply the item. A tag no loaded mod fills resolves to nothing
		return this.output.ingredient().items().findFirst()
			.map(item -> new ItemStack(item, this.output.count())).orElse(ItemStack.EMPTY);
	}

	@Override
	public int getProcessingTime() {
		return this.processingTime;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return getOutput();
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<? extends IForestryRecipe> getSerializer() {
		return FactoryRecipeTypes.SMELTER.serializer();
	}

	@Override
	public RecipeType<? extends IForestryRecipe> getType() {
		return FactoryRecipeTypes.SMELTER.type();
	}

	/**
	 * Used to see if a smelter holds enough resources to alloy a certain recipe.
	 *
	 * @param recipe   The smelter recipe to check against
	 * @param contents The contents of the given smelter
	 * @return Whether the contents of the smelter are sufficient to alloy the given recipe
	 */
	public static boolean canAlloy(ISmelterRecipe recipe, List<ItemStack> contents) {
		for (SizedIngredient input : recipe.getInputs()) {
			int found = 0;

			for (ItemStack stack : contents) {
				// Only the ingredient, never SizedIngredient#test, which also demands that this one
				// stack hold the whole count. An input may be spread over several slots
				if (input.ingredient().test(stack)) {
					found += stack.getCount();
					if (found >= input.count()) {
						break;
					}
				}
			}

			if (found < input.count()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return this.id + "= \n[ " + this.inputs + " ]: " + this.output;
	}

	public static final RecipeSerializer<SmelterRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private static class NetworkCodecs {
	}
}
