package forestry.core.platform.client;

import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.api.client.IForestryClientApi;
import forestry.apiimpl.client.ForestryClientApiImpl;
import forestry.apiimpl.plugin.PluginManager;
import forestry.core.content.analyzer.*;
import forestry.core.content.burnbarrel.GuiBurnBarrel;
import forestry.core.content.energy.features.EnergyTiles;
import forestry.core.content.escritoire.*;
import forestry.core.content.machines.RenderMill;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.engine.circuits.GuiSolderingIron;
import forestry.core.engine.genetics.GeneticsReloadHandler;
import forestry.core.features.*;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.gui.*;
import forestry.core.platform.models.ClientManager;
import forestry.core.platform.models.FluidContainerModel;
import forestry.core.platform.models.ModelBlockCached;
import forestry.core.platform.particles.CoreParticles;
import forestry.core.platform.registration.FeatureFluid;
import forestry.core.platform.render.*;
import forestry.modules.ModuleUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.common.NeoForge;

public class CoreClientHandler implements IClientModuleHandler {


	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(CoreClientHandler::registerMenuScreens);
		modBus.addListener(CoreClientHandler::registerModelLoaders);
		modBus.addListener(CoreClientHandler::registerItemModels);
		modBus.addListener(CoreClientHandler::bakeModels);
		modBus.addListener(CoreClientHandler::setupLayers);
		modBus.addListener(CoreClientHandler::clientSetupRenderers);
		modBus.addListener(CoreClientHandler::registerReloadListeners);
		modBus.addListener(forestry.core.platform.render.ForestrySpriteUploader::registerAtlas);
		modBus.addListener(CoreClientHandler::registerBlockColors);
		modBus.addListener(CoreClientHandler::registerItemColors);
		modBus.addListener(CoreClientHandler::registerParticleFactory);
		modBus.addListener(CoreClientHandler::registerSpecialModels);
		modBus.addListener(CoreClientHandler::registerFluidModels);
		modBus.addListener(NaturalistEyeRenderer::registerPipeline);
		NeoForge.EVENT_BUS.addListener(NaturalistEyeRenderer::extract);
		NeoForge.EVENT_BUS.addListener(NaturalistEyeRenderer::submit);
		// rebuild before recipe viewers consume the new map
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CoreClientHandler::onRecipesReceived);
		NeoForge.EVENT_BUS.addListener(CoreClientHandler::onLoggingOut);

		ModuleUtil.getModBus(ForestryConstants.MOD_ID).addListener(EventPriority.HIGHEST, ((ForestryClientApiImpl) IForestryClientApi.INSTANCE)::initializeTextureManager);
	}


	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(CoreMenuTypes.ALYZER.menuType(), PortableAnalyzerScreen::new);
		event.register(CoreMenuTypes.ANALYZER.menuType(), GuiAnalyzer::new);
		event.register(CoreMenuTypes.NATURALIST_INVENTORY.menuType(), GuiNaturalistInventory<ContainerNaturalistInventory>::new);
		event.register(CoreMenuTypes.ESCRITOIRE.menuType(), GuiEscritoire::new);
		event.register(CoreMenuTypes.BURN_BARREL.menuType(), GuiBurnBarrel::new);
		event.register(CoreMenuTypes.SOLDERING_IRON.menuType(), GuiSolderingIron::new);
	}

	private static void registerModelLoaders(ModelEvent.RegisterLoaders event) {


		// Client model registration depends on TreeManager / BeeManager / etc. being
		// initialized. RegisterLoaders fires before FMLCommonSetupEvent, so make
		// sure the API is bootstrapped before plugin client registration runs.
		forestry.core.ModuleCore.ensureApiInitialized();
		PluginManager.registerClient();
	}

	private static void registerItemModels(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
		event.register(ForestryConstants.forestry("fluid_container"), FluidContainerModel.Unbaked.MAP_CODEC);
	}

	private static void bakeModels(ModelEvent.ModifyBakingResult event) {
		ClientManager.INSTANCE.onBakeModels(event);
	}

	private static void setupLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(ForestryModelLayers.NATURALIST_CHEST_LAYER, RenderNaturalistChest::createBodyLayer);
		event.registerLayerDefinition(ForestryModelLayers.MILL_LAYER, RenderMill::createBodyLayer);
		event.registerLayerDefinition(ForestryModelLayers.ENGINE_LAYER, RenderEngine::createBodyLayer);
	}

	private static void clientSetupRenderers(EntityRenderersEvent.RegisterRenderers event) {
		// Core
		event.registerBlockEntityRenderer(CoreTiles.ANALYZER.tileType(), RenderAnalyzer::new);
		event.registerBlockEntityRenderer(CoreTiles.ESCRITOIRE.tileType(), RenderEscritoire::new);
		// Apiculture/Arboriculture/Lepidopterology
		event.registerBlockEntityRenderer(CoreTiles.APIARIST_CHEST.tileType(), ctx -> new RenderNaturalistChest(ctx, "apiaristchest"));
		event.registerBlockEntityRenderer(CoreTiles.ARBORIST_CHEST.tileType(), ctx -> new RenderNaturalistChest(ctx, "arbchest"));
		event.registerBlockEntityRenderer(CoreTiles.LEPIDOPTERIST_CHEST.tileType(), ctx -> new RenderNaturalistChest(ctx, "lepichest"));
		// Engine
		event.registerBlockEntityRenderer(EnergyTiles.CLOCKWORK_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_clock_"));
		event.registerBlockEntityRenderer(EnergyTiles.BIOGAS_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_bronze_"));
		event.registerBlockEntityRenderer(EnergyTiles.PEAT_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_copper_"));
		event.registerBlockEntityRenderer(EnergyTiles.COMBUSTION_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_iron_"));
		event.registerBlockEntityRenderer(EnergyTiles.SOLAR_ENGINE.tileType(), ctx -> new RenderEngine(ctx, Constants.TEXTURE_PATH_BLOCK + "/engine_tin_"));
		// Factory
		event.registerBlockEntityRenderer(FactoryTiles.RAINMAKER.tileType(), ctx -> new RenderMill(ctx, Constants.TEXTURE_PATH_BLOCK + "/rainmaker_"));
	}

	private static void registerReloadListeners(AddClientReloadListenersEvent event) {
		event.addListener(ForestryConstants.forestry("colours"), ColourProperties.INSTANCE);

		event.addListener(ForestryConstants.forestry("model_cache"), (sharedState, backgroundExecutor, prepBarrier, gameExecutor) -> {
			return prepBarrier.wait(Unit.INSTANCE).thenRunAsync(ModelBlockCached::clear, gameExecutor);
		});
	}

	private static void registerParticleFactory(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(CoreParticles.REFRACTORY_WAX.get(), RefractoryWaxParticle::new);
	}

	private static void registerSpecialModels(RegisterSpecialModelRendererEvent event) {
		event.register(ForestryConstants.forestry("machine"), MachineSpecialRenderer.Unbaked.CODEC);
	}

	private static void registerFluidModels(RegisterFluidModelsEvent event) {
		for (ForestryFluids fluid : ForestryFluids.values()) {
			if (fluid.getFluid().getFluidType() instanceof FeatureFluid.ForestryFluidType type) {
				var still = new Material(type.getStillTexture(), true);
				var flowing = new Material(type.getFlowingTexture(), true);
				// textures already contain the fluid color
				var model = new FluidModel.Unbaked(still, flowing, null,
			FluidTintSources.constant(0xFFFFFFFF));
				event.register(model, fluid.getFluid(), fluid.getFlowing());
			}
		}
	}

	private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
		// Turf wears the grass texture and tints it by biome, the same way vanilla tints its grass block.
		// Deviation from 1.20.1: that tree named the two blocks inside ClientManager's shared block colour,
		// which every other forestry block reaches through IColoredBlock. Neither turf block is one, so they
		// get vanilla's own lambda here instead
		event.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grass()), CoreBlocks.TURF_BLOCK.block(), CoreBlocks.TURF.block());

		// Packaging machine tank slices, tinted with the fluid colour of whatever they hold
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, FactoryBlocks.PLAIN.blockArray());
	}

	private static void registerItemColors(RegisterColorHandlersEvent.ItemTintSources event) {
		event.register(ForestryConstants.forestry("item"), forestry.core.platform.models.ForestryItemTintSource.CODEC);
	}

	private static void onRecipesReceived(RecipesReceivedEvent event) {
		ClientsideCode.setRecipes(event.getRecipeMap());
		forestry.core.platform.recipes.RecipeManagers.invalidateCaches();
		// integrated servers share species identity and already rebuilt their mutations
		if (!Minecraft.getInstance().hasSingleplayerServer()) {
			GeneticsReloadHandler.rebuildMutations(event.getRecipeMap());
		}
	}

	private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		forestry.core.engine.genetics.root.BreedingTrackerManager.INSTANCE.clearClientCache();
		ClientsideCode.setRecipes(net.minecraft.world.item.crafting.RecipeMap.EMPTY);
		forestry.core.platform.recipes.RecipeManagers.invalidateCaches();
	}

}
