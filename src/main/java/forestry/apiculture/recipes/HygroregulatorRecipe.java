package forestry.apiculture.recipes;

import forestry.api.core.machines.IForestryRecipe;
import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.machines.IHygroregulatorRecipe;
import forestry.apiculture.features.ApicultureRecipeTypes;
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

// recipes used by Alveary Hygroregulator
public class HygroregulatorRecipe implements IHygroregulatorRecipe {
	private static final MapCodec<HygroregulatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(HygroregulatorRecipe::getId),
		FluidStackTemplate.CODEC.fieldOf("liquid").forGetter(r -> r.liquid),
		Codec.INT.fieldOf("time").forGetter(HygroregulatorRecipe::getRetainTime),
		Codec.BYTE.fieldOf("humidity_steps").forGetter(HygroregulatorRecipe::getHumiditySteps),
		Codec.BYTE.fieldOf("temperature_steps").forGetter(HygroregulatorRecipe::getTemperatureSteps)
	).apply(instance, HygroregulatorRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, HygroregulatorRecipe> STREAM_CODEC = StreamCodec.of(
		NetworkCodecs::toNetwork,
		NetworkCodecs::fromNetwork
	);

	private final Identifier id;
	private final FluidStackTemplate liquid;
	private final byte humiditySteps;
	private final byte temperatureSteps;
	private final int retainTime;

	public HygroregulatorRecipe(Identifier id, FluidStack liquid, int retainTime, byte humiditySteps, byte temperatureSteps) {
		this(id, FluidStackTemplate.fromNonEmptyStack(liquid), retainTime, humiditySteps, temperatureSteps);
	}

	public HygroregulatorRecipe(Identifier id, FluidStackTemplate liquid, int retainTime, byte humiditySteps, byte temperatureSteps) {
		Preconditions.checkNotNull(id, "Recipe identifier cannot be null");
		Preconditions.checkNotNull(liquid);
		this.id = id;
		this.liquid = liquid;
		this.retainTime = retainTime;
		this.humiditySteps = humiditySteps;
		this.temperatureSteps = temperatureSteps;
	}

	@Override
	public FluidStack getInputFluid() {
		return this.liquid.create();
	}

	@Override
	public int getRetainTime() {
		return this.retainTime;
	}

	@Override
	public byte getHumiditySteps() {
		return this.humiditySteps;
	}

	@Override
	public byte getTemperatureSteps() {
		return this.temperatureSteps;
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
		return ApicultureRecipeTypes.HYGROREGULATOR.serializer();
	}

	@Override
	public RecipeType<? extends IForestryRecipe> getType() {
		return ApicultureRecipeTypes.HYGROREGULATOR.type();
	}

	public static final RecipeSerializer<HygroregulatorRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	private static class NetworkCodecs {

		private static HygroregulatorRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
			Identifier recipeId = Identifier.STREAM_CODEC.decode(buffer);
			FluidStackTemplate liquid = FluidStackTemplate.STREAM_CODEC.decode(buffer);
			int retainTime = ByteBufCodecs.VAR_INT.decode(buffer);
			byte humiditySteps = ByteBufCodecs.BYTE.decode(buffer);
			byte temperatureSteps = ByteBufCodecs.BYTE.decode(buffer);

			return new HygroregulatorRecipe(recipeId, liquid, retainTime, humiditySteps, temperatureSteps);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buffer, HygroregulatorRecipe recipe) {
			Identifier.STREAM_CODEC.encode(buffer, recipe.id);
			FluidStackTemplate.STREAM_CODEC.encode(buffer, recipe.liquid);
			ByteBufCodecs.VAR_INT.encode(buffer, recipe.retainTime);
			ByteBufCodecs.BYTE.encode(buffer, recipe.humiditySteps);
			ByteBufCodecs.BYTE.encode(buffer, recipe.temperatureSteps);
		}
	}
}
