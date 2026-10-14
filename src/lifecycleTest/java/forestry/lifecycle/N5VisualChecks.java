package forestry.lifecycle;

import forestry.api.IForestryApi;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.content.sorting.gui.GuiGeneticFilter;
import forestry.core.platform.gui.widgets.WidgetScrollBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static forestry.lifecycle.N5ClientTest.check;

final class N5VisualChecks {
    static void items(Minecraft client) throws Exception {
        var samples = new ArrayList<ItemStack>();
        BuiltInRegistries.ITEM.stream().filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("forestry"))
            .forEach(item -> samples.add(item.getDefaultInstance()));
        for (ISpeciesType<?, ?> type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
            for (ISpecies<?> species : type.getAllSpecies()) samples.add(species.createStack(type.getDefaultStage()));
        }
        for (String id : List.of("can", "wax_capsule", "refractory_capsule")) {
            var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("forestry:" + id)));
            stack.set(forestry.core.features.CoreDataComponents.FLUID_CONTENT,
                net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000)));
            samples.add(stack);
        }
        var layers = ItemStackRenderState.class.getDeclaredField("layers");
        var count = ItemStackRenderState.class.getDeclaredField("activeLayerCount");
        layers.setAccessible(true); count.setAccessible(true);
        var material = ItemStackRenderState.LayerRenderState.class.getDeclaredField("particleMaterial");
        material.setAccessible(true);
        var state = new ItemStackRenderState();
        int checked = 0;
        for (var stack : samples) for (var context : ItemDisplayContext.values()) {
            client.getItemModelResolver().updateForTopItem(state, stack, context, client.level, client.player, 7);
            check(!state.isEmpty(), "Registered item produces render layers: " + stack + "/" + context);
            var array = (ItemStackRenderState.LayerRenderState[]) layers.get(state);
            for (int i = 0; i < count.getInt(state); i++) {
                var sprite = (net.minecraft.client.resources.model.sprite.Material.Baked) material.get(array[i]);
                check(sprite == null || !sprite.sprite().contents().name().getPath().equals("missingno"), "No missing material: " + stack + "/" + context);
                for (var quad : array[i].prepareQuadList()) check(!quad.materialInfo().sprite().contents().name().getPath().equals("missingno"), "No missing quad texture: " + stack + "/" + context);
            }
            checked++;
        }
        com.mojang.logging.LogUtils.getLogger().info("FORESTRY_N5_ITEMS_READY stacks={} contexts={} renders={}", samples.size(), ItemDisplayContext.values().length, checked);
    }

    static void filter(Minecraft client, GuiGeneticFilter screen) throws Exception {
        var event = new net.minecraft.client.input.MouseButtonEvent(screen.getGuiLeft() + 88, screen.getGuiTop() + 26,
            new net.minecraft.client.input.MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
        check(screen.selection.getLogic() != null, "Species cell opens selection panel");
        var field = GuiGeneticFilter.class.getDeclaredField("scrollBar"); field.setAccessible(true);
        var scroll = (WidgetScrollBar) field.get(screen);
        for (int i = 0; i < 30; i++) check(screen.mouseScrolled(screen.getGuiLeft() + 100, screen.getGuiTop() + 175, 0, -1), "Selection handles native wheel");
        check(scroll.getValue() == 3, "69 bees require seven rows; partial last row stays reachable");
        var searchField = GuiGeneticFilter.class.getDeclaredField("searchField"); searchField.setAccessible(true);
        var search = (net.minecraft.client.gui.components.EditBox) searchField.get(screen);
        screen.setFocused(search); search.setFocused(true);
        screen.charTyped(new net.minecraft.client.input.CharacterEvent('林'));
        check(search.getValue().equals("林") && scroll.getValue() == 0, "Unicode input filters species and resets scroll");
        screen.keyPressed(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_E, 0, 0));
        check(client.gui.screen() == screen, "Inventory key cannot close focused search");
        search.setValue("");
        screen.setFocused(null);
        com.mojang.logging.LogUtils.getLogger().info("FORESTRY_N5_FILTER_READY unicode=true wheel=true partial_row=true focused_keys=true");
    }

    static void naturalist(forestry.core.platform.gui.GuiNaturalistInventory<?> screen) {
        check(screen.mouseScrolled(screen.getGuiLeft() + 20, screen.getGuiTop() + 20, 0, -1), "Naturalist inventory handles wheel");
        var down = new net.minecraft.client.input.MouseButtonEvent(screen.getGuiLeft() + 158, screen.getGuiTop() + 10, new net.minecraft.client.input.MouseButtonInfo(0, 0));
        screen.mouseClicked(down, false);
        var drag = new net.minecraft.client.input.MouseButtonEvent(down.x(), screen.getGuiTop() + 94, down.buttonInfo());
        screen.mouseDragged(drag, 0, 84); screen.mouseReleased(drag);
        check(screen.getMenu().getScrollRow() == 11, "Native drag reaches final naturalist inventory row");
    }

    static void letter(Minecraft client, forestry.mail.gui.LetterScreen screen) throws Exception {
        var field = forestry.mail.gui.LetterScreen.class.getDeclaredField("text"); field.setAccessible(true);
        var text = (net.minecraft.client.gui.components.MultiLineEditBox) field.get(screen);
        screen.setFocused(text); text.setFocused(true);
        for (int ch : "林业 UI".codePoints().toArray()) screen.charTyped(new net.minecraft.client.input.CharacterEvent(ch));
        check(text.getValue().equals("林业 UI"), "Letter native multiline field accepts Unicode");
        screen.setFocused(null);
    }
}
