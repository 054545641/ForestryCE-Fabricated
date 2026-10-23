package forestry.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import forestry.api.ForestryConstants;
import forestry.api.core.FluidProduct;
import forestry.api.core.IFluidProduct;

@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class FluidProductCodecTest {
	@GameTest(template = "empty")
	public static void referenceImplProducesStack(GameTestHelper helper) {
		FluidProduct product = FluidProduct.of(Fluids.WATER, 1000);
		FluidStack stack = product.createFluidStack();
		FluidStack random = product.createRandomFluidStack(helper.getLevel().random);
		if (stack.getFluid() != Fluids.WATER || stack.getAmount() != 1000) {
			helper.fail("createFluidStack did not return the wrapped fluid/amount: " + stack);
			return;
		}
		if (random.getFluid() != Fluids.WATER || random.getAmount() != 1000) {
			helper.fail("createRandomFluidStack did not return the wrapped fluid/amount: " + random);
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void referenceImplRoundTrips(GameTestHelper helper) {
		FluidProduct original = FluidProduct.of(Fluids.LAVA, 500);

		// Stream round-trip
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		FluidProduct.STREAM_CODEC.encode(buf, original);
		FluidProduct streamDecoded = FluidProduct.STREAM_CODEC.decode(buf);
		if (streamDecoded.stack().getFluid() != Fluids.LAVA || streamDecoded.stack().getAmount() != 500) {
			helper.fail("Stream round-trip lost fluid/amount: " + streamDecoded.stack());
			return;
		}

		// JSON round-trip (fluid holder codec needs a registry-backed ops)
		RegistryOps<com.google.gson.JsonElement> jsonOps = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		var json = FluidProduct.MAP_CODEC.codec().encodeStart(jsonOps, original).getOrThrow();
		FluidProduct jsonDecoded = FluidProduct.MAP_CODEC.codec().parse(jsonOps, json).getOrThrow();
		if (jsonDecoded.stack().getFluid() != Fluids.LAVA || jsonDecoded.stack().getAmount() != 500) {
			helper.fail("JSON round-trip lost fluid/amount: " + json);
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void dispatchDefaultTypeOmitsTypeKey(GameTestHelper helper) {
		RegistryOps<com.google.gson.JsonElement> jsonOps = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		IFluidProduct product = FluidProduct.of(Fluids.WATER, 1000);

		var json = IFluidProduct.CODEC.encodeStart(jsonOps, product).getOrThrow();
		if (json.isJsonObject() && json.getAsJsonObject().has("type")) {
			helper.fail("Default FluidProduct must serialize without a 'type' key, got: " + json);
			return;
		}
		IFluidProduct decoded = IFluidProduct.CODEC.parse(jsonOps, json).getOrThrow();
		if (!(decoded instanceof FluidProduct fp) || fp.stack().getFluid() != Fluids.WATER || fp.stack().getAmount() != 1000) {
			helper.fail("Dispatch JSON round-trip failed for default FluidProduct: " + json);
			return;
		}
		helper.succeed();
	}

	/**
	 * An unknown "type" must fail with an error naming it, not fall back to the default FluidProduct. The JSON is
	 * otherwise a valid default product, so a dispatch built on trial decoding would silently accept it.
	 */
	@GameTest(template = "empty")
	public static void unknownFluidProductTypeFailsInsteadOfFallingBack(GameTestHelper helper) {
		RegistryOps<com.google.gson.JsonElement> jsonOps = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		String bogus = ForestryConstants.forestry("no_such_fluid_product_type").toString();

		JsonObject json = IFluidProduct.CODEC.encodeStart(jsonOps, FluidProduct.of(Fluids.WATER, 1000)).getOrThrow().getAsJsonObject();
		json.addProperty("type", bogus);

		DataResult<IFluidProduct> result = IFluidProduct.CODEC.parse(jsonOps, json);
		if (result.result().isPresent()) {
			helper.fail("Unknown fluid product type must not decode, got: " + result.result().get());
			return;
		}
		if (result.error().isEmpty() || !result.error().get().message().contains(bogus)) {
			helper.fail("Unknown fluid product type error must name the type, got: " + result.error().map(DataResult.Error::message).orElse("no error"));
			return;
		}

		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void dispatchStreamRoundTrip(GameTestHelper helper) {
		IFluidProduct product = FluidProduct.of(Fluids.LAVA, 500);
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		IFluidProduct.STREAM_CODEC.encode(buf, product);
		IFluidProduct decoded = IFluidProduct.STREAM_CODEC.decode(buf);
		if (!(decoded instanceof FluidProduct fp) || fp.stack().getFluid() != Fluids.LAVA || fp.stack().getAmount() != 500) {
			helper.fail("Dispatch stream round-trip failed for default FluidProduct");
			return;
		}
		helper.succeed();
	}
}
