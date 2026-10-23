package forestry.arboriculture.client;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.arboriculture.leaves.DecorativeLeavesBlock;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.features.ArboricultureEntities;
import forestry.arboriculture.features.ArboricultureTiles;
import forestry.arboriculture.models.*;
import forestry.core.platform.models.ClientManager;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.IEventBus;

public class ArboricultureClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ArboricultureClientHandler::registerBlockColors);
		modBus.addListener(ArboricultureClientHandler::registerModelLoaders);
		modBus.addListener(ArboricultureClientHandler::registerItemModels);
		modBus.addListener(ArboricultureClientHandler::registerBlockModels);
		modBus.addListener(ArboricultureClientHandler::registerEntityRenderers);
		modBus.addListener(ArboricultureClientHandler::registerModelLayers);
		modBus.addListener((net.neoforged.neoforge.client.event.AddClientReloadListenersEvent event) ->
			SignEditorTexture.register(event, java.util.Arrays.stream(forestry.arboriculture.wood.ForestryWoodType.VALUES).map(type -> type.getSerializedName()).toList()));
	}


	private static void registerModelLoaders(ModelEvent.RegisterLoaders event) {

		// Registered here and not in FMLClientSetupEvent: this event is posted at the top of ModelManager.reload,
		// so it is ordered before the ModifyBakingResult that reads these models back. Client setup runs on its own
		// thread during the same reload, and losing that race leaves the leaf blocks on the particle-only models
		// they fall back to, which bake to no quads at all - a see-through block.
		ClientManager clientManager = ClientManager.INSTANCE;
		clientManager.registerModel(new LeavesModel(), ArboricultureBlocks.LEAVES);
		clientManager.registerModel(new DecorativeLeavesModel<>(DecorativeLeavesBlock.class), ArboricultureBlocks.LEAVES_DECORATIVE);
		clientManager.registerModel(new DefaultLeavesModel(), ArboricultureBlocks.LEAVES_DEFAULT);
		clientManager.registerModel(new DefaultFruitLeavesModel(), ArboricultureBlocks.LEAVES_DEFAULT_FRUIT);
	}

	private static void registerItemModels(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
        event.register(ForestryConstants.forestry("sapling_ge"), SaplingModel.Item.CODEC);
    }
    private static void registerBlockModels(net.neoforged.neoforge.client.event.RegisterBlockStateModels event) {
        event.registerModel(ForestryConstants.forestry("sapling_ge"), SaplingModel.Block.CODEC);
    }
    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ArboricultureEntities.BOAT.entityType(), ctx -> new ForestryBoatRenderer(ctx, false));
		event.registerEntityRenderer(ArboricultureEntities.CHEST_BOAT.entityType(), ctx -> new ForestryBoatRenderer(ctx, true));
		event.registerBlockEntityRenderer(ArboricultureTiles.SIGN.tileType(), StandingSignRenderer::new);
		// HANGING_SIGN BE renderer is provided by vanilla; Forestry's hanging-sign blocks reuse
		// vanilla's BlockEntityType.HANGING_SIGN via BlockEntityTypeAddBlocksEvent.
	}

	private static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(BoatRenderGeometry.BOAT, BoatModel::createBoatModel);
		event.registerLayerDefinition(BoatRenderGeometry.CHEST_BOAT, BoatModel::createChestBoatModel);
		// Sign boards now come from block models; the vanilla block entity renderers submit only their text.
	}

	private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ArboricultureBlocks.LEAVES.block());
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ArboricultureBlocks.LEAVES_DEFAULT.blockArray());
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.blockArray());
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ArboricultureBlocks.LEAVES_DECORATIVE.blockArray());
	}


}
