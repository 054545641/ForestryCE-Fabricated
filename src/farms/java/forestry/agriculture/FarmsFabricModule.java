package forestry.agriculture;

import forestry.Forestry;
import forestry.api.modules.*;
import forestry.core.platform.registration.FeatureRegistry;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Physical farms and the reloadable farm domain share the optional farms artifact. */
public final class FarmsFabricModule implements IForestryModule {
    @Override public Identifier getId() { return ForestryModuleIds.FARMING; }
    @Override public String getOwnerModId() { return "forestryfarms"; }
    @Override public List<Identifier> getModuleDependencies() { return List.of(ForestryModuleIds.CORE); }
    @Override public void register() {
        forestry.core.platform.registration.ModFeatureRegistry.create(ForestryModuleIds.CULTIVATION, "forestryfarms");
        forestry.agriculture.features.MinifarmBlocks.MANAGED_PLANTER.getFeatures();
        forestry.agriculture.features.MinifarmBlocks.MANUAL_PLANTER.getFeatures();
        forestry.agriculture.features.MultifarmBlocks.FARM.getFeatures();
        forestry.agriculture.features.MinifarmBlockEntities.ARBORETUM.getName();
        forestry.agriculture.features.MultifarmBlockEntities.PLAIN.getName();
        forestry.agriculture.features.MinifarmMenuTypes.PLANTER.getName();
        forestry.agriculture.features.MultifarmMenuTypes.FARM.getName();
        var registry = forestry.core.platform.registration.ModFeatureRegistry.get(getId());
        registry.addRegistryListener(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, () -> {
            for (var type : List.of(forestry.agriculture.features.MinifarmBlockEntities.ARBORETUM,
                    forestry.agriculture.features.MinifarmBlockEntities.BOG, forestry.agriculture.features.MinifarmBlockEntities.CROPS,
                    forestry.agriculture.features.MinifarmBlockEntities.ENDER, forestry.agriculture.features.MinifarmBlockEntities.GOURD,
                    forestry.agriculture.features.MinifarmBlockEntities.MUSHROOM, forestry.agriculture.features.MinifarmBlockEntities.NETHER))
                registerPlanter(type.tileType());
            team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getEnergyStorage(side), forestry.agriculture.features.MultifarmBlockEntities.GEARBOX.tileType());
            net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getItemHandler(side), forestry.agriculture.features.MultifarmBlockEntities.HATCH.tileType());
            net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getFluidHandler(side), forestry.agriculture.features.MultifarmBlockEntities.VALVE.tileType());
        });
        registry.creativeTab("farming", tab -> tab.icon(() -> forestry.agriculture.features.MinifarmBlocks.MANAGED_PLANTER.getFeatures().iterator().next().stack())
            .displayItems((parameters, output) -> {
                forestry.agriculture.features.MinifarmBlocks.MANAGED_PLANTER.getFeatures().forEach(feature -> output.accept(feature.stack()));
                forestry.agriculture.features.MinifarmBlocks.MANUAL_PLANTER.getFeatures().forEach(feature -> output.accept(feature.stack()));
                forestry.agriculture.features.MultifarmBlocks.FARM.getFeatures().forEach(feature -> output.accept(feature.stack()));
            }));
        forestry.core.platform.data.GeneticDataLoader.afterProjection(() -> {
            var registration = new forestry.agriculture.plugin.FarmingRegistration();
            java.util.ServiceLoader.load(forestry.api.plugin.IForestryPlugin.class).stream().map(java.util.ServiceLoader.Provider::get)
                .filter(forestry.api.plugin.IForestryPlugin::shouldLoad).forEach(plugin -> plugin.registerFarming(registration));
            ((forestry.apiimpl.ForestryApiImpl) forestry.api.IForestryApi.INSTANCE).setFarmingManager(new forestry.agriculture.farmlogic.FarmingManager(registration.buildFarmTypes()));
        });
    }
    private static <T extends forestry.agriculture.minifarm.tiles.AbstractMinifarmBlockEntity> void registerPlanter(net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getItemHandler(side), type);
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getFluidHandler(side), type);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getEnergyStorage(side), type);
    }
}
