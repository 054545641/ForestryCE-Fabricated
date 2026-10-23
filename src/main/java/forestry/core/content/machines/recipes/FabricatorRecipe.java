package forestry.core.content.machines.recipes;

import java.util.Optional;
import forestry.api.core.machines.IForestryRecipe;
import com.google.common.base.Preconditions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.IFabricatorRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import forestry.api.core.fluids.FluidVolume;
import forestry.api.core.fluids.FluidTemplate;

public class FabricatorRecipe implements IFabricatorRecipe {

	private static final MapCodec<FabricatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(FabricatorRecipe::getId),
		Ingredient.CODEC.optionalFieldOf("plan").forGetter(FabricatorRecipe::getPlan),
		FluidTemplate.CODEC.fieldOf("molten").forGetter(r -> r.resultFluid),
		ShapedRecipe.SERIALIZER.codec().codec().fieldOf("recipe").forGetter(FabricatorRecipe::getCraftingGridRecipe)
	).apply(instance, (id, plan, molten, recipe) -> new FabricatorRecipe(id, plan, molten, recipe)));
	private static final StreamCodec<RegistryFriendlyByteBuf, FabricatorRecipe> STREAM_CODEC = StreamCodec.of(
		NetworkCodecs::toNetwork,
		NetworkCodecs::fromNetwork
	);

	private final Identifier id;
	private final Optional<Ingredient> plan;
	private final FluidTemplate resultFluid;
	private final ShapedRecipe recipe;

	public FabricatorRecipe(Identifier id, Ingredient plan, FluidVolume resultFluid, ShapedRecipe recipe) {
		this(id, plan, FluidTemplate.from(resultFluid), recipe);
	}

	public FabricatorRecipe(Identifier id, Ingredient plan, FluidTemplate resultFluid, ShapedRecipe recipe) {
		this(id, Optional.of(plan), resultFluid, recipe);
	}

	public FabricatorRecipe(Identifier id, Optional<Ingredient> plan, FluidVolume resultFluid, ShapedRecipe recipe) {
		this(id, plan, FluidTemplate.from(resultFluid), recipe);
	}

	public FabricatorRecipe(Identifier id, Optional<Ingredient> plan, FluidTemplate resultFluid, ShapedRecipe recipe) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(plan);
		Preconditions.checkNotNull(resultFluid);

		this.id = id;
		this.plan = plan;
		this.resultFluid = resultFluid;
		this.recipe = recipe;
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	@Override
	public Optional<Ingredient> getPlan() {
		return this.plan;
	}

	@Override
	public FluidVolume getResultFluid() {
		return this.resultFluid.create();
	}

	@Override
	public ShapedRecipe getCraftingGridRecipe() {
		return this.recipe;
	}

	@Override
	public boolean matches(Level level, FluidVolume liquid, ItemStack stack, Container inventory) {
		return FluidVolume.isSameFluidSameComponents(liquid, getResultFluid()) && liquid.getAmount() >= this.resultFluid.amount() && Ingredient.testOptionalIngredient(this.plan, stack) && this.recipe.matches(FakeCraftingInventory.of(inventory), level);
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return this.recipe.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);
	}

	@Override
	public RecipeSerializer<? extends IForestryRecipe> getSerializer() {
		return FactoryRecipeTypes.FABRICATOR.serializer();
	}

	@Override
	public RecipeType<? extends IForestryRecipe> getType() {
		return FactoryRecipeTypes.FABRICATOR.type();
	}

	public static final RecipeSerializer<FabricatorRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private static class NetworkCodecs {

		private static FabricatorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			Identifier id = Identifier.STREAM_CODEC.decode(buffer);
			Optional<Ingredient> plan = Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buffer);
			FluidTemplate molten = FluidTemplate.STREAM_CODEC.decode(buffer);
			ShapedRecipe shapedRecipe = ShapedRecipe.SERIALIZER.streamCodec().decode(buffer);

			return new FabricatorRecipe(id, plan, molten, shapedRecipe);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, FabricatorRecipe recipe) {
			Identifier.STREAM_CODEC.encode(buffer, recipe.id);
			Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buffer, recipe.plan);
			FluidTemplate.STREAM_CODEC.encode(buffer, recipe.resultFluid);
			ShapedRecipe.SERIALIZER.streamCodec().encode(buffer, recipe.recipe);
		}
	}
}
