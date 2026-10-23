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
import forestry.api.core.fluids.FluidVolume;
import forestry.api.core.fluids.FluidTemplate;

public class StillRecipe implements IStillRecipe {
	private static final MapCodec<StillRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(StillRecipe::getId),
		Codec.INT.fieldOf("time").forGetter(StillRecipe::getCyclesPerUnit),
		FluidTemplate.CODEC.fieldOf("input").forGetter(r -> r.input),
		FluidTemplate.CODEC.fieldOf("output").forGetter(r -> r.output)
	).apply(instance, StillRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, StillRecipe> STREAM_CODEC = StreamCodec.of(
		NetworkCodecs::toNetwork,
		NetworkCodecs::fromNetwork
	);

	private final Identifier id;
	private final int timePerUnit;
	private final FluidTemplate input;
	private final FluidTemplate output;

	public StillRecipe(Identifier id, int timePerUnit, FluidVolume input, FluidVolume output) {
		this(id, timePerUnit, FluidTemplate.from(input), FluidTemplate.from(output));
	}

	public StillRecipe(Identifier id, int timePerUnit, FluidTemplate input, FluidTemplate output) {
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
	public FluidVolume getInput() {
		return this.input.create();
	}

	@Override
	public FluidVolume getOutput() {
		return this.output.create();
	}

	@Override
	public boolean matches(FluidVolume input) {
		return FluidVolume.isSameFluidSameComponents(input, getInput()) && input.getAmount() >= this.input.amount();
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
			FluidTemplate input = FluidTemplate.STREAM_CODEC.decode(buffer);
			FluidTemplate output = FluidTemplate.STREAM_CODEC.decode(buffer);

			return new StillRecipe(id, timePerUnit, input, output);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, StillRecipe recipe) {
			Identifier.STREAM_CODEC.encode(buffer, recipe.id);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.timePerUnit);
			FluidTemplate.STREAM_CODEC.encode(buffer, recipe.input);
			FluidTemplate.STREAM_CODEC.encode(buffer, recipe.output);
		}
	}
}
