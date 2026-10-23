package forestry.gametest;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;

import forestry.api.ForestryConstants;
import forestry.api.core.IProduct;
import forestry.api.core.Product;
import forestry.api.core.ProductType;
import forestry.api.core.machines.ICentrifugeRecipe;
import forestry.apiculture.bees.genetics.FireworkProduct;
import forestry.core.features.CoreItems;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.features.FactoryRecipeTypes;

/**
 * Behavioral oracle for "centrifuge products as an IProduct dispatch". Proves that the default {@link Product}
 * serialises byte-for-byte like the plain product codec (no {@code "type"} key) and that a dynamic product
 * ({@link FireworkProduct}) round-trips through its own declared type, that the list stream codec round-trips a mixed
 * list, and that the built-in centrifuge recipes still expose and resolve their products through the migrated path.
 */


public class CentrifugeProductDispatchTest {
	/** A plain Product must encode identically through the dispatch codec (no "type" key), and legacy JSON must decode. */

	public static void defaultProductMatchesPlainProductJson(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		Product product = Product.of(Items.SUGAR, 2, 0.9f);

		JsonElement viaProduct = Product.CODEC.encodeStart(ops, product).getOrThrow();
		JsonElement viaDispatch = IProduct.CODEC.encodeStart(ops, product).getOrThrow();
		if (!viaProduct.equals(viaDispatch)) {
			helper.fail("Default product must encode byte-for-byte like the plain product codec. plain=" + viaProduct + " dispatch=" + viaDispatch);
			return;
		}
		if (viaDispatch.getAsJsonObject().has("type")) {
			helper.fail("Default product must not write a \"type\" key: " + viaDispatch);
			return;
		}

		// Legacy JSON (a bare product, no "type") must still decode under the dispatch codec.
		IProduct decoded = IProduct.CODEC.parse(ops, viaProduct).getOrThrow();
		if (!product.equals(decoded)) {
			helper.fail("Legacy product JSON decoded to a different product: " + decoded);
			return;
		}

		helper.succeed();
	}

	/** A dynamic product must round-trip through its own declared "type" key. */

	public static void dynamicProductRoundTripsWithType(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		FireworkProduct firework = new FireworkProduct(0.5f);

		JsonElement json = IProduct.CODEC.encodeStart(ops, firework).getOrThrow();
		String type = json.getAsJsonObject().has("type") ? json.getAsJsonObject().get("type").getAsString() : null;
		if (!ForestryConstants.forestry("firework").toString().equals(type)) {
			helper.fail("Dynamic firework product must declare its type; got " + json);
			return;
		}

		IProduct decoded = IProduct.CODEC.parse(ops, json).getOrThrow();
		if (!(decoded instanceof FireworkProduct decodedFirework) || decodedFirework.chance() != 0.5f) {
			helper.fail("Firework product did not round-trip through the dispatch codec: " + decoded);
			return;
		}

		helper.succeed();
	}

	/**
	 * An unknown "type" must fail with an error naming it. The JSON is otherwise a valid plain product, so a
	 * dispatch built on trial decoding (ex. Codec#withAlternative) would silently decode it as a plain Product
	 * instead. This pins the "dispatch on key presence" behaviour that prevents that.
	 */

	public static void unknownProductTypeFailsInsteadOfFallingBack(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		String bogus = ForestryConstants.forestry("no_such_product_type").toString();

		JsonObject json = IProduct.CODEC.encodeStart(ops, Product.of(Items.SUGAR, 2, 0.9f)).getOrThrow().getAsJsonObject();
		json.addProperty("type", bogus);

		DataResult<IProduct> result = IProduct.CODEC.parse(ops, json);
		if (result.result().isPresent()) {
			helper.fail("Unknown product type must not decode, got: " + result.result().get());
			return;
		}
		if (result.error().isEmpty() || !result.error().get().message().contains(bogus)) {
			helper.fail("Unknown product type error must name the type, got: " + result.error().map(DataResult.Error::message).orElse("no error"));
			return;
		}

		helper.succeed();
	}

	/** Encoding a product whose type was never registered must fail rather than write a dangling type key. */

	public static void unregisteredProductTypeFailsToEncode(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);

		DataResult<JsonElement> result = IProduct.CODEC.encodeStart(ops, new UnregisteredProduct(0.5f));
		if (result.error().isEmpty() || !result.error().get().message().contains("Unregistered product type")) {
			helper.fail("Unregistered product type must fail to encode, got: " + result);
			return;
		}

		helper.succeed();
	}

	/** The list stream codec must round-trip a mixed list of a plain and a dynamic product. */

	public static void dispatchListStreamRoundTrip(GameTestHelper helper) {
		List<IProduct> products = List.of(Product.of(Items.SUGAR, 2, 0.9f), new FireworkProduct(0.5f));
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());

		IProduct.LIST_STREAM_CODEC.encode(buf, products);
		List<IProduct> decoded = IProduct.LIST_STREAM_CODEC.decode(buf);
		if (decoded.size() != 2 || !products.get(0).equals(decoded.get(0)) || !(decoded.get(1) instanceof FireworkProduct)) {
			helper.fail("List stream codec round-trip changed the products: " + decoded);
			return;
		}

		helper.succeed();
	}

	/** The built-in centrifuge recipes must still expose and resolve their products through the migrated path. */

	public static void centrifugeRecipesResolveProducts(GameTestHelper helper) {
		RecipeManager manager = helper.getLevel().getServer().getRecipeManager();

		ICentrifugeRecipe honeyComb = RecipeUtils.getRecipes(manager.recipeMap(), FactoryRecipeTypes.CENTRIFUGE)
			.filter(recipe -> recipe.getId().equals(ForestryConstants.forestry("centrifuge/honey_comb")))
			.findFirst()
			.orElse(null);
		if (honeyComb == null) {
			helper.fail("Missing built-in centrifuge/honey_comb recipe");
			return;
		}

		// Beeswax is a guaranteed (chance 1.0) product of the honey comb.
		IProduct beeswax = honeyComb.getAllProducts().stream()
			.filter(product -> product.item() == CoreItems.BEESWAX.item())
			.findFirst()
			.orElse(null);
		if (beeswax == null) {
			helper.fail("Honey comb no longer lists beeswax among its products: " + honeyComb.getAllProducts());
			return;
		}
		if (beeswax.chance() != 1.0f) {
			helper.fail("Beeswax product chance changed: " + beeswax.chance());
			return;
		}

		// A guaranteed product must actually be produced.
		List<ItemStack> produced = honeyComb.getProducts(RandomSource.create(0), 1.0);
		boolean gotBeeswax = produced.stream().anyMatch(stack -> stack.is(CoreItems.BEESWAX.item()) && !stack.isEmpty());
		if (!gotBeeswax) {
			helper.fail("Honey comb did not produce its guaranteed beeswax output: " + produced);
			return;
		}

		helper.succeed();
	}

	/** A product whose type is deliberately never registered, used to exercise the encode-side guard. */
	private record UnregisteredProduct(float chance) implements IProduct {
		static final MapCodec<UnregisteredProduct> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.FLOAT.fieldOf("chance").forGetter(UnregisteredProduct::chance)
		).apply(instance, UnregisteredProduct::new));
		static final ProductType<UnregisteredProduct> TYPE = new ProductType<>(MAP_CODEC, StreamCodec.unit(new UnregisteredProduct(1f)));

		@Override
		public Item item() {
			return Items.SUGAR;
		}

		@Override
		public ItemStack createStack() {
			return new ItemStack(Items.SUGAR);
		}

		@Override
		public ProductType<?> type() {
			return TYPE;
		}
	}
}
