package forestry.fabric.test;

import forestry.Forestry;
import forestry.arboriculture.leaves.AbstractLeavesBlock;
import forestry.arboriculture.leaves.DecorativeLeavesBlock;
import forestry.arboriculture.leaves.DefaultFruitLeavesBlock;
import forestry.arboriculture.leaves.DefaultLeavesBlock;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static forestry.fabric.test.F1RegistrationTests.id;
import static forestry.fabric.test.F1RegistrationTests.require;

@SuppressWarnings("UnstableApiUsage")
public final class F3LeafItemClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        boolean originalCutout = context.computeOnClient(client -> client.options.cutoutLeaves().get());
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getConnection().waitForChunksRender();
            context.runOnClient(client -> client.options.cutoutLeaves().set(true));
            verifyCreativeScreens(context);
            context.setScreen(() -> null);
            verifyBothModes(context, "initial");
            var reload = context.computeOnClient(Minecraft::reloadResourcePacks);
            context.waitFor(client -> reload.isDone() && client.gui.overlay() == null);
            reload.join();
            world.getConnection().waitForChunksRender();
            verifyBothModes(context, "reloaded");
        } finally {
            context.runOnClient(client -> client.options.cutoutLeaves().set(originalCutout));
        }
        Forestry.LOGGER.info("FORESTRY_LEAF_ITEM_CLIENT_TEST_PASSED");
    }

    private static void verifyBothModes(ClientGameTestContext context, String stage) {
        for (boolean cutout : new boolean[]{false, true}) {
            context.runOnClient(client -> {
                client.options.cutoutLeaves().set(cutout);
                verifyModels(client, stage, cutout);
            });
        }
    }

    private static void verifyModels(Minecraft client, String stage, boolean cutout) {
        Map<Identifier, Boolean> visibleSprites = new HashMap<>();
        int regular = 0;
        int fruit = 0;
        int decorative = 0;
        int genetic = 0;
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof AbstractLeavesBlock || blockItem.getBlock() instanceof DecorativeLeavesBlock)) continue;
            var itemId = BuiltInRegistries.ITEM.getKey(item);
            var state = new CapturedState();
            client.getItemModelResolver().updateForTopItem(state, new ItemStack(item), ItemDisplayContext.GUI, client.level, client.player, 0);
            require(!state.isEmpty(), "Empty leaf item render state: " + itemId);
            var visibleFoliageFaces = EnumSet.noneOf(Direction.class);
            int quadCount = 0;
            for (var layer : state.captured) {
                for (var quad : layer.prepareQuadList()) {
                    quadCount++;
                    require(nonDegenerate(quad), "Degenerate leaf item face: " + itemId);
                    var material = quad.materialInfo();
                    var sprite = material.sprite().contents();
                    require(!sprite.name().equals(MissingTextureAtlasSprite.getLocation()), "Missing leaf item texture: " + itemId);
                    if (material.isTinted()) {
                        require(material.tintIndex() < layer.tintLayers().size(), "Missing leaf item tint: " + itemId);
                        require(layer.tintLayers().getInt(material.tintIndex()) >>> 24 == 255,
                            "Transparent leaf item tint: " + itemId + " tint=" + material.tintIndex());
                    }
                    if (material.tintIndex() == AbstractLeavesBlock.FOLIAGE_COLOR_INDEX
                        && visibleSprites.computeIfAbsent(sprite.name(), ignored -> hasVisiblePixel(sprite))) {
                        visibleFoliageFaces.add(quad.direction());
                    }
                }
            }
            require(quadCount >= 6 && visibleFoliageFaces.size() == 6, "Leaf item lacks six visible foliage faces: " + itemId);
            var bounds = state.getModelBoundingBox();
            require(bounds.getXsize() > 0 && bounds.getYsize() > 0 && bounds.getZsize() > 0, "Leaf item has empty bounds: " + itemId);
            if (blockItem.getBlock() instanceof DefaultLeavesBlock) regular++;
            else if (blockItem.getBlock() instanceof DefaultFruitLeavesBlock) fruit++;
            else if (blockItem.getBlock() instanceof DecorativeLeavesBlock) decorative++;
            else genetic++;
        }
        require(regular == 50 && fruit == 50 && decorative == 50 && genetic == 1,
            "Unexpected registered leaf item coverage: " + regular + "/" + fruit + "/" + decorative + "/" + genetic);
        Forestry.LOGGER.info("FORESTRY_LEAF_ITEM_GEOMETRY_VERIFIED stage={} cutout={} count=151 regular={} fruit={} decorative={} genetic={}",
            stage, cutout, regular, fruit, decorative, genetic);
    }

    private static boolean nonDegenerate(BakedQuad quad) {
        var edge = new Vector3f(quad.position1()).sub(quad.position0());
        var diagonal = new Vector3f(quad.position2()).sub(quad.position0());
        var lastEdge = new Vector3f(quad.position3()).sub(quad.position0());
        return new Vector3f(edge).cross(diagonal).lengthSquared() > 0.000001f
            && new Vector3f(diagonal).cross(lastEdge).lengthSquared() > 0.000001f;
    }

    private static boolean hasVisiblePixel(SpriteContents sprite) {
        for (int frame : sprite.getUniqueFrames()) {
            for (int row = 0; row < sprite.height(); row++) {
                for (int column = 0; column < sprite.width(); column++) {
                    if (!sprite.isTransparent(frame, column, row)) return true;
                }
            }
        }
        return false;
    }

    private static void verifyCreativeScreens(ClientGameTestContext context) {
        context.getInput().pressKey(options -> options.keyInventory);
        context.waitForScreen(CreativeModeInventoryScreen.class);
        search(context, "forestry:sequoia");
        scroll(context, -10000);
        context.takeScreenshot("forestry-leaves-sequoia-creative");
        Set<Identifier> sequoia = Set.of(id("sequoia_default_leaves"), id("sequoia_default_leaves_fruit"), id("sequoia_decorative_leaves"));
        context.runOnClient(client -> {
            var screen = (CreativeModeInventoryScreen) client.gui.screen();
            var results = new HashSet<Identifier>();
            screen.getMenu().items.forEach(stack -> results.add(BuiltInRegistries.ITEM.getKey(stack.getItem())));
            require(results.containsAll(sequoia), "Native creative search omits a sequoia leaf variant");
        });
        require(visibleItems(context).containsAll(sequoia), "Sequoia leaf variants are not visible in the creative screenshot");

        search(context, "forestry:_default_leaves_fruit");
        context.takeScreenshot("forestry-leaves-fruit-creative-top");
        var expected = context.computeOnClient(client -> {
            var screen = (CreativeModeInventoryScreen) client.gui.screen();
            var results = new HashSet<Identifier>();
            for (var stack : screen.getMenu().items) {
                require(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof DefaultFruitLeavesBlock,
                    "Native fruit-leaf search returned another item");
                results.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
            require(results.size() == 50, "Native creative search does not expose all 50 fruit leaves");
            return Set.copyOf(results);
        });
        var photographed = new HashSet<>(visibleItems(context));
        scroll(context, -10000);
        photographed.addAll(visibleItems(context));
        context.takeScreenshot("forestry-leaves-fruit-creative-bottom");
        require(photographed.containsAll(expected), "Creative screenshots do not cover all 50 fruit leaves");
        Forestry.LOGGER.info("FORESTRY_LEAF_CREATIVE_SCREENSHOTS_VERIFIED sequoia=3 fruit=50 pages=2");
    }

    private static void search(ClientGameTestContext context, String query) {
        context.runOnClient(client -> {
            var screen = (FabricCreativeModeInventoryScreen) client.gui.screen();
            require(screen.setSelectedTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id("arboriculture"))), "Could not select the arboriculture creative tab");
            require(screen.setSelectedTab(CreativeModeTabs.searchTab()), "Could not select the native creative search tab");
        });
        context.getInput().typeChars(query);
        context.waitTicks(5);
    }

    private static void scroll(ClientGameTestContext context, double rows) {
        var center = context.computeOnClient(client -> new double[]{client.getWindow().getScreenWidth() / 2.0, client.getWindow().getScreenHeight() / 2.0});
        context.getInput().setCursorPos(center[0], center[1]);
        context.getInput().scroll(rows);
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(5);
    }

    private static Set<Identifier> visibleItems(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            var screen = (CreativeModeInventoryScreen) client.gui.screen();
            var visible = new HashSet<Identifier>();
            for (int slot = 0; slot < 45; slot++) {
                var stack = screen.getMenu().getSlot(slot).getItem();
                if (!stack.isEmpty()) visible.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
            return Set.copyOf(visible);
        });
    }

    private static final class CapturedState extends ItemStackRenderState {
        private final List<LayerRenderState> captured = new ArrayList<>();

        @Override
        public LayerRenderState newLayer() {
            var layer = super.newLayer();
            this.captured.add(layer);
            return layer;
        }
    }
}
