package forestry.api.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import forestry.api.ForestryRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import forestry.api.core.fluids.FluidVolume;

/**
 * A fluid analog of {@link IProduct}. Represents a fluid output, for example, from the Squeezer.
 *
 * @see FluidProduct The default fixed-fluid implementation used by Forestry's own recipes.
 */
public interface IFluidProduct {
	String TYPE_KEY = "type";

	MapCodec<IFluidProduct> MAP_CODEC = OptionalTypeMapCodec.of(ForestryRegistries.FLUID_PRODUCT_TYPE, "fluid product type", TYPE_KEY, () -> FluidProduct.TYPE, IFluidProduct::type, FluidProductType::codec);
	Codec<IFluidProduct> CODEC = MAP_CODEC.codec();
	StreamCodec<RegistryFriendlyByteBuf, IFluidProduct> STREAM_CODEC = ByteBufCodecs.registry(ForestryRegistries.Keys.FLUID_PRODUCT_TYPE).dispatch(IFluidProduct::type, FluidProductType::streamCodec);

	/**
	 * Creates a new, non-random stack to represent this product in recipe viewers. <p>
	 *
	 * The fluid amount should be the MAXIMUM amount of fluid a stack returned by this product's
	 * {@link #createRandomFluidVolume(RandomSource)} can have. For example, if your recipe produces 100-300 mB of some
	 * fluid, this method should return a stack with 300 mB of fluid. This ensures machines like the Squeezer know how much
	 * space must be available to fit this fluid product into the output tank. <p>
	 *
	 * This can return empty, like in the case where the product is a modded fluid not present in the current instance.
	 * However, in these cases, your recipe should use the proper conditions to disable itself rather than relying on this behavior.
	 *
	 * @return A new stack representing this product. If your product is dynamic, return a "default" stack.
	 */
	FluidVolume createFluidVolume();

	/**
	 * Used to produce a fluid stack from this product, taking randomness into account. This is the final result produced
	 * by machines. <p>
	 *
	 * @param random The random source. If no randomness is desired, this defaults to {@link #createFluidVolume()}.
	 * @return A new stack for this cycle, possibly empty or smaller than the representative stack.
	 */
	default FluidVolume createRandomFluidVolume(RandomSource random) {
		return createFluidVolume();
	}

	/**
	 * The type of this product, used to (de)serialize it via {@link #CODEC}. Plain {@link FluidProduct} instances
	 * return {@link FluidProduct#TYPE}, which the dispatch codec treats as the default: it serializes without a
	 * {@code "type"} key.
	 *
	 * @return The type of this product.
	 */
	FluidProductType<?> type();
}
