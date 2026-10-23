package forestry.core.content.energy;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.energy.client.EnergyClientHandler;
import forestry.core.content.energy.features.EnergyTiles;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Consumer;
import forestry.api.ForestryDataMaps;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@ForestryModule
public class ModuleEnergy extends BlankForestryModule {
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, EnergyTiles.PEAT_ENGINE.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, EnergyTiles.BIOGAS_ENGINE.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, EnergyTiles.COMBUSTION_ENGINE.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyTiles.CLOCKWORK_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyTiles.BIOGAS_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyTiles.PEAT_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		// Deviation from 1.20.1: capabilities are registered here instead of overriding getCapability on the tile.
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyTiles.SOLAR_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, EnergyTiles.COMBUSTION_ENGINE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, EnergyTiles.BIOGAS_ENGINE.tileType(), (tile, side) -> tile.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, EnergyTiles.COMBUSTION_ENGINE.tileType(), (tile, side) -> tile.getFluidHandler(side));
	}

	@Override
	public Identifier getId() {
		return ForestryModuleIds.ENERGY;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleEnergy::registerCapabilities);
		modBus.addListener(ModuleEnergy::registerDataMaps);
	}

	private static void registerDataMaps(RegisterDataMapTypesEvent event) {
		event.register(ForestryDataMaps.BIOGAS_FUELS);
		event.register(ForestryDataMaps.COMBUSTION_FUELS);
		event.register(ForestryDataMaps.COMBUSTION_COOLANTS);
		event.register(ForestryDataMaps.PEAT_FUELS);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new EnergyClientHandler());
	}
}
