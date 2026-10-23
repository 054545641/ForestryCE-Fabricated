package forestry.core.content.machines.recipes;

import forestry.api.core.machines.IForestryRecipe;
import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.ICarpenterRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import forestry.api.core.fluids.FluidVolume;
import forestry.api.core.fluids.FluidTemplate;

import javax.annotation.Nullable;
import java.util.Optional;

public class CarpenterRecipe implements ICarpenterRecipe {
	// Inner crafting recipe is either Shaped or Shapeless; dispatch via Either so
	// existing data (e.g. wood pulp, bronze ingot) using shapeless can persist.
	private static final Codec<CraftingRecipe> CRAFTING_RECIPE_CODEC = Codec.either(
		ShapedRecipe.SERIALIZER.codec().codec(),
		ShapelessRecipe.SERIALIZER.codec().codec()
	).xmap(
		either -> either.map(sr -> (CraftingRecipe) sr, slr -> (CraftingRecipe) slr),
		cr -> {
			if (cr instanceof ShapedRecipe sr) return Either.left(sr);
			if (cr instanceof ShapelessRecipe slr) return Either.right(slr);
			throw new IllegalStateException("Unsupported CraftingRecipe type for CarpenterRecipe: " + cr.getClass().getName());
		}
	);

	private static final MapCodec<CarpenterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(CarpenterRecipe::getId),
		Codec.INT.fieldOf("time").forGetter(CarpenterRecipe::getPackagingTime),
		net.minecraft.util.ExtraCodecs.optionalEmptyMap(FluidTemplate.CODEC).optionalFieldOf("liquid", Optional.empty()).forGetter(r -> r.liquid),
		Ingredient.CODEC.optionalFieldOf("box").forGetter(CarpenterRecipe::getBox),
		CRAFTING_RECIPE_CODEC.fieldOf("recipe").forGetter(CarpenterRecipe::getCraftingGridRecipe),
		ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(r -> r.result)
	).apply(instance, (id, time, liquid, box, recipe, optResult) -> new CarpenterRecipe(id, time, liquid, box, recipe, optResult)));
	private static final StreamCodec<RegistryFriendlyByteBuf, CarpenterRecipe> STREAM_CODEC = StreamCodec.of(
		NetworkCodecs::toNetwork,
		NetworkCodecs::fromNetwork
	);

	private final Identifier id;
	private final int packagingTime;
	private final Optional<FluidTemplate> liquid;
	private final Optional<Ingredient> box;
	private final CraftingRecipe recipe;
	private final Optional<ItemStackTemplate> result;

	public CarpenterRecipe(Identifier id, int packagingTime, FluidVolume liquid, Ingredient box, CraftingRecipe recipe, @Nullable ItemStack result) {
		this(id, packagingTime, liquid, Optional.of(box), recipe, result);
	}

	public CarpenterRecipe(Identifier id, int packagingTime, FluidVolume liquid, Optional<Ingredient> box, CraftingRecipe recipe, @Nullable ItemStack result) {
		this(id, packagingTime, liquid.isEmpty() ? Optional.empty() : Optional.of(FluidTemplate.from(liquid)),
			box, recipe, result == null || result.isEmpty() ? Optional.empty() : Optional.of(ItemStackTemplate.fromNonEmptyStack(result)));
	}

	public CarpenterRecipe(Identifier id, int packagingTime, Optional<FluidTemplate> liquid, Optional<Ingredient> box, CraftingRecipe recipe, Optional<ItemStackTemplate> result) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(liquid, "Recipe liquid cannot be null; use FluidVolume.EMPTY for no fluid input");
		Preconditions.checkNotNull(box);
		Preconditions.checkNotNull(recipe);
		if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) {
			throw new IllegalArgumentException("CarpenterRecipe inner recipe must be ShapedRecipe or ShapelessRecipe; got " + recipe.getClass().getName());
		}

		this.id = id;
		this.packagingTime = packagingTime;
		this.liquid = liquid;
		this.box = box;
		this.recipe = recipe;
		this.result = result;
	}

	@Override
	public int getPackagingTime() {
		return this.packagingTime;
	}

	@Override
	public Optional<Ingredient> getBox() {
		return this.box;
	}

	@Override
	public FluidVolume getInputFluid() {
		return this.liquid.map(FluidTemplate::create).orElse(FluidVolume.EMPTY);
	}

	@Override
	public CraftingRecipe getCraftingGridRecipe() {
		return this.recipe;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return this.result.map(ItemStackTemplate::create).orElseGet(() -> this.recipe.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY));
	}

	Optional<ItemStack> getOptionalResult() {
		return this.result.map(ItemStackTemplate::create);
	}

	@Override
	public boolean matches(FluidVolume fluid, ItemStack boxStack, Container craftingInventory, Level level) {
		FluidVolume liquid = getInputFluid();
		if (!liquid.isEmpty()) {
			if (fluid.isEmpty() || !(FluidVolume.isSameFluidSameComponents(fluid, liquid) && fluid.getAmount() >= liquid.getAmount())) {
				return false;
			}
		}

		Optional<Ingredient> box = this.box;
		if (box.isPresent() && !box.get().test(boxStack)) {
			return false;
		}

		return this.recipe.matches(FakeCraftingInventory.of(craftingInventory), level);
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<? extends IForestryRecipe> getSerializer() {
		return FactoryRecipeTypes.CARPENTER.serializer();
	}

	@Override
	public RecipeType<? extends IForestryRecipe> getType() {
		return FactoryRecipeTypes.CARPENTER.type();
	}

	public static final RecipeSerializer<CarpenterRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private static class NetworkCodecs {

		private static CarpenterRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			Identifier recipeId = Identifier.STREAM_CODEC.decode(buffer);
			int packagingTime = ByteBufCodecs.VAR_INT.decode(buffer);
			Optional<FluidTemplate> liquid = ByteBufCodecs.optional(FluidTemplate.STREAM_CODEC).decode(buffer);
			Optional<Ingredient> box = Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.decode(buffer);
			CraftingRecipe internal = buffer.readBoolean()
				? ShapedRecipe.SERIALIZER.streamCodec().decode(buffer)
				: ShapelessRecipe.SERIALIZER.streamCodec().decode(buffer);
			Optional<ItemStackTemplate> result = ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(buffer);

			return new CarpenterRecipe(recipeId, packagingTime, liquid, box, internal, result);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, CarpenterRecipe recipe) {
			Identifier.STREAM_CODEC.encode(buffer, recipe.id);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.packagingTime);
			ByteBufCodecs.optional(FluidTemplate.STREAM_CODEC).encode(buffer, recipe.liquid);
			Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC.encode(buffer, recipe.box);
			if (recipe.recipe instanceof ShapedRecipe shaped) {
				buffer.writeBoolean(true);
				ShapedRecipe.SERIALIZER.streamCodec().encode(buffer, shaped);
			} else if (recipe.recipe instanceof ShapelessRecipe shapeless) {
				buffer.writeBoolean(false);
				ShapelessRecipe.SERIALIZER.streamCodec().encode(buffer, shapeless);
			} else {
				throw new IllegalStateException("Unsupported CraftingRecipe type for CarpenterRecipe network sync: " + recipe.recipe.getClass().getName());
			}

			ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buffer, recipe.result);
		}
	}
}
