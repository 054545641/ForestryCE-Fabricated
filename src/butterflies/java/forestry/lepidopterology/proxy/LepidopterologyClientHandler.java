package forestry.lepidopterology.proxy;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import forestry.core.platform.models.ClientManager;
import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.render.ForestryModelLayers;
import forestry.lepidopterology.features.LepidopterologyEntities;
import forestry.lepidopterology.features.LepidopterologyItems;
import forestry.lepidopterology.butterflies.ItemButterflyGE;
import forestry.lepidopterology.client.ButterflyEntityRenderer;
import forestry.lepidopterology.client.ButterflyItemModel;
import forestry.lepidopterology.client.ButterflyModel;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.IEventBus;

public class LepidopterologyClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(LepidopterologyClientHandler::setupRenderers);
		modBus.addListener(LepidopterologyClientHandler::setupLayers);
		modBus.addListener(LepidopterologyClientHandler::registerModelLoaders);
		modBus.addListener(LepidopterologyClientHandler::registerItemProperties);
	}

    private static void registerItemProperties(net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(ForestryConstants.forestry("age"), forestry.lepidopterology.client.CocoonAgeProperty.CODEC);
    }

	public static void setupRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(LepidopterologyEntities.BUTTERFLY.entityType(), ButterflyEntityRenderer::new);
	}

	public static void setupLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ForestryModelLayers.BUTTERFLY_LAYER, ButterflyModel::createLayer);
	}

	public static void registerModelLoaders(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
		event.register(ForestryConstants.forestry("butterfly_ge"), ButterflyItemModel.CODEC);
	}


}
