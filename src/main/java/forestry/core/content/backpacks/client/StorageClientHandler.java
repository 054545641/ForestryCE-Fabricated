package forestry.core.content.backpacks.client;

import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.gui.GuiNaturalistInventory;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.backpacks.features.BackpackMenuTypes;
import forestry.core.content.backpacks.gui.ContainerNaturalistBackpack;
import forestry.core.content.backpacks.gui.GuiBackpack;
import forestry.core.content.backpacks.items.BackpackItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public class StorageClientHandler implements IClientModuleHandler {
	// Standalone models resolve through ModelBakery.MODEL_LISTER ("models/<path>.json"),
	// without the legacy "item/" prefix that vanilla item lookups added in pre-1.21
	// versions. Use the explicit "item/filled_crate" path so the loader finds
	// assets/forestry/models/item/filled_crate.json.

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(StorageClientHandler::registerModelLoaders);
		modBus.addListener(StorageClientHandler::registerItemModels);
		modBus.addListener(StorageClientHandler::onClientSetup);
		modBus.addListener(StorageClientHandler::registerBackpackItemProperties);
	}

	private static void registerBackpackItemProperties(net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(ForestryConstants.forestry("backpack_mode"), BackpackModeProperty.CODEC);
    }


    private static void registerItemModels(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
        event.register(ForestryConstants.forestry("filled_crate"), FilledCrateModel.Item.CODEC);
    }
	private static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
		event.register(ForestryConstants.forestry("filled_crate"), new FilledCrateModel.Loader());
	}


	private static void onClientSetup(RegisterMenuScreensEvent event) {
		event.register(BackpackMenuTypes.BACKPACK.menuType(), GuiBackpack::new);
		event.register(BackpackMenuTypes.NATURALIST_BACKPACK.menuType(), GuiNaturalistInventory<ContainerNaturalistBackpack>::new);
	}
}
