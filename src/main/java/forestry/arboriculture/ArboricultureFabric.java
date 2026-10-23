package forestry.arboriculture;

import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.features.ArboricultureTiles;
import forestry.arboriculture.trees.TreeItem;
import forestry.arboriculture.wood.*;
import forestry.core.platform.registration.*;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.fabric.api.registry.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** Original tree content is owned by the core JAR; no additional Fabric mod is introduced. */
public final class ArboricultureFabric {
    public static void initializeRegistration() {
        var registry = ModFeatureRegistry.create(ForestryModuleIds.ARBORICULTURE, "forestry");
        ArboricultureBlocks.SAPLING_GE.id();
        forestry.arboriculture.features.CharcoalBlocks.LOG_PILE.id();
        ArboricultureTiles.SAPLING.getRegistry();
        forestry.arboriculture.features.ArboricultureItems.GRAFTER.getName();
        forestry.arboriculture.features.ArboricultureEntities.BOAT.getName();
        forestry.arboriculture.features.ArboricultureEntities.CHEST_BOAT.getName();
        registry.addRegistryListener(net.minecraft.core.registries.Registries.ITEM, () -> {
            for (var type : ForestryWoodType.VALUES) {
                net.minecraft.world.level.block.DispenserBlock.registerBehavior(forestry.arboriculture.features.ArboricultureItems.BOAT.item(type), new ForestryBoatDispenserBehavior(type, false));
                net.minecraft.world.level.block.DispenserBlock.registerBehavior(forestry.arboriculture.features.ArboricultureItems.CHEST_BOAT.item(type), new ForestryBoatDispenserBehavior(type, true));
            }
        });
        registry.addRegistryListener(net.minecraft.core.registries.Registries.BLOCK, ArboricultureFabric::finishBlocks);
        registry.creativeTab("arboriculture", tab -> tab.icon(() -> forestry.arboriculture.features.ArboricultureItems.GRAFTER.stack())
            .displayItems((parameters, output) -> {
                registry.getFeatures().stream()
                    .filter(feature -> feature instanceof IItemFeature<?>).map(feature -> (IItemFeature<?>) feature)
                    .forEach(feature -> BuiltInRegistries.ITEM.getOptional(feature.id())
                        .filter(item -> !(item instanceof TreeItem)).ifPresent(output::accept));
                SpeciesUtil.addTypeToCreativeTab(output, ForestrySpeciesTypes.TREE);
            }));
        var packets = forestry.core.platform.network.PacketRegistry.INSTANCE;
        packets.clientbound(forestry.arboriculture.network.PacketRipeningUpdate.TYPE, forestry.arboriculture.network.PacketRipeningUpdate::encode,
            forestry.arboriculture.network.PacketRipeningUpdate::decode, forestry.arboriculture.network.PacketRipeningUpdate::handle);
        packets.clientbound(forestry.core.platform.network.packets.PacketTileStream.TYPE, forestry.core.platform.network.packets.PacketTileStream::encode,
            forestry.core.platform.network.packets.PacketTileStream::decode, forestry.core.platform.network.packets.PacketTileStream::handle);
    }
    private static void finishBlocks() {
        FlammableBlockRegistry.getDefaultInstance().add(forestry.arboriculture.features.CharcoalBlocks.LOG_PILE.block(), 12, 25);
        FlammableBlockRegistry.getDefaultInstance().add(forestry.arboriculture.features.CharcoalBlocks.DECORATIVE_LOG_PILE.block(), 12, 25);
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof IWoodTyped wood) {
                if (!wood.isFireproof()) FlammableBlockRegistry.getDefaultInstance().add(block, 5, block instanceof ForestryLogBlock ? 5 : 20);
                if (block instanceof ForestryLogBlock log) {
                    var stripped = log.getStrippedState(block.defaultBlockState());
                    if (stripped != null) StrippableBlockRegistry.registerCopyState(block, stripped.getBlock());
                }
            } else if (block instanceof forestry.arboriculture.leaves.AbstractLeavesBlock || block instanceof forestry.arboriculture.leaves.DecorativeLeavesBlock) {
                FlammableBlockRegistry.getDefaultInstance().add(block, 5, 60);
            }
        }
        ArboricultureBlocks.HANGING_SIGN.getList().forEach(net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN::addValidBlock);
        ArboricultureBlocks.WALL_HANGING_SIGN.getList().forEach(net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN::addValidBlock);
        FuelValueEvents.BUILD.register((builder, context) -> {
            for (var type : ForestryWoodType.VALUES) {
                builder.add(forestry.arboriculture.features.ArboricultureItems.BOAT.item(type), 1200);
                builder.add(forestry.arboriculture.features.ArboricultureItems.CHEST_BOAT.item(type), 1200);
            }
            for (var item : BuiltInRegistries.ITEM) if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof IWoodTyped wood)
                builder.add(item, wood.isFireproof() ? 0 : blockItem.getBlock() instanceof ForestrySlabBlock ? 150 : 300);
            for (var stage : forestry.api.arboriculture.genetics.TreeLifeStage.values()) builder.add(stage.getItemForm(), 100);
            builder.add(forestry.arboriculture.features.CharcoalBlocks.CHARCOAL.item(), 16000);
        });
    }
}
