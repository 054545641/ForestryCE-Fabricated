package forestry.core;

import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.features.FluidsItems;
import forestry.core.platform.item.FluidContainerItem;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@ForestryModule
public class ModuleFluids extends BlankForestryModule {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleFluids::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(Capabilities.Fluid.ITEM, (stack, context) -> ((FluidContainerItem) stack.getItem()).createFluidHandler(context), FluidsItems.CONTAINERS.itemArray());
	}

	@Override
	public Identifier getId() {
		return ForestryModuleIds.FLUIDS;
	}
}
