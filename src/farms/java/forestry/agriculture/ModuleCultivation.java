package forestry.agriculture;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.agriculture.features.MinifarmBlockEntities;
import forestry.agriculture.client.MinifarmClientHandler;
import forestry.agriculture.minifarm.tiles.AbstractMinifarmBlockEntity;
import forestry.modules.BlankForestryModule;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;
import java.util.function.Consumer;

@ForestryModule
public class ModuleCultivation extends BlankForestryModule {
	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		registerPlanterCapabilities(event, MinifarmBlockEntities.ARBORETUM.tileType());
		registerPlanterCapabilities(event, MinifarmBlockEntities.BOG.tileType());
		registerPlanterCapabilities(event, MinifarmBlockEntities.CROPS.tileType());
		registerPlanterCapabilities(event, MinifarmBlockEntities.ENDER.tileType());
		registerPlanterCapabilities(event, MinifarmBlockEntities.GOURD.tileType());
		registerPlanterCapabilities(event, MinifarmBlockEntities.MUSHROOM.tileType());
		registerPlanterCapabilities(event, MinifarmBlockEntities.NETHER.tileType());
	}

	private static <T extends AbstractMinifarmBlockEntity> void registerPlanterCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, type, (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (tile, side) -> tile.getEnergyHandler(side));
		event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (tile, side) -> tile.getFluidHandler(side));
	}

	@Override
	public Identifier getId() {
		return ForestryModuleIds.CULTIVATION;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleCultivation::registerCapabilities);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new MinifarmClientHandler());
	}

	@Override
	public List<Identifier> getModuleDependencies() {
		return List.of(ForestryModuleIds.CORE, ForestryModuleIds.FARMING);
	}
}
