package forestry.fabric.test;

import forestry.Forestry;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class F1ClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        for (String mod : new String[]{"jade", "jei"}) {
            boolean expected = Boolean.getBoolean("forestry.expect" + (mod.equals("jade") ? "Jade" : "Jei"));
            F1RegistrationTests.require(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(mod) == expected,
                "Incorrect development tool state: " + mod);
        }
        context.runOnClient(client -> {
            for (var fluid : forestry.core.platform.fluids.ForestryFluids.values()) {
                var variant = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid.getFluid());
                var name = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(variant).getString();
                F1RegistrationTests.require(!name.startsWith("fluid.") && !name.startsWith("fluid_type."), "Untranslated fluid name: " + name);
            }
            var models = client.getModelManager();
            var missing = models.getItemModel(F1RegistrationTests.id("deliberately_missing_test_model"));
            int checked = 0;
            for (var id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                if (id.getNamespace().equals("forestry")) {
                    F1RegistrationTests.require(models.getItemModel(id) != missing, "Missing baked item model: " + id);
                    checked++;
                }
            }
            Forestry.LOGGER.info("FORESTRY_F2_ITEM_MODELS_VERIFIED count={}", checked);
        });
        context.takeScreenshot("forestry-f2-title");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("fill -4 99 -4 4 99 4 minecraft:stone");
            world.getServer().runCommand("setblock 0 100 3 forestry:apatite_ore");
            world.getServer().runCommand("tp @p 0.5 100 0.5 0 15");
            world.getServer().runCommand("give @p forestry:apatite");
            world.getServer().runCommand("give @p forestry:foresters_manual");
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("forestrymail")) world.getServer().runCommand("give @p forestry:stamp_1n");
            world.getConnection().waitForChunksRender();
            context.getInput().lookAt(new BlockPos(0, 100, 3));
            context.waitTicks(30);
            context.takeScreenshot("forestry-f2-jade-ore");
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitForScreen(CreativeModeInventoryScreen.class);
            context.waitTicks(20);
            context.runOnClient(F1ClientTest::verifyCreativeSpecies);
            for (String name : new String[]{"apiculture", "arboriculture", "lepidopterology"}) {
                if (name.equals("lepidopterology") && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("forestrybutterflies")) continue;
                context.runOnClient(client -> {
                    var tab = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValue(F1RegistrationTests.id(name));
                    F1RegistrationTests.require(((net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen) client.gui.screen()).setSelectedTab(tab), "Could not select genetic creative tab");
                });
                context.waitTicks(5);
                context.takeScreenshot("forestry-species-" + name);
            }
            verifyTreeCreativeScreens(context);
            context.takeScreenshot("forestry-f2-items");
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
                context.waitFor(client -> F1JeiProbe.isReady());
                context.runOnClient(client -> F1JeiProbe.verifyGeneticIngredients());
                context.runOnClient(client -> F1JeiProbe.showApatiteRecipe());
                context.waitTicks(20);
                context.takeScreenshot("forestry-f2-jei-recipe");
            }
            context.setScreen(() -> null);
        }
        Forestry.LOGGER.info("FORESTRY_F2_CLIENT_TEST_PASSED");
    }

    public static void verifyCreativeSpecies(net.minecraft.client.Minecraft client) {
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(client.level.enabledFeatures(), true, client.level.registryAccess());
        for (String tabName : new String[]{"apiculture", "arboriculture", "lepidopterology"}) {
            var type = switch (tabName) {
                case "apiculture" -> SpeciesUtil.BEE_TYPE.get();
                case "arboriculture" -> SpeciesUtil.TREE_TYPE.get();
                default -> SpeciesUtil.getButterflyTypeSafe();
            };
            if (type == null) continue;
            var tab = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValue(F1RegistrationTests.id(tabName));
            F3WorldTests.verifyGeneticVariants(tab.getDisplayItems(), type, "client " + tabName);
            F3WorldTests.verifyGeneticVariants(net.minecraft.world.item.CreativeModeTabs.searchTab().getDisplayItems(), type, "creative search");
            Forestry.LOGGER.info("FORESTRY_GENETIC_CREATIVE_VERIFIED type={} species={} stages={}", type.id(), type.getSpeciesCount(), type.getLifeStages().size());
        }
    }

    private static void verifyTreeCreativeScreens(ClientGameTestContext context) {
        for (var stage : TreeLifeStage.values()) {
            context.runOnClient(client -> {
                var screen = (FabricCreativeModeInventoryScreen) client.gui.screen();
                F1RegistrationTests.require(screen.setSelectedTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(F1RegistrationTests.id("arboriculture"))),
                    "Could not select the arboriculture creative tab");
                F1RegistrationTests.require(screen.setSelectedTab(CreativeModeTabs.searchTab()), "Could not select the native creative search tab");
            });
            context.getInput().typeChars(stage.itemId().toString());
            context.waitTicks(5);
            var expected = context.computeOnClient(client -> {
                var screen = (CreativeModeInventoryScreen) client.gui.screen();
                F1RegistrationTests.require(screen.getMenu().items.stream().allMatch(stack -> stack.is(stage.getItemForm())),
                    "Native tree creative search returned an unrelated item");
                F3WorldTests.verifyGeneticStageVariants(screen.getMenu().items, SpeciesUtil.TREE_TYPE.get(), stage, "native creative search");
                return Set.copyOf(SpeciesUtil.TREE_TYPE.get().getAllSpeciesIds());
            });
            context.takeScreenshot("forestry-tree-" + stage.getSerializedName() + "-creative-top");
            var photographed = new HashSet<>(visibleTreeSpecies(context, stage));
            var center = context.computeOnClient(client -> new double[]{client.getWindow().getScreenWidth() / 2.0, client.getWindow().getScreenHeight() / 2.0});
            context.getInput().setCursorPos(center[0], center[1]);
            context.getInput().scroll(-10000);
            context.getInput().setCursorPos(0, 0);
            context.waitTicks(5);
            photographed.addAll(visibleTreeSpecies(context, stage));
            context.takeScreenshot("forestry-tree-" + stage.getSerializedName() + "-creative-bottom");
            F1RegistrationTests.require(photographed.equals(expected), "Creative screenshots do not cover every tree species for " + stage.getSerializedName());
            Forestry.LOGGER.info("FORESTRY_TREE_CREATIVE_SCREENSHOTS_VERIFIED stage={} species={} pages=2", stage.getSerializedName(), expected.size());
        }
    }

    private static Set<Identifier> visibleTreeSpecies(ClientGameTestContext context, TreeLifeStage stage) {
        return context.computeOnClient(client -> {
            var screen = (CreativeModeInventoryScreen) client.gui.screen();
            var visible = new HashSet<Identifier>();
            for (int slot = 0; slot < 45; slot++) {
                var stack = screen.getMenu().getSlot(slot).getItem();
                if (stack.isEmpty()) continue;
                F1RegistrationTests.require(stack.is(stage.getItemForm()) && IIndividualHandlerItem.hasIndividual(stack),
                    "Visible tree creative slot contains an unrelated or bare item");
                var individual = IIndividualHandlerItem.getIndividual(stack);
                F1RegistrationTests.require(individual != null && visible.add(individual.getSpecies().id()),
                    "Visible tree creative slots contain an invalid or duplicate species");
            }
            return Set.copyOf(visible);
        });
    }
}
