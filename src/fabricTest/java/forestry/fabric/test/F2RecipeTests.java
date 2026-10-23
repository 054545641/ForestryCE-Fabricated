package forestry.fabric.test;
import forestry.core.features.*;
import forestry.core.platform.item.FluidContainerType;
import forestry.core.platform.recipes.FluidContainerIngredient;
import forestry.core.platform.util.RecipeUtils;
import forestry.api.core.fluids.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import static forestry.fabric.test.F1RegistrationTests.require;
public final class F2RecipeTests {
    @GameTest public void filledIngredientTestsComponentsAndNativeWireCodec(GameTestHelper h) {
        var json = com.google.gson.JsonParser.parseString("{\"fabric:type\":\"forestry:fluid_container\",\"item\":\"forestry:can\",\"fluid\":{\"id\":\"minecraft:water\",\"amount\":1000}}");
        var ingredient = net.minecraft.world.item.crafting.Ingredient.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), json).getOrThrow();
        var stack = FluidsItems.CONTAINERS.get(FluidContainerType.CAN).stack();
        require(!ingredient.test(stack), "Empty can accepted by filled ingredient");
        stack.set(CoreDataComponents.FLUID_CONTENT.get(), FluidVolume.milliBuckets(Fluids.WATER, 999)); require(!ingredient.test(stack), "Partial capsule accepted by recipe");
        stack.set(CoreDataComponents.FLUID_CONTENT.get(), FluidVolume.milliBuckets(Fluids.WATER, 1000)); require(ingredient.test(stack), "Full native ingredient failed");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        try { net.minecraft.world.item.crafting.Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient); require(net.minecraft.world.item.crafting.Ingredient.CONTENTS_STREAM_CODEC.decode(buffer).test(stack), "Filled ingredient lost custom predicate over network"); }
        finally { buffer.release(); }
        var custom = new FluidContainerIngredient(stack.getItem().builtInRegistryHolder(), FluidTemplate.from(FluidVolume.milliBuckets(Fluids.WATER, 1000)));
        require(custom.test(custom.createDisplayStack()), "Recipe display does not match its ingredient");
        h.succeed();
    }
    @GameTest public void migratedCompoundConditionalAndComponentRecipesAreAvailable(GameTestHelper h) {
        for (String path : java.util.List.of("genetic_filter", "cake_can", "cake_wax_capsule", "cake_refractory_capsule", "bog_earth_can", "charcoal_from_block", "centrifuge/amber_drone", "centrifuge/amber_sapling", "smelter/silicon_from_coal", "carpenter/kit_axe"))
            require(RecipeUtils.getRecipe(Identifier.fromNamespaceAndPath("forestry", path)) != null, "Migrated server recipe missing: " + path);
        require(RecipeUtils.getRecipe(Identifier.parse("forestry:smelter/silicon_from_coke")) == null, "Absent coke tag did not disable conditional recipe");
        require(RecipeUtils.getRecipe(Identifier.parse("forestry:smelter/brass_from_ingots")) == null, "Absent brass/zinc tags did not disable conditional recipe");
        h.succeed();
    }
}
