package forestry.api.modules;

import forestry.api.client.IClientModuleHandler;
import forestry.api.client.plugin.IClientRegistration;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * IF YOU WANT TO ADD BEE SPECIES, FORESTRY COMPATIBILITY, ETC. USE A {@link forestry.api.plugin.IForestryPlugin}.
 * <p>
 * The entry point for a Forestry module. Your mod probably doesn't need this, but it's here if you want to use it.
 * Must be annotated by {@link ForestryModule} to be loaded and must have an empty constructor.
 */
public interface IForestryModule {
	/**
	 * @return The unique identifier for this module. The namespace should be the modid of the mod adding this module.
	 */
	Identifier getId();

	/**
	 * @return A list of identifiers of the modules this module requires in order to load (Apiculture, Mail, etc.)
	 */
	default List<Identifier> getModuleDependencies() {
		return List.of();
	}

	/**
	 * @return A list of identifiers of the mods this module requires in order to load (IC2, BuildCraft, etc.)
	 */
	default List<String> getModDependencies() {
		return List.of();
	}

	/**
	 * Called during mod construction, allowing modules to subscribe to mod bus events using their mod's event bus.
	 * For client-only events, use {@link IForestryModule#registerClientHandler} and {@link IClientModuleHandler#registerEvents}.
	 *
	 * @param modBus The mod-specific event bus for the mod found from the namespace of {@link #getId()}.
	 */
	default void registerEvents(IEventBus modBus) {
	}

	/**
	 * Runs at mod construction on the logical client, after {@link #registerEvents}.
	 */
	default void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
	}

	/**
	 * Note: this is generally not recommended to use.
	 * Called during Forestry's common phase. Initialize any static API.
	 */
	@Deprecated
	default void setupApi() {
	}

	default void registerPackets(IPacketRegistry registry) {
	}

	/**
	 * Called when the server gathers its datapack reload listeners. Modules add their own data loaders
	 * here. Called in module load order, so a module's data may depend on data loaded by any module it
	 * names in {@link #getModuleDependencies}.
	 *
	 * @param event The reload listener registration event
	 */
	default void registerReloadListeners(AddServerReloadListenersEvent event) {
	}

	/**
	 * Send datapack data to clients here. Called after taxa and flower types are synced. <p>
	 *
	 * Data-driven parts of the genetics system are sent via this method. This includes bee effects, bee species,
	 * tree species, and butterfly species. It does not include code-only parts of the genetic system, such as tree
	 * effects or fruit types.
	 *
	 * @param event The datapack sync event, fired when a player joins the server or /reload is ran, before tags/recipes
	 */
	default void sendDatapackData(OnDatapackSyncEvent event) {
	}

	/**
	 * Called after item registration, in module load order, before datapack loading.
	 * Register API types here without constructing ItemStacks; default item components are not bound yet.
	 */
	default void applyPluginRegistration() {
	}

	/**
	 * Rebuilds data that needs bound item components, after server datapack loading or remote-client sync.
	 * Called in module load order. Replace previous data rather than appending: this runs again on reload.
	 */
	default void applyLoadedDataRegistration() {
	}

	/**
	 * Called during client plugin registration after {@link #applyPluginRegistration}. Modules build
	 * their client manager from the assembled registration and install it here.
	 *
	 * @param registration The completed client registration
	 */
	default void applyClientPluginRegistration(IClientRegistration registration) {
	}

	/**
	 * @return If this module is a "core" module, a dependency of all other modules added by this mod. Loads before other modules.
	 */
	default boolean isCore() {
		return false;
	}
}
