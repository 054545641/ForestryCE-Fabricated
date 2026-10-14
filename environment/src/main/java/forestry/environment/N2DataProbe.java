package forestry.environment;

import com.google.gson.JsonParser;
import forestry.core.content.machines.recipes.RecipeSerializers;
import forestry.core.platform.util.InventoryData;
import forestry.core.platform.util.SavedDataMigration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.minecraft.world.item.crafting.Ingredient;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.material.Fluids;

import java.nio.file.Files;

public final class N2DataProbe {
	public static void inventory(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();
		ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("N2 registry round trip"));
		stack.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
		SimpleContainer original = new SimpleContainer(300);
		original.setItem(299, stack);
		CompoundTag saved = new CompoundTag();
		InventoryData.write(original, "inventory", saved, registries);
		CompoundTag nullableProduct = new CompoundTag();
		helper.assertTrue(InventoryData.readNullableStack(nullableProduct, "PendingProduct", registries) == null, "Missing nullable product means idle");
		nullableProduct.put("PendingProduct", new CompoundTag());
		helper.assertTrue(InventoryData.readNullableStack(nullableProduct, "PendingProduct", registries) == null, "Empty nullable product means idle");
		nullableProduct.put("PendingProduct", InventoryData.writeStack(stack, registries));
		helper.assertTrue(ItemStack.matches(stack, InventoryData.readNullableStack(nullableProduct, "PendingProduct", registries)), "Pending product retains registry-backed components");
		var problems = new net.minecraft.util.ProblemReporter.Collector();
		var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(problems, registries);
		InventoryData.write(original, "inventory", output);
		helper.assertTrue(problems.isEmpty() && saved.equals(output.buildResult()), "ValueOutput uses its registry context and preserves the legacy inventory format");
		SimpleContainer restored = new SimpleContainer(300);
		restored.setItem(0, new ItemStack(Items.STONE));
		InventoryData.read(restored, "inventory", saved, registries);
		helper.assertTrue(ItemStack.matches(stack, restored.getItem(299)), "High slot and registry-backed components survive saving");
		helper.assertTrue(restored.getItem(0).isEmpty(), "Reload clears omitted slots");
		CompoundTag invalid = saved.copy();
		invalid.getListOrEmpty("inventory").getCompoundOrEmpty(0).putString("id", "forestry_environment:missing_item");
		boolean rejected = false;
		try {
			InventoryData.read(restored, "inventory", invalid, registries);
		} catch (IllegalStateException exception) {
			rejected = true;
		}
		helper.assertTrue(rejected, "Unknown items fail explicitly");
		helper.assertTrue(ItemStack.matches(stack, restored.getItem(299)), "Failed decode preserves live contents");
		var pending = new java.util.ArrayDeque<ItemStack>();
		var savedQueue = new net.minecraft.nbt.ListTag();
		savedQueue.add(InventoryData.writeStack(stack, registries));
		var queueOutput = net.minecraft.world.level.storage.TagValueOutput.createWithContext(problems, registries);
		InventoryData.writeStacks(java.util.List.of(stack), "PendingProducts", queueOutput);
		helper.assertTrue(problems.isEmpty() && savedQueue.equals(queueOutput.buildResult().getListOrEmpty("PendingProducts")), "Native pending-product output retains components and legacy list shape");
		InventoryData.replaceStacks(pending, savedQueue, registries);
		InventoryData.replaceStacks(pending, savedQueue, registries);
		helper.assertTrue(pending.size() == 1 && ItemStack.matches(stack, pending.peek()), "Repeated loading replaces pending products");
		var brokenQueue = savedQueue.copy();
		CompoundTag broken = InventoryData.writeStack(stack, registries);
		broken.putString("id", "forestry_environment:missing_item");
		brokenQueue.add(broken);
		rejected = false;
		try {
			InventoryData.replaceStacks(pending, brokenQueue, registries);
		} catch (IllegalStateException exception) {
			rejected = true;
		}
		helper.assertTrue(rejected && pending.size() == 1 && ItemStack.matches(stack, pending.peek()), "Malformed queue cannot partially replace pending products");
		InventoryData.replaceStacks(pending, new net.minecraft.nbt.ListTag(), registries);
		helper.assertTrue(pending.isEmpty(), "Missing products clear the previous queue");
		helper.succeed();
	}

	public static void codecs(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();
		var ingredient = RecipeSerializers.deserialize(JsonParser.parseString("{\"neoforge:ingredient_type\":\"neoforge:compound\",\"children\":[\"#minecraft:logs\",\"minecraft:stone\"]}"), registries).orElseThrow();
		helper.assertTrue(ingredient.test(new ItemStack(Items.OAK_LOG)), "Compound ingredient retains its tag alternative");
		helper.assertTrue(ingredient.test(new ItemStack(Items.STONE)), "Compound ingredient retains its item alternative");
		helper.assertTrue(!ingredient.test(new ItemStack(Items.DIAMOND)), "Compound ingredient rejects unrelated items");
		helper.assertTrue(RecipeSerializers.deserialize(JsonParser.parseString("[]"), registries).isEmpty(), "Legacy empty optional ingredient");
		var bucket = DataComponentIngredient.of(true, new ItemStack(Items.WATER_BUCKET));
		var bucketJson = Ingredient.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), bucket).getOrThrow();
		helper.assertTrue(RecipeSerializers.deserialize(bucketJson, registries).orElseThrow().test(new ItemStack(Items.WATER_BUCKET)), "Exhaustive component ingredient round trip");
		try {
			Files.writeString(java.nio.file.Path.of("n2-water-bucket-ingredient.json"), bucketJson.toString());
		} catch (java.io.IOException exception) {
			throw new java.io.UncheckedIOException(exception);
		}
		ItemStack stack = new ItemStack(Items.DIAMOND, 12);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("N2 JSON"));
		helper.assertTrue(ItemStack.matches(stack, RecipeSerializers.item(RecipeSerializers.item(stack, registries), registries)), "Item JSON round trip");
		FluidStack fluid = new FluidStack(Fluids.WATER, 750);
		FluidStack decoded = RecipeSerializers.deserializeFluid(RecipeSerializers.serializeFluid(fluid, registries), registries);
		helper.assertTrue(FluidStack.isSameFluidSameComponents(fluid, decoded) && decoded.getAmount() == 750, "Fluid JSON round trip");
		helper.succeed();
	}

	public static void savedData(GameTestHelper helper) {
		try {
			var root = Files.createTempDirectory("forestry-n2-saved-data-");
			var legacyDirectory = Files.createDirectories(root.resolve("data"));
			var directory = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(net.minecraft.world.level.Level.OVERWORLD, root).resolve("data");
			CompoundTag value = new CompoundTag();
			value.putInt("count", 42);
			CompoundTag envelope = new CompoundTag();
			envelope.put("data", value);
			NbtIo.writeCompressed(envelope, legacyDirectory.resolve("ApiaristTracker.common.dat"));
			var type = new SavedDataType<>(Identifier.fromNamespaceAndPath("forestry_environment", "breeding_tracker/bee/common"),
				() -> new ProbeData(new CompoundTag()), CompoundTag.CODEC.xmap(ProbeData::new, data -> data.tag));
			try (var storage = new SavedDataStorage(helper.getLevel(), directory, DataFixers.getDataFixer(), helper.getLevel().registryAccess())) {
				var migrated = SavedDataMigration.getOrCreate(storage, directory, type, () -> new ProbeData(new CompoundTag()),
					ProbeData::new, "ApiaristTracker.common", directory, legacyDirectory);
				helper.assertTrue(migrated.tag.getIntOr("count", 0) == 42, "Legacy root data is imported into the new dimension storage");
			}
			helper.assertTrue(Files.exists(type.id().withSuffix(".dat").resolveAgainst(directory)), "SavedData uses the target dimension directory");
			try (var storage = new SavedDataStorage(helper.getLevel(), directory, DataFixers.getDataFixer(), helper.getLevel().registryAccess())) {
				var restored = SavedDataMigration.getOrCreate(storage, directory, type, () -> { throw new AssertionError("must not recreate"); },
					tag -> { throw new AssertionError("must not import twice"); }, "ApiaristTracker.common", legacyDirectory);
				helper.assertTrue(restored.tag.getIntOr("count", 0) == 42, "Existing namespaced data wins over the legacy file");
			}
			helper.assertTrue(Files.exists(legacyDirectory.resolve("ApiaristTracker.common.dat")), "Migration keeps the legacy file");

			var brokenType = new SavedDataType<>(Identifier.parse("forestry_environment:broken"),
				() -> new ProbeData(new CompoundTag()), type.codecFactory().create(helper.getLevel()));
			var brokenPath = brokenType.id().withSuffix(".dat").resolveAgainst(directory);
			Files.writeString(brokenPath, "invalid NBT");
			try (var storage = new SavedDataStorage(helper.getLevel(), directory, DataFixers.getDataFixer(), helper.getLevel().registryAccess())) {
				boolean rejected = false;
				try {
					SavedDataMigration.getOrCreate(storage, directory, brokenType, () -> { throw new AssertionError("must not recreate corrupt data"); },
						ProbeData::new, "ApiaristTracker.common", legacyDirectory);
				} catch (IllegalStateException exception) {
					rejected = true;
				}
				helper.assertTrue(rejected, "Corrupt namespaced data cannot fall back to legacy or empty data");
			}
			helper.assertTrue(Files.readString(brokenPath).equals("invalid NBT"), "Corrupt namespaced file is unchanged after storage closes");

			var missingType = new SavedDataType<>(Identifier.parse("forestry_environment:new"),
				() -> new ProbeData(new CompoundTag()), type.codecFactory().create(helper.getLevel()));
			Files.writeString(legacyDirectory.resolve("broken_legacy.dat"), "invalid legacy NBT");
			try (var storage = new SavedDataStorage(helper.getLevel(), directory, DataFixers.getDataFixer(), helper.getLevel().registryAccess())) {
				boolean rejected = false;
				try {
					SavedDataMigration.getOrCreate(storage, directory, missingType, () -> new ProbeData(new CompoundTag()),
						ProbeData::new, "broken_legacy", legacyDirectory);
				} catch (java.io.UncheckedIOException exception) {
					rejected = true;
				}
				helper.assertTrue(rejected, "Invalid legacy file is not replaced by an empty record");
			}
			helper.assertTrue(!Files.exists(missingType.id().withSuffix(".dat").resolveAgainst(directory)), "Failed legacy import does not create a new file");
			helper.succeed();
		} catch (java.io.IOException exception) {
			throw new java.io.UncheckedIOException(exception);
		}
	}

	private static final class ProbeData extends SavedData {
		private final CompoundTag tag;

		private ProbeData(CompoundTag tag) {
			this.tag = tag;
		}
	}
}
