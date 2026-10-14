package forestry.core.content.machines;

import forestry.api.ForestryDataMaps;
import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IPacketRegistry;
import forestry.core.content.machines.client.FactoryClientHandler;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.network.packets.PacketRecipeTransferRequest;
import forestry.core.content.machines.network.packets.PacketRecipeTransferUpdate;
import forestry.core.platform.network.PacketIdClient;
import forestry.core.platform.network.PacketIdServer;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import java.util.function.Consumer;

@ForestryModule
public class ModuleFactory extends BlankForestryModule {
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.BOTTLER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.CARPENTER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.CENTRIFUGE.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.FABRICATOR.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.FERMENTER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.RAINMAKER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.MOISTENER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.SMELTER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.SQUEEZER.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FactoryTiles.STILL.tileType(), (tile, side) -> tile.getItemHandler(side));

		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryTiles.BOTTLER.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryTiles.CARPENTER.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryTiles.CENTRIFUGE.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryTiles.FABRICATOR.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryTiles.FERMENTER.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryTiles.SMELTER.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryTiles.SQUEEZER.tileType(), (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, FactoryTiles.STILL.tileType(), (tile, side) -> tile.getEnergyHandler(side));

		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryTiles.BOTTLER.tileType(), (tile, side) -> tile.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryTiles.CARPENTER.tileType(), (tile, side) -> tile.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryTiles.FABRICATOR.tileType(), (tile, side) -> tile.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryTiles.FERMENTER.tileType(), (tile, side) -> tile.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryTiles.MOISTENER.tileType(), (tile, side) -> tile.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryTiles.SQUEEZER.tileType(), (tile, side) -> tile.getFluidHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, FactoryTiles.STILL.tileType(), (tile, side) -> tile.getFluidHandler(side));
	}

	@Override
	public Identifier getId() {
		return ForestryModuleIds.FACTORY;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleFactory::registerCapabilities);
		modBus.addListener(ModuleFactory::registerDataMaps);
	}

	private static void registerDataMaps(RegisterDataMapTypesEvent event) {
		event.register(ForestryDataMaps.FERMENTER_FUELS);
		event.register(ForestryDataMaps.MOISTENER_FUELS);
		event.register(ForestryDataMaps.RAINMAKER_FUELS);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new FactoryClientHandler());
	}

	@Override
	public void registerPackets(IPacketRegistry registry) {
		registry.serverbound(PacketIdServer.RECIPE_TRANSFER_REQUEST, PacketRecipeTransferRequest::encode, PacketRecipeTransferRequest::decode, PacketRecipeTransferRequest::handle);
		registry.clientbound(PacketIdClient.RECIPE_TRANSFER_UPDATE, PacketRecipeTransferUpdate::encode, PacketRecipeTransferUpdate::decode, PacketRecipeTransferUpdate::handle);
	}
}
