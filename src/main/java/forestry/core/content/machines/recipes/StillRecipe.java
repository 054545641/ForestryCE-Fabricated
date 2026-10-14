package forestry.core.content.machines.recipes;

import forestry.api.core.machines.IForestryRecipe;
import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.IStillRecipe;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

public class StillRecipe implements IStillRecipe {
	private static final MapCodec<StillRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(StillRecipe::getId),
		Codec.INT.fieldOf("time").forGetter(StillRecipe::getCyclesPerUnit),
		FluidStackTemplate.CODEC.fieldOf("input").forGetter(r -> r.input),
		FluidStackTemplate.CODEC.fieldOf("output").forGetter(r -> r.output)
	).apply(instance, StillRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, StillRecipe> STREAM_CODEC = StreamCodec.of(
		NetworkCodecs::toNetwork,
		NetworkCodecs::fromNetwork
	);

	private final Identifier id;
	private final int timePerUnit;
	private final FluidStackTemplate input;
	private final FluidStackTemplate output;

	public StillRecipe(Identifier id, int timePerUnit, FluidStack input, FluidStack output) {
		this(id, timePerUnit, FluidStackTemplate.fromNonEmptyStack(input), FluidStackTemplate.fromNonEmptyStack(output));
	}

	public StillRecipe(Identifier id, int timePerUnit, FluidStackTemplate input, FluidStackTemplate output) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(input, "Still recipes need an input. Input was null.");
		Preconditions.checkNotNull(output, "Still recipes need an output. Output was null.");

		this.id = id;
		this.timePerUnit = timePerUnit;
		this.input = input;
		this.output = output;
	}

	@Override
	public int getCyclesPerUnit() {
		return this.timePerUnit;
	}

	@Override
	public FluidStack getInput() {
		return this.input.create();
	}

	@Override
	public FluidStack getOutput() {
		return this.output.create();
	}

	@Override
	public boolean matches(FluidStack input) {
		return FluidStack.isSameFluidSameComponents(input, getInput()) && input.getAmount() >= this.input.amount();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider lookupProvider) {
		return ItemStack.EMPTY;
	}

	@Override
	public Identifier getId() {
		return this.id;
	}

	@Override
	public RecipeSerializer<? extends IForestryRecipe> getSerializer() {
		return FactoryRecipeTypes.STILL.serializer();
	}

	@Override
	public RecipeType<? extends IForestryRecipe> getType() {
		return FactoryRecipeTypes.STILL.type();
	}

	public static final RecipeSerializer<StillRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private static class NetworkCodecs {

		private static StillRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			Identifier id = Identifier.STREAM_CODEC.decode(buffer);
			int timePerUnit = ByteBufCodecs.VAR_INT.decode(buffer);
			FluidStackTemplate input = FluidStackTemplate.STREAM_CODEC.decode(buffer);
			FluidStackTemplate output = FluidStackTemplate.STREAM_CODEC.decode(buffer);

			return new StillRecipe(id, timePerUnit, input, output);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, StillRecipe recipe) {
			Identifier.STREAM_CODEC.encode(buffer, recipe.id);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.timePerUnit);
			FluidStackTemplate.STREAM_CODEC.encode(buffer, recipe.input);
			FluidStackTemplate.STREAM_CODEC.encode(buffer, recipe.output);
		}
	}
}
