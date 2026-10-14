package forestry.core;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import forestry.api.ForestryCapabilities;
import forestry.api.ForestryConstants;
import forestry.api.ForestryRegistries;
import forestry.api.IForestryApi;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IForestryModule;
import forestry.api.modules.IPacketRegistry;
import forestry.core.engine.genetics.FlowerTypeManager;
import forestry.core.engine.genetics.TaxonManager;
import forestry.apiimpl.plugin.PluginManager;
import forestry.core.platform.block.TileStreamUpdateTracker;
import forestry.core.platform.client.CoreClientHandler;
import forestry.core.manual.IManualAccess;
import forestry.core.manual.ManualAccess;
import forestry.core.engine.climate.ForestryClimateManager;
import forestry.core.platform.commands.DiagnosticsCommand;
import forestry.core.platform.commands.DumpCommand;
import forestry.core.features.CoreItems;
import forestry.core.features.CoreTiles;
import forestry.core.engine.genetics.GeneticsReloadHandler;
import forestry.core.content.tools.ItemPipette;
import forestry.core.content.tools.ItemSpectacles;
import forestry.core.platform.loot.ConditionLootModifier;
import forestry.core.platform.network.PacketIdClient;
import forestry.core.platform.network.PacketIdServer;
import forestry.core.platform.network.packets.*;
import forestry.core.platform.owner.GameProfileDataSerializer;
import forestry.core.platform.recipes.RecipeManagers;
import forestry.core.platform.util.NetworkUtil;
import forestry.modules.BlankForestryModule;
import forestry.modules.ForestryModuleManager;
import forestry.modules.ModuleUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.DefaultDataComponentsBoundEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Set;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import forestry.core.platform.PickupHandlerCore;

@ForestryModule
public class ModuleCore extends BlankForestryModule {
	private static volatile boolean apiInitialized = false;

	@Override
	public Identifier getId() {
		return ForestryModuleIds.CORE;
	}

	private static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS =
		DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, ForestryConstants.MOD_ID);

	static {
		ENTITY_DATA_SERIALIZERS.register("game_profile", () -> GameProfileDataSerializer.INSTANCE);
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		ENTITY_DATA_SERIALIZERS.register(modBus);
		modBus.addListener(ModuleCore::onCommonSetup);
		modBus.addListener(ModuleCore::registerCapabilities);
		modBus.addListener(ModuleCore::registerGlobalLootModifiers);
		modBus.addListener(ModuleCore::registerRecipeIngredients);
		modBus.addListener(ModuleCore::registerForestryRegistries);
		modBus.addListener((GatherDataEvent.Client event) -> ensureApiInitialized());
		modBus.addListener((GatherDataEvent.Server event) -> ensureApiInitialized());

		ModuleUtil.loadFeatureProviders();
		NeoForge.EVENT_BUS.addListener(ModuleCore::onItemPickup);
		NeoForge.EVENT_BUS.addListener(ModuleCore::onLevelTick);
		NeoForge.EVENT_BUS.addListener(ModuleCore::onTagsUpdated);
		NeoForge.EVENT_BUS.addListener(ModuleCore::onDataComponentsBound);
		NeoForge.EVENT_BUS.addListener(ModuleCore::onAddReloadListeners);
		NeoForge.EVENT_BUS.addListener(ModuleCore::registerCommands);
		NeoForge.EVENT_BUS.addListener(ModuleCore::onDatapackSync);
	}

	private static void onCommonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(ModuleCore::ensureApiInitialized);

		// the adapter may be absent even when the upstream mod is installed
		if (ModList.get().isLoaded("patchouli")) {
			ServiceLoader.load(IManualAccess.class).findFirst().ifPresent(ManualAccess::install);
		}
	}

	private static void registerForestryRegistries(NewRegistryEvent event) {
		event.register(ForestryRegistries.CIRCUIT);
		event.register(ForestryRegistries.POSTAL_CARRIER);
		event.register(ForestryRegistries.SPECIES_TYPE);
		event.register(ForestryRegistries.BEE_EFFECT_TYPE);
		event.register(ForestryRegistries.BEE_JUBILANCE_TYPE);
		event.register(ForestryRegistries.MUTATION_CONDITION_TYPE);
		event.register(ForestryRegistries.PRODUCT_TYPE);
		event.register(ForestryRegistries.FLUID_PRODUCT_TYPE);
		event.register(ForestryRegistries.FLOWER_TYPE_SERIALIZER);
	}

	// datagen skips common setup and may initialize the API first
	public static synchronized void ensureApiInitialized() {
		if (apiInitialized) {
			return;
		}
		postItemRegistry();
		((ForestryModuleManager) IForestryApi.INSTANCE.getModuleManager()).setupApi();
		apiInitialized = true;
	}

	private static void onDataComponentsBound(DefaultDataComponentsBoundEvent event) {
		if (event.shouldUpdateStaticData()) refreshDataRegistrations();
	}

	/** Rebuild after components bind, and after a remote client's tree species arrive. */
	public static void refreshDataRegistrations() {
		ensureApiInitialized();
		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getLoadedModules()) {
			module.applyLoadedDataRegistration();
		}
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(ForestryCapabilities.SPECTACLE_VISION, (stack, context) -> ItemSpectacles.SPECTACLE_VISION, CoreItems.SPECTACLES.item());
		event.registerItem(Capabilities.Fluid.ITEM, (stack, context) -> ((ItemPipette) stack.getItem()).createFluidHandler(context), CoreItems.PIPETTE.item());
		event.registerBlockEntity(Capabilities.Item.BLOCK, CoreTiles.ANALYZER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, CoreTiles.ESCRITOIRE.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, CoreTiles.APIARIST_CHEST.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, CoreTiles.ARBORIST_CHEST.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, CoreTiles.LEPIDOPTERIST_CHEST.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, CoreTiles.ANALYZER.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, CoreTiles.ANALYZER.tileType(), (tile, side) -> tile.getTankManager());
	}

	private static void registerRecipeIngredients(RegisterEvent event) {
		event.register(NeoForgeRegistries.Keys.INGREDIENT_TYPES, helper -> helper.register(ForestryConstants.forestry("fluid_container"),
			forestry.core.platform.recipes.FluidContainerIngredient.TYPE));
		event.register(Registries.SLOT_DISPLAY, helper -> helper.register(ForestryConstants.forestry("fluid_container"),
			forestry.core.platform.recipes.FluidContainerIngredient.Display.TYPE));
	}

	private static void registerGlobalLootModifiers(RegisterEvent event) {
		event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper -> {
			helper.register(ForestryConstants.forestry("condition_modifier"), ConditionLootModifier.CODEC);
		});
	}

	private static void postItemRegistry() {
		// Modules load in dependency order (see ForestryModuleManager). A module that supplies one of
		// the api managers installs it here, over the no-op base put there at construction.
		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getLoadedModules()) {
			module.applyPluginRegistration();
		}
	}

	private static void onItemPickup(ItemEntityPickupEvent.Post event) {
		PickupHandlerCore.onItemPickup(event.getPlayer(), event.getItemEntity());
	}

	private static void onLevelTick(LevelTickEvent.Post event) {
		var server = event.getLevel().getServer();
		if (server != null) {
			TileStreamUpdateTracker.syncVisualUpdates(server);
		}
	}

	private static void onTagsUpdated(TagsUpdatedEvent event) {
		if (event.shouldUpdateStaticData()) {
			event.getRegistries().lookup(Registries.BIOME).ifPresent(registry -> ((ForestryClimateManager) IForestryApi.INSTANCE.getClimateManager()).onBiomesReloaded(registry));
		}
	}

	private static void onAddReloadListeners(AddServerReloadListenersEvent event) {
		Set<Identifier> existing = Set.copyOf(event.getRegistry().keySet());
		event.addListener(ForestryConstants.forestry("recipe_cache"), (sharedState, backgroundExecutor, barrier, gameExecutor) ->
			barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
				RecipeManagers.invalidateCaches();
				NetworkUtil.sendToAllPlayers(new RecipeCachePacket());
			}, gameExecutor));

		// taxa and flower types must exist before any species is projected
		event.addListener(ForestryConstants.forestry("taxa"), TaxonManager.INSTANCE);
		event.addListener(ForestryConstants.forestry("flower_types"), FlowerTypeManager.INSTANCE);
		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getLoadedModules()) {
			module.registerReloadListeners(event);
		}

		RecipeManager recipeManager = event.getServerResources().getRecipeManager();
		event.addListener(ForestryConstants.forestry("mutations"), (sharedState, backgroundExecutor, barrier, gameExecutor) ->
			barrier.wait(Unit.INSTANCE).thenRunAsync(() -> GeneticsReloadHandler.rebuildMutations(recipeManager), gameExecutor));

		List<Identifier> order = event.getRegistry().keySet().stream().filter(id -> !existing.contains(id)).toList();
		for (int i = 1; i < order.size(); i++) {
			event.addDependency(order.get(i - 1), order.get(i));
		}
	}

	private static void onDatapackSync(OnDatapackSyncEvent event) {
		// NeoForge sends requested recipes after species packets and tag synchronization
		event.sendRecipes(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
		forestry.core.platform.registration.ModFeatureRegistry.getRegistries().values().forEach(mod ->
			mod.getModules().values().forEach(module -> module.getFeatures().forEach(feature -> {
				if (feature instanceof forestry.core.platform.registration.FeatureRecipeType<?> recipe) {
					event.sendRecipes(recipe.type());
				}
			})));

		TaxonSyncPacket taxa = new TaxonSyncPacket(TaxonManager.INSTANCE.getDefinitions());
		FlowerTypeSyncPacket flowerTypes = new FlowerTypeSyncPacket(FlowerTypeManager.INSTANCE.getDefinitions());

		// send taxa and flower type datapack data to each client, before species
		event.getRelevantPlayers().forEach(player -> {
			NetworkUtil.sendToPlayer(taxa, player);
			NetworkUtil.sendToPlayer(flowerTypes, player);
		});

		// send necessary data from each module (currently, this is where each module sends its respective bee/tree/butterfly species)
		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getLoadedModules()) {
			module.sendDatapackData(event);
		}
	}

	private static void registerCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> forestryCommand = LiteralArgumentBuilder.literal("forestry");

		forestryCommand.then(DiagnosticsCommand.register());
		forestryCommand.then(DumpCommand.register());
		forestryCommand.then(forestry.core.platform.commands.MultiblockDebugCommand.register());

		for (IForestryModule module : IForestryApi.INSTANCE.getModuleManager().getModulesForMod(ForestryConstants.MOD_ID)) {
			if (module instanceof BlankForestryModule forestryModule) {
				forestryModule.addToRootCommand(forestryCommand);
			}
		}

		event.getDispatcher().register(forestryCommand);
	}

	@Override
	public void applyPluginRegistration() {
		PluginManager.registerGenetics();
		PluginManager.registerPollen();
	}

	@Override
	public void applyLoadedDataRegistration() {
		PluginManager.registerCircuits();
	}

	@Override
	public boolean isCore() {
		return true;
	}

	@Override
	public List<Identifier> getModuleDependencies() {
		return List.of();
	}

	@Override
	public void registerPackets(IPacketRegistry registry) {
		registry.serverbound(PacketIdServer.GUI_SELECTION_REQUEST, PacketGuiSelectRequest::encode, PacketGuiSelectRequest::decode, PacketGuiSelectRequest::handle);
		registry.serverbound(PacketIdServer.PIPETTE_CLICK, PacketPipetteClick::encode, PacketPipetteClick::decode, PacketPipetteClick::handle);
		registry.serverbound(PacketIdServer.CHIPSET_CLICK, PacketChipsetClick::encode, PacketChipsetClick::decode, PacketChipsetClick::handle);
		registry.serverbound(PacketIdServer.SOLDERING_IRON_CLICK, PacketSolderingIronClick::encode, PacketSolderingIronClick::decode, PacketSolderingIronClick::handle);

		registry.clientbound(PacketIdClient.ERROR_UPDATE, PacketErrorUpdate::encode, PacketErrorUpdate::decode, PacketErrorUpdate::handle);
		registry.clientbound(PacketIdClient.GUI_UPDATE, PacketGuiStream::encode, PacketGuiStream::decode, PacketGuiStream::handle);
		registry.clientbound(PacketIdClient.GUI_LAYOUT_SELECT, PacketGuiLayoutSelect::encode, PacketGuiLayoutSelect::decode, PacketGuiLayoutSelect::handle);
		registry.clientbound(PacketIdClient.GUI_ENERGY, PacketGuiEnergy::encode, PacketGuiEnergy::decode, PacketGuiEnergy::handle);
		registry.clientbound(PacketIdClient.SOCKET_UPDATE, PacketSocketUpdate::encode, PacketSocketUpdate::decode, PacketSocketUpdate::handle);
		registry.clientbound(PacketIdClient.TILE_FORESTRY_UPDATE, PacketTileStream::encode, PacketTileStream::decode, PacketTileStream::handle);
		registry.clientbound(PacketIdClient.TILE_FORESTRY_ACTIVE, PacketActiveUpdate::encode, PacketActiveUpdate::decode, PacketActiveUpdate::handle);
		registry.clientbound(PacketIdClient.ITEMSTACK_DISPLAY, PacketItemStackDisplay::encode, PacketItemStackDisplay::decode, PacketItemStackDisplay::handle);
		registry.clientbound(PacketIdClient.GENOME_TRACKER_UPDATE, PacketGenomeTrackerSync::encode, PacketGenomeTrackerSync::decode, PacketGenomeTrackerSync::handle);
		registry.clientbound(PacketIdClient.TANK_LEVEL_UPDATE, PacketTankLevelUpdate::encode, PacketTankLevelUpdate::decode, PacketTankLevelUpdate::handle);
		registry.clientbound(PacketIdClient.RECIPE_CACHE, RecipeCachePacket::encode, RecipeCachePacket::decode, RecipeCachePacket::handle);
		registry.clientbound(PacketIdClient.REFRACTORY_WAX_ON, PacketRefractoryWax::encode, PacketRefractoryWax::decode, PacketRefractoryWax::handle);
		registry.clientbound(PacketIdClient.TAXON_SYNC, TaxonSyncPacket::encode, TaxonSyncPacket::decode, TaxonSyncPacket::handle);
		registry.clientbound(PacketIdClient.FLOWER_TYPE_SYNC, FlowerTypeSyncPacket::encode, FlowerTypeSyncPacket::decode, FlowerTypeSyncPacket::handle);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new CoreClientHandler());
	}
}
