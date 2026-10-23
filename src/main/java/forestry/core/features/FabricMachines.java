package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.energy.features.*;
import forestry.core.content.machines.features.*;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.platform.tile.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import team.reborn.energy.api.EnergyStorage;
import java.util.List;

/** Registers the original machines under their persisted IDs, owned by the base artifact. */
public final class FabricMachines {
    public static void register() {
        var packets = forestry.core.platform.network.PacketRegistry.INSTANCE;
        packets.serverbound(forestry.core.content.machines.network.packets.PacketRecipeTransferRequest.TYPE,
            forestry.core.content.machines.network.packets.PacketRecipeTransferRequest::encode,
            forestry.core.content.machines.network.packets.PacketRecipeTransferRequest::decode,
            forestry.core.content.machines.network.packets.PacketRecipeTransferRequest::handle);
        packets.clientbound(forestry.core.content.machines.network.packets.PacketRecipeTransferUpdate.TYPE,
            forestry.core.content.machines.network.packets.PacketRecipeTransferUpdate::encode,
            forestry.core.content.machines.network.packets.PacketRecipeTransferUpdate::decode,
            forestry.core.content.machines.network.packets.PacketRecipeTransferUpdate::handle);
        var factory = ModFeatureRegistry.create(ForestryModuleIds.FACTORY, "forestry");
        var energy = ModFeatureRegistry.create(ForestryModuleIds.ENERGY, "forestry");
        FactoryBlocks.PLAIN.getFeatures(); FactoryBlocks.TESR.getFeatures();
        EnergyBlocks.ENGINES.getFeatures(); EnergyBlocks.SOLAR_PANEL.id();
        FactoryRecipeTypes.CARPENTER.getName(); FactoryMenuTypes.CARPENTER.getName(); EnergyMenus.ENGINE_BIOGAS.getName();
        factory.addRegistryListener(Registries.BLOCK_ENTITY_TYPE, () -> {
            for (var feature : List.of(FactoryTiles.BOTTLER, FactoryTiles.CARPENTER, FactoryTiles.CENTRIFUGE,
                    FactoryTiles.FABRICATOR, FactoryTiles.FERMENTER, FactoryTiles.RAINMAKER, FactoryTiles.MOISTENER,
                    FactoryTiles.SMELTER, FactoryTiles.SQUEEZER, FactoryTiles.STILL)) registerMachine(feature.tileType(), true);
        });
        energy.addRegistryListener(Registries.BLOCK_ENTITY_TYPE, () -> {
            for (var feature : List.of(EnergyTiles.PEAT_ENGINE, EnergyTiles.BIOGAS_ENGINE, EnergyTiles.COMBUSTION_ENGINE)) registerMachine(feature.tileType(), false);
            for (var feature : List.of(EnergyTiles.PEAT_ENGINE, EnergyTiles.BIOGAS_ENGINE, EnergyTiles.CLOCKWORK_ENGINE,
                    EnergyTiles.COMBUSTION_ENGINE, EnergyTiles.SOLAR_ENGINE))
                EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getEnergyStorage(side), feature.tileType());
        });
        factory.creativeTab("factory", tab -> tab.icon(() -> FactoryBlocks.PLAIN.getFeatures().iterator().next().stack())
            .displayItems((parameters, output) -> {
                FactoryBlocks.PLAIN.getFeatures().forEach(feature -> output.accept(feature.stack()));
                FactoryBlocks.TESR.getFeatures().forEach(feature -> output.accept(feature.stack()));
                EnergyBlocks.ENGINES.getFeatures().forEach(feature -> output.accept(feature.stack()));
                output.accept(EnergyBlocks.SOLAR_PANEL.stack());
            }));
    }
    private static <T extends TileForestry> void registerMachine(BlockEntityType<T> type, boolean powered) {
        ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getItemHandler(side), type);
        if (powered) EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile instanceof TilePowered machine ? machine.getEnergyStorage(side) : null, type);
        FluidStorage.SIDED.registerForBlockEntity((tile, side) -> !tile.isRemoved() && tile instanceof ILiquidTankTile tank ? tank.getTankManager() : null, type);
    }
}
