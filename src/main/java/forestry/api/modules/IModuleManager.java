package forestry.api.modules;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * The module manager of Forestry.
 * Modules are a way to organize related features and compatibility into separate parts of the mod.
 * Register your {@link IForestryModule} classes with the {@link ForestryModule} annotation.
 */
public interface IModuleManager {
	/**
	 * @return List of loaded modules in LOAD order, where dependency modules come before dependent modules.
	 * Modules with the same dependencies are not guaranteed to be in the same order between runs.
	 */
	Collection<IForestryModule> getLoadedModules();

	/**
	 * @return {@code true} if a module with the given ID is loaded,
	 * or {@code false} if the module does not exist or is missing module dependencies.
	 */
	boolean isModuleLoaded(Identifier id);

	List<IForestryModule> getModulesForMod(String modId);
}
