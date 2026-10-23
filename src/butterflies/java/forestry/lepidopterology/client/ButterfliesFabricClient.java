package forestry.lepidopterology.client;

import forestry.api.ForestryConstants;
import forestry.lepidopterology.features.LepidopterologyEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ButterfliesFabricClient implements ClientModInitializer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ForestryConstants.forestry("butterfly"), "main");
    @Override public void onInitializeClient() {
        forestry.lepidopterology.butterflies.genetics.LepidopterologyReloadHandler.installClientRefresh(() -> {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) for (var entity : level.entitiesForRendering()) if (entity instanceof forestry.lepidopterology.entities.EntityButterfly butterfly) butterfly.refreshClientSpecies();
        });
        ModelLayerRegistry.registerModelLayer(LAYER, ButterflyModel::createLayer);
        EntityRendererRegistry.register(LepidopterologyEntities.BUTTERFLY.entityType(), ButterflyEntityRenderer::new);
        net.minecraft.client.renderer.item.ItemModels.ID_MAPPER.put(ForestryConstants.forestry("butterfly_ge"), ButterflyItemModel.CODEC);
        net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.ID_MAPPER.put(ForestryConstants.forestry("age"), CocoonAgeProperty.CODEC);
    }
}
