package forestry.core.content.machines.recipes;

import forestry.api.core.machines.IForestryRecipe;
import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.FluidProductType;
import forestry.api.core.IFluidProduct;
import forestry.api.core.machines.ISqueezerContainerRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import forestry.api.core.fluids.FluidVolume;

import java.util.List;

public class SqueezerContainerRecipe implements ISqueezerContainerRecipe {
	// Container recipes have no fluid output at all; this is never serialized (there is no "output" codec field
	// below), so createFluidVolume() being EMPTY is enough to make TileSqueezer refuse a fluid-fill for these recipes.
	private static final IFluidProduct NO_FLUID_OUTPUT = new IFluidProduct() {
		@Override
		public FluidVolume createFluidVolume() {
			return FluidVolume.EMPTY;
		}

		@Override
		public FluidProductType<?> type() {
			throw new UnsupportedOperationException("SqueezerContainerRecipe's fluid output is never serialized");
		}
	};

	private static final MapCodec<SqueezerContainerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(SqueezerContainerRecipe::getId),
		ItemStackTemplate.CODEC.fieldOf("container").forGetter(r -> r.emptyContainer),
		Codec.INT.fieldOf("time").forGetter(SqueezerContainerRecipe::getProcessingTime),
		ItemStackTemplate.CODEC.fieldOf("remnants").forGetter(r -> r.remnants),
		Codec.FLOAT.fieldOf("remnantsChance").forGetter(r -> r.remnantsChance)
	).apply(instance, SqueezerContainerRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, SqueezerContainerRecipe> STREAM_CODEC = StreamCodec.of(
		NetworkCodecs::toNetwork,
		NetworkCodecs::fromNetwork
	);

	private final Identifier id;
	private final ItemStackTemplate emptyContainer;
	private final int processingTime;
	private final ItemStackTemplate remnants;
	private final float remnantsChance;

	public SqueezerContainerRecipe(Identifier id, ItemStack emptyContainer, int processingTime, ItemStack remnants, float remnantsChance) {
		this(id, ItemStackTemplate.fromNonEmptyStack(emptyContainer), processingTime, ItemStackTemplate.fromNonEmptyStack(remnants), remnantsChance);
	}

	public SqueezerContainerRecipe(Identifier id, ItemStackTemplate emptyContainer, int processingTime, ItemStackTemplate remnants, float remnantsChance) {
		this.id = id;
		Preconditions.checkNotNull(emptyContainer);
		Preconditions.checkNotNull(remnants);

		this.emptyContainer = emptyContainer;
		this.processingTime = processingTime;
		this.remnants = remnants;
		this.remnantsChance = remnantsChance;
	}

	@Override
	public ItemStack getEmptyContainer() {
		return this.emptyContainer.create();
	}

	@Override
	public List<Ingredient> getInputs() {
		return List.of();
	}

	@Override
	public int getProcessingTime() {
		return this.processingTime;
	}

	@Override
	public ItemStack getRemnants() {
		return this.remnants.create();
	}

	@Override
	public float getRemnantsChance() {
		return this.remnantsChance;
	}

	@Override
	public IFluidProduct getFluidOutput() {
		return NO_FLUID_OUTPUT;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return this.remnants.create();
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<? extends IForestryRecipe> getSerializer() {
		return FactoryRecipeTypes.SQUEEZER_CONTAINER.serializer();
	}

	@Override
	public RecipeType<? extends IForestryRecipe> getType() {
		return FactoryRecipeTypes.SQUEEZER_CONTAINER.type();
	}

	public static final RecipeSerializer<SqueezerContainerRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private static class NetworkCodecs {

		private static SqueezerContainerRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			Identifier recipeId = Identifier.STREAM_CODEC.decode(buffer);
			ItemStackTemplate emptyContainer = ItemStackTemplate.STREAM_CODEC.decode(buffer);
			int processingTime = ByteBufCodecs.VAR_INT.decode(buffer);
			ItemStackTemplate remnants = ItemStackTemplate.STREAM_CODEC.decode(buffer);
			float remnantsChance = ByteBufCodecs.FLOAT.decode(buffer);

			return new SqueezerContainerRecipe(recipeId, emptyContainer, processingTime, remnants, remnantsChance);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, SqueezerContainerRecipe recipe) {
			Identifier.STREAM_CODEC.encode(buffer, recipe.id);
			ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.emptyContainer);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.processingTime);
			ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.remnants);
			ByteBufCodecs.FLOAT.encode(buffer, recipe.remnantsChance);
		}
	}
}
