package forestry.core.platform.compat;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IForestryModule;
import forestry.core.platform.compat.curios.client.CuriosClientHandler;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

import java.util.function.Consumer;

@ForestryModule
public class ModuleCurios implements IForestryModule {
	@Override
	public Identifier getId() {
		return ForestryModuleIds.CURIOS;
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		if (ModList.get().isLoaded("curios")) {
			registrar.accept(new CuriosClientHandler());
		}
	}
}
