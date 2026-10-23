package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.block.*;
import forestry.core.platform.item.*;
import forestry.core.platform.registration.*;
import forestry.core.content.burnbarrel.BlockBurnBarrel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.MapColor;
import java.util.List;

/** Original utility block declarations, separate from the already registered material families. */
public final class CoreUtilityBlocks {
    private static final FeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
    public static final FeatureBlockGroup<BlockCore, BlockTypeCoreTesr> BASE = REGISTRY
        .blockGroup((properties, type) -> new BlockCore(type, properties), List.of(BlockTypeCoreTesr.values()))
        .blockProperties((properties, type) -> type.getBlockProperties()).item(ItemBlockTesr::new).create();
    public static final FeatureBlockGroup<BlockTesr<NaturalistChestBlockType>, NaturalistChestBlockType> NATURALIST_CHEST = REGISTRY
        .blockGroup((properties, type) -> new BlockTesr<>(type, properties.sound(SoundType.WOOD)), List.of(NaturalistChestBlockType.values()))
        .item(ItemBlockTesr::new).create();
    public static final FeatureBlock<BlockBurnBarrel, ItemBlockForestry<?>> BURN_BARREL = REGISTRY.block(BlockBurnBarrel::new,
        () -> Block.Properties.ofFullCopy(Blocks.FURNACE).mapColor(MapColor.COLOR_GRAY).sound(SoundType.METAL)
            .lightLevel(state -> state.getValue(BlockBurnBarrel.LIT) ? 15 : 0), ItemBlockForestry::new, "burn_barrel");

    public static void register() {
        forestry.core.platform.particles.CoreParticles.REFRACTORY_WAX.getId();
        BASE.getFeatures(); NATURALIST_CHEST.getFeatures(); BURN_BARREL.id(); CoreMenuTypes.ANALYZER.getName();
        REGISTRY.addRegistryListener(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, () -> {
            for (var feature : List.of(CoreTiles.ANALYZER, CoreTiles.ESCRITOIRE, CoreTiles.APIARIST_CHEST,
                    CoreTiles.ARBORIST_CHEST, CoreTiles.LEPIDOPTERIST_CHEST, CoreTiles.BURN_BARREL))
                net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getItemHandler(side), feature.tileType());
            net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getTankManager(), CoreTiles.ANALYZER.tileType());
            team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getEnergyStorage(side), CoreTiles.ANALYZER.tileType());
        });
        REGISTRY.creativeTab("utilities", tab -> tab.icon(() -> BASE.get(BlockTypeCoreTesr.ANALYZER).stack())
            .displayItems((parameters, output) -> {
                BASE.getFeatures().forEach(feature -> output.accept(feature.stack()));
                NATURALIST_CHEST.getFeatures().forEach(feature -> output.accept(feature.stack()));
                output.accept(BURN_BARREL.stack());
                output.accept(forestry.core.content.worktable.features.WorktableBlocks.WORKTABLE.stack());
                output.accept(forestry.core.content.sorting.features.SortingBlocks.FILTER.stack());
            }));
    }
}
