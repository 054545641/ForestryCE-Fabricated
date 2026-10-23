package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.sorting.features.*;
import forestry.core.content.sorting.network.packets.*;
import forestry.core.content.worktable.features.*;
import forestry.core.content.worktable.network.packets.*;
import forestry.core.platform.registration.ModFeatureRegistry;

public final class FabricStorageBlocks {
    public static void register() {
        var worktable = ModFeatureRegistry.create(ForestryModuleIds.WORKTABLE, "forestry");
        var sorting = ModFeatureRegistry.create(ForestryModuleIds.SORTING, "forestry");
        WorktableBlocks.WORKTABLE.id(); WorktableMenus.WORKTABLE.getName();
        SortingBlocks.FILTER.id(); SortingMenuTypes.GENETIC_FILTER.getName();
        WorktableTiles.WORKTABLE.getName(); SortingTiles.GENETIC_FILTER.getName();
        worktable.addRegistryListener(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, () ->
            net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getItemHandler(side), WorktableTiles.WORKTABLE.tileType()));
        sorting.addRegistryListener(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, () -> {
            net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getItemHandler(side), SortingTiles.GENETIC_FILTER.tileType());
            forestry.api.ForestryCapabilities.FILTER_LOGIC.registerForBlockEntity((tile, side) -> tile.isRemoved() ? null : tile.getLogic(), SortingTiles.GENETIC_FILTER.tileType());
        });
        var packets = forestry.core.platform.network.PacketRegistry.INSTANCE;
        packets.serverbound(PacketFilterChangeRule.TYPE, PacketFilterChangeRule::encode, PacketFilterChangeRule::decode, PacketFilterChangeRule::handle);
        packets.serverbound(PacketFilterChangeGenome.TYPE, PacketFilterChangeGenome::encode, PacketFilterChangeGenome::decode, PacketFilterChangeGenome::handle);
        packets.clientbound(PacketGuiFilterUpdate.TYPE, PacketGuiFilterUpdate::encode, PacketGuiFilterUpdate::decode, PacketGuiFilterUpdate::handle);
        packets.serverbound(PacketWorktableRecipeRequest.TYPE, PacketWorktableRecipeRequest::encode, PacketWorktableRecipeRequest::decode, PacketWorktableRecipeRequest::handle);
        packets.clientbound(PacketWorktableRecipeUpdate.TYPE, PacketWorktableRecipeUpdate::encode, PacketWorktableRecipeUpdate::decode, PacketWorktableRecipeUpdate::handle);
        packets.clientbound(PacketWorktableMemoryUpdate.TYPE, PacketWorktableMemoryUpdate::encode, PacketWorktableMemoryUpdate::decode, PacketWorktableMemoryUpdate::handle);
    }
}
