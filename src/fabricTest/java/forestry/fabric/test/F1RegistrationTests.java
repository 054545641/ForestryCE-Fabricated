package forestry.fabric.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import forestry.core.manual.ManualAccess;
import forestry.core.platform.registration.RegistrationBatch;
import forestry.modules.ForestryModuleManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class F1RegistrationTests {
    static Identifier id(String path) { return Identifier.fromNamespaceAndPath("forestry", path); }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    @GameTest
    public void installedModulesAndExactContent(GameTestHelper test) throws Exception {
        var expectedItems = new TreeSet<Identifier>();
        var expectedBlocks = new TreeSet<Identifier>();
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream("/forestry-f3-content.json"));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            var content = JsonParser.parseReader(reader).getAsJsonObject();
            int modules = 0;
            int requestedMask = Integer.getInteger("forestry.testModules", -1);
            require(requestedMask >= 0 && requestedMask <= 7, "Missing requested module mask");
            for (String owner : List.of("main", "butterflies", "farms", "mail")) {
                String mod = owner.equals("main") ? "forestry" : "forestry" + owner;
                int bit = List.of("butterflies", "farms", "mail").indexOf(owner);
                boolean expectedInstalled = owner.equals("main") || (requestedMask & (1 << bit)) != 0;
                require(FabricLoader.getInstance().isModLoaded(mod) == expectedInstalled, "Requested module was not actually loaded: " + mod);
                if (FabricLoader.getInstance().isModLoaded(mod)) {
                    modules++;
                    require(ForestryModuleManager.INSTANCE.getModulesForMod(mod).size() == 1, "Wrong owner mapping: " + mod);
                    content.getAsJsonObject(owner).getAsJsonArray("items").forEach(value -> expectedItems.add(id(value.getAsString())));
                    content.getAsJsonObject(owner).getAsJsonArray("blocks").forEach(value -> expectedBlocks.add(id(value.getAsString())));
                }
            }
            require(ForestryModuleManager.INSTANCE.getLoadedModules().size() == modules, "Module count mismatch");
            require(BuiltInRegistries.REGISTRY.containsKey(forestry.api.ForestryRegistries.Keys.POSTAL_CARRIER.identifier()) == ((requestedMask & 4) != 0), "Optional postal registry leaked or is missing");
            require(forestry.api.IForestryApi.INSTANCE.getFarmingManager().isLoaded() == ((requestedMask & 2) != 0), "Optional farm manager boundary mismatch");
            require((forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(forestry.api.core.genetics.ForestrySpeciesTypes.BUTTERFLY) != null) == ((requestedMask & 1) != 0), "Optional butterfly domain leaked or is missing");
        }
        var actualItems = new TreeSet<>(BuiltInRegistries.ITEM.keySet().stream().filter(key -> key.getNamespace().equals("forestry")).toList());
        var actualBlocks = new TreeSet<>(BuiltInRegistries.BLOCK.keySet().stream().filter(key -> key.getNamespace().equals("forestry")).toList());
        var inventory = new com.google.gson.JsonObject();
        inventory.add("items", new com.google.gson.Gson().toJsonTree(actualItems.stream().map(Identifier::getPath).toList()));
        inventory.add("blocks", new com.google.gson.Gson().toJsonTree(actualBlocks.stream().map(Identifier::getPath).toList()));
        java.nio.file.Files.writeString(java.nio.file.Path.of("f2-registry-snapshot.json"), inventory.toString());
        require(actualItems.equals(expectedItems), "Item registry differs from active Fabric scope: " + actualItems);
        require(actualBlocks.equals(expectedBlocks), "Block registry differs from active Fabric scope: " + actualBlocks);
        require(!BuiltInRegistries.ITEM.containsKey(id("butterfly_ge")), "F2 butterfly item must not be replaced with a placeholder");
        test.succeed();
    }

    @GameTest
    public void actualBlockTypesAndOreDrops(GameTestHelper test) {
        var ore = BuiltInRegistries.BLOCK.getValue(id("apatite_ore"));
        require(ore instanceof DropExperienceBlock, "Ore implementation was not preserved");
        require(BuiltInRegistries.BLOCK.getValue(id("ash_brick_stairs")) instanceof StairBlock, "Stairs must keep native behavior");
        require(BuiltInRegistries.BLOCK.getValue(id("ash_brick_slab")) instanceof SlabBlock, "Slabs must keep native behavior");
        require(BuiltInRegistries.BLOCK.getValue(id("ash_brick_wall")) instanceof WallBlock, "Walls must keep native behavior");
        test.setBlock(1, 1, 1, ore);
        test.assertBlockPresent(ore, 1, 1, 1);
        var drops = Block.getDrops(ore.defaultBlockState(), test.getLevel(), test.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)), null, null, new ItemStack(Items.DIAMOND_PICKAXE));
        int apatite = drops.stream().filter(stack -> stack.is(BuiltInRegistries.ITEM.getValue(id("apatite")))).mapToInt(ItemStack::getCount).sum();
        require(apatite >= 2 && apatite <= 7, "Apatite loot table did not preserve 2..7 drops: " + drops);
        test.succeed();
    }

    @GameTest
    public void vanillaCraftingAndOptionalMail(GameTestHelper test) {
        var impossible = test.getLevel().getServer().getRecipeManager().getRecipes().stream()
            .filter(recipe -> recipe.id().identifier().getNamespace().equals("forestry"))
            .filter(recipe -> !(recipe.value() instanceof forestry.api.core.machines.IForestryRecipe))
            .filter(recipe -> !(recipe.value() instanceof net.minecraft.world.item.crafting.CustomRecipe))
            .filter(recipe -> recipe.value().placementInfo().isImpossibleToPlace())
            .map(recipe -> recipe.id().identifier()).toList();
        require(impossible.isEmpty(), "Active recipes reference unavailable ingredients: " + impossible);
        var apatite = BuiltInRegistries.ITEM.getValue(id("apatite"));
        var input = CraftingInput.of(3, 3, java.util.stream.IntStream.range(0, 9).mapToObj(i -> new ItemStack(apatite)).toList());
        var recipe = test.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, test.getLevel()).orElseThrow();
        require(recipe.id().identifier().equals(id("apatite_block")), "Unexpected compression recipe");
        var output = recipe.value().assemble(input);
        require(output.is(BuiltInRegistries.ITEM.getValue(id("apatite_block"))) && output.getCount() == 1, "Compression output changed");
        boolean mail = FabricLoader.getInstance().isModLoaded("forestrymail");
        var stampRecipe = test.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, id("stamp_1n")));
        require(stampRecipe.isPresent() == mail, "Mail recipe leaked or is missing");
        test.succeed();
    }

    @GameTest
    @SuppressWarnings("unchecked")
    public void componentCodecRoundTrip(GameTestHelper test) {
        var analyzed = (DataComponentType<Boolean>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id("analyzed"));
        var health = (DataComponentType<Integer>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id("health"));
        require(analyzed != null && health != null, "Component registration missing");
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id("apatite")), 3);
        stack.set(analyzed, true);
        stack.set(health, 17);
        var ops = RegistryOps.create(JsonOps.INSTANCE, test.getLevel().registryAccess());
        var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        ItemStack decoded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        require(decoded.getCount() == 3 && Boolean.TRUE.equals(decoded.get(analyzed)) && decoded.get(health) == 17, "Component data changed in codec round-trip");
        require(encoded.toString().contains("forestry:analyzed") && encoded.toString().contains("forestry:health"), "Component IDs changed");
        test.succeed();
    }

    @GameTest
    public void manualAndRegistrationBoundaries(GameTestHelper test) throws Exception {
        require(!ManualAccess.get().isAvailable(), "Unexpected Patchouli adapter in F1");
        require(BuiltInRegistries.ITEM.getValue(id("foresters_manual")) instanceof forestry.core.content.tools.ForestersManualItem, "Manual was replaced with a plain item");
        try { RegistrationBatch.create(Registries.ITEM, "forestry"); throw new AssertionError("Late registration was allowed"); }
        catch (IllegalStateException expected) { }
        for (String type : List.of("net.neoforged.fml.ModList", "forestry.core.platform.compat.patchouli.PatchouliManualAccess")) {
            try { Class.forName(type, false, getClass().getClassLoader()); throw new AssertionError("Forbidden runtime class: " + type); }
            catch (ClassNotFoundException expected) { }
        }
        test.succeed();
    }
}
