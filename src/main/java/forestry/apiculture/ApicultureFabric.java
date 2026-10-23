package forestry.apiculture;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.features.*;
import forestry.apiculture.alveary.multiblock.*;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.registries.Registries;
import team.reborn.energy.api.EnergyStorage;
import java.util.List;

/** Physical bee housing shares the original domain implementation and persisted identifiers. */
public final class ApicultureFabric {
    public static void register() {
        var registry = ModFeatureRegistry.create(ForestryModuleIds.APICULTURE, "forestry");
        var items = new java.util.ArrayList<forestry.core.platform.registration.IItemFeature<?>>();
        items.add(registry.item(properties -> new forestry.apiculture.apiary.HiveFrameItem(80, 0.9f, properties), "untreated_frame"));
        items.add(registry.item(properties -> new forestry.apiculture.apiary.HiveFrameItem(240, 0.4f, properties), "impregnated_frame"));
        items.add(registry.item(properties -> new forestry.apiculture.apiary.HiveFrameItem(720, 0.3f, properties), "proven_frame"));
        items.add(registry.item(forestry.apiculture.apiary.CreativeHiveFrameItem::new, "creative_frame"));
        items.add(registry.item(forestry.apiculture.apiarist.ItemSmoker::new, "bee_smoker"));
        items.add(registry.item(forestry.apiculture.bees.ItemAmbrosia::new, "ambrosia"));
        items.add(registry.item(properties -> new forestry.core.platform.item.ItemForestryFood(8, 0.6f, properties), "honeyed_slice"));
        for (String id : List.of("experience_drop", "magmatic_drop", "amber_drone_fossil")) items.add(registry.item(net.minecraft.world.item.Item::new, id));
        for (var propolis : forestry.apiculture.bees.EnumPropolis.values()) items.add(registry.item(properties ->
            new forestry.apiculture.bees.PropolisItem(propolis, properties), propolis == forestry.apiculture.bees.EnumPropolis.NORMAL ? "propolis" : propolis.getSerializedName() + "_propolis"));
        ApicultureBlocks.BASE.getFeatures(); ApicultureBlocks.HIVE.getFeatures();
        ApicultureBlocks.COMB_BLOCK.getFeatures(); ApicultureBlocks.ALVEARY.getFeatures();
        ApicultureMenuTypes.BEE_HOUSING.getName(); ApicultureRecipeTypes.HYGROREGULATOR.getName();
        registry.addRegistryListener(Registries.BLOCK_ENTITY_TYPE, () -> {
            ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getItemHandler(side), ApicultureTiles.APIARY.tileType());
            for (var feature : List.of(ApicultureTiles.ALVEARY_PLAIN, ApicultureTiles.ALVEARY_SIEVE,
                    ApicultureTiles.ALVEARY_SWARMER, ApicultureTiles.ALVEARY_HYGROREGULATOR,
                    ApicultureTiles.ALVEARY_STABILISER, ApicultureTiles.ALVEARY_FAN, ApicultureTiles.ALVEARY_HEATER))
                ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getItemHandler(side), feature.tileType());
            EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getEnergyStorage(side), ApicultureTiles.ALVEARY_FAN.tileType());
            EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getEnergyStorage(side), ApicultureTiles.ALVEARY_HEATER.tileType());
            FluidStorage.SIDED.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getFluidHandler(side), ApicultureTiles.ALVEARY_HYGROREGULATOR.tileType());
        });
        registry.addRegistryListener(Registries.BLOCK, () -> {
            var fire = net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance();
            ApicultureBlocks.HIVE.getFeatures().forEach(feature -> fire.add(feature.block(), 5, 5));
            fire.add(ApicultureBlocks.WAX_BLOCK.block(), 45, 45);
        });
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!player.isSpectator() && player.getItemInHand(hand).getItem() instanceof forestry.apiculture.apiarist.ItemSmoker smoker)
                return smoker.useBeforeBlock(player.getItemInHand(hand), new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
            return net.minecraft.world.InteractionResult.PASS;
        });
        var packets = forestry.core.platform.network.PacketRegistry.INSTANCE;
        packets.clientbound(forestry.apiculture.network.packets.PacketBeeLogicActive.TYPE,
            forestry.apiculture.network.packets.PacketBeeLogicActive::encode,
            forestry.apiculture.network.packets.PacketBeeLogicActive::decode,
            forestry.apiculture.network.packets.PacketBeeLogicActive::handle);
        forestry.api.core.ForestryEvent.EVENTS.register(event -> {
            if (event instanceof forestry.api.core.ForestryEvent.BeeMatingEvent mating
                    && mating.getPrincess().getSpecies().getGenusName().equals(forestry.api.core.genetics.ForestryTaxa.GENUS_EMBITTERED)
                    && mating.getHousing().temperature() != forestry.api.core.TemperatureType.HELLISH)
                mating.setPrincess(forestry.core.platform.util.SpeciesUtil.getBeeSpecies(forestry.api.apiculture.ForestryBeeSpecies.ZOMBIFIED).createIndividual());
        });
        registry.creativeTab("apiculture", tab -> tab.icon(() -> ApicultureBlocks.BASE.getFeatures().iterator().next().stack())
            .displayItems((parameters, output) -> {
                items.forEach(feature -> output.accept(feature.stack()));
                ApicultureBlocks.BASE.getFeatures().forEach(feature -> output.accept(feature.stack()));
                ApicultureBlocks.HIVE.getFeatures().forEach(feature -> output.accept(feature.stack()));
                ApicultureBlocks.COMB_BLOCK.getFeatures().forEach(feature -> output.accept(feature.stack()));
                ApicultureBlocks.ALVEARY.getFeatures().forEach(feature -> output.accept(feature.stack()));
                output.accept(ApicultureBlocks.WAX_BLOCK.stack()); output.accept(ApicultureBlocks.REFRACTORY_WAX_BLOCK.stack());
                forestry.core.platform.util.SpeciesUtil.addTypeToCreativeTab(output, forestry.api.core.genetics.ForestrySpeciesTypes.BEE);
            }));
    }
}
