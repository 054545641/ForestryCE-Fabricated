package forestry.lifecycle;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import forestry.api.IForestryApi;
import forestry.api.agriculture.ForestryFarmTypes;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.core.machines.fuels.MoistenerFuel;
import forestry.apiculture.bees.genetics.HiveDrop;
import forestry.core.ModuleCore;
import forestry.core.content.machines.recipes.CarpenterRecipe;
import forestry.core.content.machines.recipes.SqueezerRecipe;
import forestry.core.content.machines.recipes.StillRecipe;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.recipes.FluidContainerIngredient;
import forestry.core.platform.util.SpeciesUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Runs with the four actual production mods; no substitute Forestry managers or recipes. */
@Mod(LifecycleTests.ID)
public final class LifecycleTests {
	public static final String ID = "forestrylifecycletest";
	private static final DeferredRegister<Consumer<GameTestHelper>> TESTS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, ID);
	private static final List<Recipe<?>> EARLY_RECIPES = new ArrayList<>();
	private static Ingredient earlyContainer;
	private static MoistenerFuel earlyFuel;
	private static HiveDrop earlyDrop;
	private static int dropCalls;
	private static boolean checkedUnbound;

	static {
		TESTS.register("socket_cursor_removal", () -> SocketRemovalTests::cursorRemoval);
		TESTS.register("socket_held_iron_rejected", () -> SocketRemovalTests::heldIronRejected);
		TESTS.register("socket_full_inventory", () -> SocketRemovalTests::fullInventory);
		TESTS.register("socket_cursor_iron_breaks", () -> SocketRemovalTests::cursorIronBreaks);
		TESTS.register("fluid_player_movement", () -> FluidMovementTests::playerMovement);
		TESTS.register("fluid_honey_current_release", () -> FluidMovementTests::honeyCurrentRelease);
		TESTS.register("analyzer_vanilla_saplings", () -> AnalyzerRegressionTests::vanillaSaplings);
		TESTS.register("analyzer_repeated_bees", () -> AnalyzerRegressionTests::repeatedBees);
		TESTS.register("analyzer_plant_oak", () -> AnalyzerRegressionTests::plantAnalyzedOak);
		TESTS.register("n4_blocked_nursery", () -> N4GameplayTests::blockedNursery);
		TESTS.register("n4_damaged_blocked_nursery", () -> N4GameplayTests::damagedBlockedNursery);
		TESTS.register("n4_farm_5x4x5", () -> h -> N4WorldgenTests.farmSize(h, 5, 5));
		TESTS.register("n4_farm_5x4x4", () -> h -> N4WorldgenTests.farmSize(h, 5, 4));
		TESTS.register("n4_farm_5x4x3", () -> h -> N4WorldgenTests.farmSize(h, 5, 3));
		TESTS.register("n4_farm_4x4x5", () -> h -> N4WorldgenTests.farmSize(h, 4, 5));
		TESTS.register("n4_farm_4x4x4", () -> h -> N4WorldgenTests.farmSize(h, 4, 4));
		TESTS.register("n4_farm_4x4x3", () -> h -> N4WorldgenTests.farmSize(h, 4, 3));
		TESTS.register("n4_farm_3x4x5", () -> h -> N4WorldgenTests.farmSize(h, 3, 5));
		TESTS.register("n4_farm_3x4x4", () -> h -> N4WorldgenTests.farmSize(h, 3, 4));
		TESTS.register("n4_farm_3x4x3", () -> h -> N4WorldgenTests.farmSize(h, 3, 3));
		TESTS.register("n4_village", () -> N4WorldgenTests::village);
		TESTS.register("n4_hivesandores", () -> N4WorldgenTests::hivesAndOres);
		TESTS.register("n4_crops", () -> N4GameplayTests::crops);
		TESTS.register("n4_backpacks", () -> N4GameplayTests::backpacks);
		TESTS.register("n4_butterflies", () -> N4GameplayTests::butterflies);
		TESTS.register("n4_trees", () -> N4GameplayTests::trees);
		TESTS.register("n4_beeoverflow", () -> N4GameplayTests::beeOverflow);
		TESTS.register("n4_bees", () -> N4GameplayTests::bees);
		forestry.gametest.N4LegacyTests.register(TESTS);
		TESTS.register("loaded_data", () -> LifecycleTests::loadedData);
		TESTS.register("recipe_templates", () -> LifecycleTests::recipeTemplates);
		TESTS.register("data_reload", () -> LifecycleTests::dataReload);
		TESTS.register("creative_tabs", () -> LifecycleTests::creativeTabs);
		TESTS.register("postal_delivery", () -> PostalTests::delivery);
		TESTS.register("postal_trade", () -> PostalTests::trade);
		TESTS.register("nbt_replacement", () -> N2SerializationTest::replacement);
		TESTS.register("n3_capabilities", () -> N3TransferTests::capabilities);
		TESTS.register("n3_containers", () -> N3TransferTests::containers);
		TESTS.register("n3_energy_transfer", () -> N3TransferTests::energyTransfer);
		TESTS.register("n3_engines", () -> N3FuelTests::engines);
		TESTS.register("n3_moistener", () -> N3FuelTests::moistener);
		TESTS.register("n3_farm_credit", () -> N3TransferTests::farmCredit);
		TESTS.register("n3_bottler_recovery", () -> N3TransferTests::bottlerRecovery);
		TESTS.register("n3_rainmaker", () -> N3TransferTests::rainmaker);
		TESTS.register("n3_upgraded_progress", () -> N3MachineTests::upgradedProgress);
		TESTS.register("n3_climate", () -> N3FuelTests::climate);
		TESTS.register("n3_energy", () -> N3MachineTests::energyRollback);
		TESTS.register("n3_readiness", () -> N3MachineTests::stillReadiness);
		TESTS.register("n3_outer_rollback", () -> N3MachineTests::stillOuterRollback);
		TESTS.register("n3_progress", () -> N3MachineTests::progress);
		TESTS.register("n3_carpenter", () -> N3MachineTests::carpenter);
		TESTS.register("n3_fermenter", () -> N3MachineTests::fermenter);
		TESTS.register("n3_squeezer", () -> N3MachineTests::squeezer);
		TESTS.register("n3_centrifuge", () -> N3MachineTests::centrifuge);
		TESTS.register("n3_smelter", () -> N3MachineTests::smelter);
		TESTS.register("n3_analyzer", () -> N3MachineTests::analyzer);
		TESTS.register("n3_bottler", () -> N3MachineTests::bottler);
		TESTS.register("n3_fabricator", () -> N3MachineTests::fabricator);
	}

	public LifecycleTests(IEventBus bus) {
		TESTS.register(bus);
		bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(LifecycleTests::beforeComponents));
	}

	private static void beforeComponents() {
		ModuleCore.ensureApiInitialized();
		if (Items.APPLE.builtInRegistryHolder().areComponentsBound()) throw new AssertionError("Test must decode before defaults bind");
		var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
		String grid = "\"recipe\":{\"pattern\":[\"##\",\"##\"],\"key\":{\"#\":\"minecraft:dirt\"},\"result\":{\"id\":\"minecraft:apple\",\"count\":2}}";
		List<String> recipes = List.of(
			"\"type\":\"forestry:still\",\"time\":100,\"input\":{\"id\":\"minecraft:water\",\"amount\":10},\"output\":{\"id\":\"minecraft:lava\",\"amount\":3}",
			"\"type\":\"forestry:moistener\",\"resource\":\"minecraft:dirt\",\"product\":{\"id\":\"minecraft:apple\",\"count\":2},\"time\":10",
			"\"type\":\"forestry:fabricator_smelting\",\"resource\":\"minecraft:sand\",\"product\":{\"id\":\"minecraft:lava\",\"amount\":144},\"melting\":1000",
			"\"type\":\"forestry:hygroregulator\",\"liquid\":{\"id\":\"minecraft:water\",\"amount\":100},\"time\":10,\"humidity_steps\":1,\"temperature_steps\":0",
			"\"type\":\"forestry:fermenter\",\"resource\":\"minecraft:apple\",\"fermentationValue\":100,\"modifier\":1.0,\"output\":\"minecraft:water\",\"fluidResource\":{\"id\":\"minecraft:water\",\"amount\":100}",
			"\"type\":\"forestry:fabricator\",\"molten\":{\"id\":\"minecraft:lava\",\"amount\":100}," + grid,
			"\"type\":\"forestry:carpenter\",\"time\":10," + grid,
			"\"type\":\"forestry:carpenter\",\"time\":10,\"liquid\":{\"id\":\"minecraft:water\",\"amount\":100},\"result\":{\"id\":\"minecraft:apple\",\"count\":3}," + grid,
			"\"type\":\"forestry:squeezer\",\"time\":10,\"resources\":[\"minecraft:apple\"],\"output\":{\"stack\":{\"id\":\"minecraft:water\",\"amount\":100}},\"chance\":0.0",
			"\"type\":\"forestry:squeezer_container\",\"container\":{\"id\":\"forestry:can\"},\"time\":10,\"remnants\":{\"id\":\"minecraft:iron_ingot\"},\"remnantsChance\":1.0");
		for (String body : recipes) {
			Recipe<?> recipe = Recipe.CODEC.parse(ops, JsonParser.parseString("{\"id\":\"forestrylifecycletest:early\"," + body + "}")).getOrThrow();
			Recipe.CODEC.encodeStart(ops, recipe).getOrThrow(); // Encoding must not materialize stacks either.
			EARLY_RECIPES.add(recipe);
		}
		earlyContainer = Ingredient.CODEC.parse(ops, JsonParser.parseString("{\"neoforge:ingredient_type\":\"forestry:fluid_container\",\"item\":\"forestry:can\",\"fluid\":{\"id\":\"minecraft:water\",\"amount\":1000}}")).getOrThrow();
		SlotDisplay.CODEC.encodeStart(ops, earlyContainer.display()).getOrThrow();
		earlyFuel = MoistenerFuel.CODEC.parse(ops, JsonParser.parseString("{\"product\":{\"id\":\"minecraft:apple\"},\"stage\":1,\"moistener_value\":10}")).getOrThrow();
		earlyDrop = new HiveDrop(1, ForestryBeeSpecies.FOREST, () -> {
			dropCalls++;
			return List.of(new ItemStack(Items.APPLE));
		}, 0, Map.of());
		if (dropCalls != 0) throw new AssertionError("Drop supplier evaluated during registration");
		checkedUnbound = true;
	}

	private static void creativeTabs(GameTestHelper h) {
		for (boolean permissions : new boolean[]{false, true, false}) {
			net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), permissions, h.getLevel().registryAccess());
			CreativeTabChecks.checkContents();
		}
		h.succeed();
	}

	private static void loadedData(GameTestHelper h) {
		assertLoaded(h);
		h.assertTrue(checkedUnbound && EARLY_RECIPES.size() == 10, "Early API and recipe setup completed with unbound components");
		var leaves = SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).getDecorativeLeaves();
		h.assertTrue(!leaves.isEmpty(), "Decorative leaves materialize after binding");
		leaves.setCount(12);
		h.assertTrue(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).getDecorativeLeaves().getCount() == 1, "Leaves do not share mutable stacks");
		h.assertTrue(earlyDrop.getExtraItems(h.getLevel(), h.absolutePos(net.minecraft.core.BlockPos.ZERO), 0).getFirst().is(Items.APPLE), "Hive extras resolve at drop time");
		h.assertTrue(dropCalls == 1 && earlyFuel.product().is(Items.APPLE), "Deferred drops and fuel outputs are available");
		String report = System.getProperty("forestry.lifecycle.itemModelReport");
		if (report != null) {
			var modelIds = new com.google.gson.JsonArray();
			BuiltInRegistries.ITEM.stream().filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("forestry"))
				.map(item -> item.getDefaultInstance().get(DataComponents.ITEM_MODEL)).filter(java.util.Objects::nonNull)
				.distinct().sorted().forEach(id -> modelIds.add(id.toString()));
			try { java.nio.file.Files.writeString(java.nio.file.Path.of(report), modelIds.toString()); }
			catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
		}
		h.succeed();
	}

	private static void assertLoaded(GameTestHelper h) {
		var api = IForestryApi.INSTANCE;
		h.assertTrue(SpeciesUtil.BEE_TYPE.get().getAllSpecies().size() == 69, "All bee species loaded");
		h.assertTrue(SpeciesUtil.TREE_TYPE.get().getAllSpecies().size() == 50, "All tree species loaded");
		h.assertTrue(SpeciesUtil.BUTTERFLY_TYPE.get().getAllSpecies().size() == 35, "All butterfly species loaded");
		h.assertTrue(api.getCircuitManager().getCircuitHolders().size() == 25, "All circuits loaded once");
		h.assertTrue(api.getFarmingManager().isLoaded() && !api.getFarmingManager().getFarmType(ForestryFarmTypes.ORCHARD).getIcon().isEmpty(), "Farm manager initialized after species and components");
		for (String path : List.of("cake_can", "cake_wax_capsule", "cake_refractory_capsule", "bog_earth_can", "letter_empty_fresh")) {
			h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("forestry", path))).isPresent(), "Recipe loaded: " + path);
		}
	}

	private static void recipeTemplates(GameTestHelper h) {
		var ops = RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess());
		for (Recipe<?> recipe : EARLY_RECIPES) {
			var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess(), ConnectionType.NEOFORGE);
			try {
				Recipe.STREAM_CODEC.encode(buffer, recipe);
				byte[] encoded = new byte[buffer.readableBytes()];
				buffer.getBytes(buffer.readerIndex(), encoded);
				var decoded = Recipe.STREAM_CODEC.decode(buffer);
				buffer.clear();
				Recipe.STREAM_CODEC.encode(buffer, decoded);
				byte[] reencoded = new byte[buffer.readableBytes()];
				buffer.getBytes(buffer.readerIndex(), reencoded);
				h.assertTrue(java.util.Arrays.equals(encoded, reencoded), "Recipe network round trip retains templates: " + recipe.getType());
			} finally { buffer.release(); }
		}
		h.assertTrue(((StillRecipe) EARLY_RECIPES.get(0)).getInput().getAmount() == 10, "Fluid amount preserved");
		h.assertTrue(((CarpenterRecipe) EARLY_RECIPES.get(6)).getInputFluid().isEmpty(), "Absent carpenter fluid stays empty");
		h.assertTrue(((CarpenterRecipe) EARLY_RECIPES.get(7)).getResultItem(h.getLevel().registryAccess()).getCount() == 3, "Explicit carpenter result preserved");
		h.assertTrue(((SqueezerRecipe) EARLY_RECIPES.get(8)).getRemnants().isEmpty(), "Absent squeezer remnant stays empty");
		var display = earlyContainer.display();
		ItemStack filled = display.resolveForFirstStack(SlotDisplayContext.fromLevel(h.getLevel()));
		h.assertTrue(earlyContainer.test(filled), "Display resolves to a matching filled capsule");
		filled.set(DataComponents.CUSTOM_NAME, Component.literal("Water"));
		h.assertTrue(earlyContainer.test(filled), "Unrelated item components do not change fluid matching");
		filled.set(CoreDataComponents.FLUID_CONTENT, SimpleFluidContent.copyOf(new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 500)));
		h.assertTrue(!earlyContainer.test(filled), "Partially filled capsule is rejected");
		filled.remove(CoreDataComponents.FLUID_CONTENT);
		h.assertTrue(!earlyContainer.test(filled), "Empty capsule is rejected");
		h.succeed();
	}

	private static void dataReload(GameTestHelper h) {
		var server = h.getLevel().getServer();
		var oldCircuits = IForestryApi.INSTANCE.getCircuitManager();
		var oldFarm = IForestryApi.INSTANCE.getFarmingManager();
		h.runAfterDelay(2, () -> server.reloadResources(server.getPackRepository().getSelectedIds()).whenCompleteAsync((unused, error) -> {
			if (error != null) { h.fail("Reload failed: " + error); return; }
			assertLoaded(h);
			h.assertTrue(oldCircuits != IForestryApi.INSTANCE.getCircuitManager() && oldFarm != IForestryApi.INSTANCE.getFarmingManager(), "Data managers are rebuilt on reload");
			h.assertTrue(earlyDrop.createIndividual(h.getLevel(), h.absolutePos(net.minecraft.core.BlockPos.ZERO)).getSpecies() == SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST), "Hive drop resolves current species after reload");
			h.succeed();
		}, server));
	}
}
