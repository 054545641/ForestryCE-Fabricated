package forestry.core.platform.client.render;
import forestry.api.ForestryConstants;
import forestry.core.features.CoreTiles;
import forestry.core.content.energy.features.EnergyTiles;
import forestry.core.content.machines.features.FactoryTiles;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
public final class FabricPlatformModels {
    public static void initialize() {
        BlockEntityRendererRegistry.register(CoreTiles.ANALYZER.tileType(), forestry.core.platform.render.RenderAnalyzer::new);
        BlockEntityRendererRegistry.register(CoreTiles.ESCRITOIRE.tileType(), forestry.core.platform.render.RenderEscritoire::new);
        net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.ID_MAPPER.put(ForestryConstants.forestry("backpack_mode"), BackpackModeProperty.CODEC);
        net.minecraft.client.renderer.special.SpecialModelRenderers.ID_MAPPER.put(ForestryConstants.forestry("machine"), MachineSpecialRenderer.Unbaked.CODEC);
        net.minecraft.client.renderer.item.ItemModels.ID_MAPPER.put(ForestryConstants.forestry("fluid_container"), FluidContainerModel.Unbaked.MAP_CODEC);
        net.minecraft.client.renderer.item.ItemModels.ID_MAPPER.put(ForestryConstants.forestry("filled_crate"), FilledCrateModel.Item.CODEC);
        ModelLayerRegistry.registerModelLayer(ForestryModelLayers.ENGINE_LAYER, EngineGeometry::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(ForestryModelLayers.MILL_LAYER, MillGeometry::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(ForestryModelLayers.NATURALIST_CHEST_LAYER, NaturalistChestGeometry::createBodyLayer);
        BlockEntityRendererRegistry.register(EnergyTiles.PEAT_ENGINE.tileType(), context -> new RenderEngine<>(context, "textures/block/engine_copper_"));
        BlockEntityRendererRegistry.register(EnergyTiles.BIOGAS_ENGINE.tileType(), context -> new RenderEngine<>(context, "textures/block/engine_bronze_"));
        BlockEntityRendererRegistry.register(EnergyTiles.CLOCKWORK_ENGINE.tileType(), context -> new RenderEngine<>(context, "textures/block/engine_clock_"));
        BlockEntityRendererRegistry.register(EnergyTiles.COMBUSTION_ENGINE.tileType(), context -> new RenderEngine<>(context, "textures/block/engine_iron_"));
        BlockEntityRendererRegistry.register(EnergyTiles.SOLAR_ENGINE.tileType(), context -> new RenderEngine<>(context, "textures/block/engine_tin_"));
        BlockEntityRendererRegistry.register(FactoryTiles.RAINMAKER.tileType(), context -> new RenderMill<>(context, "textures/block/rainmaker_"));
        BlockEntityRendererRegistry.register(CoreTiles.APIARIST_CHEST.tileType(), context -> new RenderNaturalistChest<>(context, "apiaristchest"));
        BlockEntityRendererRegistry.register(CoreTiles.ARBORIST_CHEST.tileType(), context -> new RenderNaturalistChest<>(context, "arbchest"));
        BlockEntityRendererRegistry.register(CoreTiles.LEPIDOPTERIST_CHEST.tileType(), context -> new RenderNaturalistChest<>(context, "lepichest"));
    }
}
