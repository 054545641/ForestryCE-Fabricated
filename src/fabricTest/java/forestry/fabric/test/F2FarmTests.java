package forestry.fabric.test;

import forestry.api.IForestryApi;
import forestry.api.agriculture.ForestryFarmTypes;
import forestry.agriculture.minifarm.inventory.MinifarmInventory;
import forestry.agriculture.minifarm.tiles.AbstractMinifarmBlockEntity;
import forestry.agriculture.multifarm.multiblock.InventoryFarm;
import forestry.agriculture.multifarm.tiles.TileFarm;
import forestry.core.platform.multiblock.MultiblockTileEntityForestry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import team.reborn.energy.api.EnergyStorage;
import static forestry.fabric.test.F1RegistrationTests.*;

public final class F2FarmTests {
    private static boolean enabled(GameTestHelper test) {
        if (FabricLoader.getInstance().isModLoaded("forestryfarms")) return true;
        require(!IForestryApi.INSTANCE.getFarmingManager().isLoaded(), "Absent farms module exposed a real manager");
        test.succeed(); return false;
    }

    @GameTest public void farmDomainAndFertilizerMapAreUsed(GameTestHelper test) {
        if (!enabled(test)) return;
        var manager = IForestryApi.INSTANCE.getFarmingManager();
        for (var type : java.util.List.of(ForestryFarmTypes.CROPS, ForestryFarmTypes.GOURD, ForestryFarmTypes.SHROOM,
                ForestryFarmTypes.INFERNAL, ForestryFarmTypes.POALES, ForestryFarmTypes.SUCCULENTES, ForestryFarmTypes.ENDER,
                ForestryFarmTypes.ARBOREAL, ForestryFarmTypes.PEAT, ForestryFarmTypes.ORCHARD, ForestryFarmTypes.COCOA))
            require(manager.getFarmType(type) != null, "Missing farm type " + type);
        var pos = new BlockPos(1, 1, 1);
        test.setBlock(pos, BuiltInRegistries.BLOCK.getValue(id("farm_crops_managed")));
        var tile = test.getBlockEntity(pos, AbstractMinifarmBlockEntity.class);
        tile.setManual(false);
        var fertilizer = new ItemStack(BuiltInRegistries.ITEM.getValue(id("fertilizer")), 2);
        require(manager.getFertilizeValue(fertilizer) == 500, "Fertilizer map not loaded");
        tile.setItem(MinifarmInventory.CONFIG.fertilizerStart, fertilizer);
        // Real farm work consumes the DataMap value into the persisted fertilizer buffer even when no crop is ready.
        tile.getEnergyManager().setEnergyStored(100);
        tile.processWorkStep(); tile.processWorkStep();
        test.runAfterDelay(2, () -> {
        require(tile.getItem(MinifarmInventory.CONFIG.fertilizerStart).getCount() == 1, "Farm did not consume its fertilizer");
        require(tile.saveWithoutMetadata(test.getLevel().registryAccess()).getIntOr("StoredFertilizer", 0) == 2000,
            "Farm lost the configured fertilizer multiplier");
        test.succeed();
        });
    }

    @GameTest public void planterLookupRollbackAndNativePersistence(GameTestHelper test) {
        if (!enabled(test)) return;
        var pos = new BlockPos(1, 1, 1);
        test.setBlock(pos, BuiltInRegistries.BLOCK.getValue(id("arboretum_managed")));
        var tile = test.getBlockEntity(pos, AbstractMinifarmBlockEntity.class); tile.setManual(false);
        var items = ItemStorage.SIDED.find(test.getLevel(), tile.getBlockPos(), Direction.UP);
        var fluids = FluidStorage.SIDED.find(test.getLevel(), tile.getBlockPos(), Direction.NORTH);
        var energy = EnergyStorage.SIDED.find(test.getLevel(), tile.getBlockPos(), Direction.DOWN);
        require(items != null && fluids != null && energy != null, "Planter lookup is incomplete");
        var fertilizer = ItemVariant.of(BuiltInRegistries.ITEM.getValue(id("fertilizer")));
        try (var outer = Transaction.openOuter()) {
            try (var nested = Transaction.openNested(outer)) {
                require(items.insert(fertilizer, 2, nested) == 2, "Fertilizer insertion failed");
                require(fluids.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET + 1, nested) == FluidConstants.BUCKET + 1, "Water insertion rounded droplets");
                require(energy.insert(120, nested) == 120, "Energy insertion failed");
                nested.commit();
            }
        }
        require(tile.getItem(MinifarmInventory.CONFIG.fertilizerStart).isEmpty() && tile.getTankManager().getTank(0).isEmpty()
            && energy.getAmount() == 0, "Outer rollback failed across the actual machine stores");
        try (var transaction = Transaction.openOuter()) {
            items.insert(fertilizer, 2, transaction); fluids.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET + 1, transaction);
            energy.insert(120, transaction); transaction.commit();
        }
        try (var transaction = Transaction.openOuter()) { tile.processWorkStep(); }
        require(tile.getWorkCounter() == 0 && energy.getAmount() == 120, "Aborted work charged energy or advanced progress");
        var data = tile.saveWithFullMetadata(test.getLevel().registryAccess());
        var copy = (AbstractMinifarmBlockEntity) BlockEntity.loadStatic(tile.getBlockPos(), tile.getBlockState(), data, test.getLevel().registryAccess());
        require(copy != null && copy.getEnergyManager().getAmount() == 120 && copy.getTankManager().getTank(0).getAmount() == FluidConstants.BUCKET + 1,
            "Native planter persistence lost resources");
        require(copy.getItem(MinifarmInventory.CONFIG.fertilizerStart).getCount() == 2, "Native planter inventory changed");
        test.succeed();
    }

    @GameTest(maxTicks = 150) public void farmPortsAndHolderSurviveBreakAndReassembly(GameTestHelper test) {
        if (!enabled(test)) return;
        var base = new BlockPos(1, 1, 1);
        for (int x = 0; x < 3; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 3; z++)
            test.setBlock(base.offset(x, y, z), BuiltInRegistries.BLOCK.getValue(id("stone_brick_farm_block")));
        var gear = base.offset(1, 0, 0); var hatch = base.offset(2, 0, 0); var valve = base.offset(0, 0, 1);
        test.setBlock(hatch, BuiltInRegistries.BLOCK.getValue(id("stone_brick_farm_hatch")));
        test.setBlock(valve, BuiltInRegistries.BLOCK.getValue(id("stone_brick_farm_valve")));
        test.setBlock(gear, BuiltInRegistries.BLOCK.getValue(id("stone_brick_farm_gearbox")));
        test.runAfterDelay(2, () -> {
            var holder = test.getBlockEntity(base, TileFarm.class);
            var controller = holder.getMultiblockLogic().getController();
            require(controller.isAssembled(), "Native placement callbacks did not assemble the farm");
            var items = ItemStorage.SIDED.find(test.getLevel(), test.absolutePos(hatch), Direction.DOWN);
            var fluid = FluidStorage.SIDED.find(test.getLevel(), test.absolutePos(valve), Direction.NORTH);
            var energy = EnergyStorage.SIDED.find(test.getLevel(), test.absolutePos(gear), Direction.SOUTH);
            require(items != null && fluid != null && energy != null, "Multifarm port lookup missing");
            controller.getInternalInventory().setItem(InventoryFarm.CONFIG.productionStart, new ItemStack(Items.WHEAT, 7));
            // Fabric may provide a zero-slot fallback for WorldlyContainer. Assert the observable policy.
            var plain = ItemStorage.SIDED.find(test.getLevel(), holder.getBlockPos(), Direction.DOWN);
            try (var transaction = Transaction.openOuter()) {
                require(plain == null || plain.extract(ItemVariant.of(Items.WHEAT), 1, transaction) == 0,
                    "Plain farm blocks exposed automation");
            }
            try (var transaction = Transaction.openOuter()) {
                require(items.extract(ItemVariant.of(Items.WHEAT), 2, transaction) == 2, "Hatch extraction failed");
            }
            require(controller.getInternalInventory().getItem(InventoryFarm.CONFIG.productionStart).getCount() == 7, "Aborted hatch extraction changed inventory");
            try (var transaction = Transaction.openOuter()) {
                fluid.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction); energy.insert(100, transaction); transaction.commit();
            }
            require(holder.saveWithoutMetadata(test.getLevel().registryAccess()).contains(MultiblockTileEntityForestry.PAYLOAD_KEY), "Holder did not save shared data");
            test.setBlock(gear, Blocks.AIR);
            test.setBlock(gear, BuiltInRegistries.BLOCK.getValue(id("stone_brick_farm_gearbox")));
            var restored = holder.getMultiblockLogic().getController();
            require(restored.isAssembled(), "Farm did not reassemble after replacing gearbox");
            require(restored.getInternalInventory().getItem(InventoryFarm.CONFIG.productionStart).getCount() == 7, "Shared inventory was lost or duplicated across reassembly");
            require(restored.getTankManager().getTank(0).getAmount() == FluidConstants.BUCKET, "Shared tank was lost across reassembly");
            test.succeed();
        });
    }
}
