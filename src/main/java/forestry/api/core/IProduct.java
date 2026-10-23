package forestry.api.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import forestry.api.ForestryRegistries;
import it.unimi.dsi.fastutil.Hash;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents some item that has a set chance of being produced.
 *
 * @see Product The default implementation used in the majority of cases.
 */
public interface IProduct {
	/**
	 * A hashing strategy used for FastUtil custom hash collections.
	 * Currently, Forestry uses this to remove common products between species from the product list of a hybrid bee.
	 */
	Hash.Strategy<IProduct> ITEM_ONLY_STRATEGY = new Hash.Strategy<>() {
		@Override
		public int hashCode(@Nullable IProduct o) {
			return o == null ? 0 : o.item().hashCode();
		}

		@Override
		public boolean equals(@Nullable IProduct a, @Nullable IProduct b) {
			return (a == null || b == null) ? a == b : a.item() == b.item();
		}
	};

	MapCodec<IProduct> MAP_CODEC = OptionalTypeMapCodec.of(ForestryRegistries.PRODUCT_TYPE, "product type", "type", () -> Product.TYPE, IProduct::type, ProductType::codec);
	Codec<IProduct> CODEC = MAP_CODEC.codec();
	StreamCodec<RegistryFriendlyByteBuf, IProduct> STREAM_CODEC = ByteBufCodecs.registry(ForestryRegistries.Keys.PRODUCT_TYPE).dispatch(IProduct::type, ProductType::streamCodec);

	Codec<List<IProduct>> LIST_CODEC = CODEC.listOf();
	StreamCodec<RegistryFriendlyByteBuf, List<IProduct>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

	// todo should this be replaced with is(ItemStack) and getIconStack() methods instead?

	/**
	 * Gets the item this product contains. In the case of a dynamic product, return an item that might
	 * be used to display it in a screen or for equality purposes in {@link #ITEM_ONLY_STRATEGY}.
	 *
	 * @return The item this product represents.
	 */
	Item item();

	/**
	 * @return The set chance of this product being produced.
	 */
	float chance();

	/**
	 * @return A new stack of this product. If your product is dynamic, return a "default" nonempty stack.
	 */
	ItemStack createStack();

	/**
	 * Used to produce a random variant of this product.
	 *
	 * @param random The random source. If no randomness is desired, call {@link #createStack} instead.
	 * @return A new stack of this product with potentially random properties.
	 */
	default ItemStack createRandomStack(RandomSource random) {
		return createStack();
	}

	/**
	 * The type of this product, used to (de)serialize it via {@link #CODEC}. Most products are plain
	 * {@link Product} instances and return
	 * {@link Product#TYPE}, which the dispatch codec treats as the default: it serializes without a {@code "type"}
	 * key. Dynamic products (e.g. a randomized firework) return their own type so their randomness survives the
	 * round-trip through JSON and network sync.
	 *
	 * @return The type of this product.
	 */
	ProductType<?> type();
}
