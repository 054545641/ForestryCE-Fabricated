package forestry.fabric.test;
import forestry.core.features.FluidsItems;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.item.FluidContainerType;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.fluids.FluidHelper;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
public final class F2ContainerTests {
    @GameTest public void wholeBucketContainersAndConsumptionRollback(GameTestHelper h) {
        for (var type : FluidContainerType.values()) {
            var inventory = new SimpleContainer(FluidsItems.CONTAINERS.get(type).stack());
            inventory.getItem(0).set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("F2 vessel"));
            var handler = ContainerItemContext.ofSingleSlot(ContainerStorage.of(inventory, null).getSlot(0)).find(FluidStorage.ITEM);
            try (var tx = Transaction.openOuter()) { h.assertTrue(handler.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET - 1, tx) == 0, "Container partially filled: " + type); }
            try (var tx = Transaction.openOuter()) { h.assertTrue(handler.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET, "Container fill failed: " + type); }
            h.assertTrue(FluidHelper.getFluidContained(inventory.getItem(0)).isEmpty(), "Aborted fill persisted");
            try (var tx = Transaction.openOuter()) { handler.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx); tx.commit(); }
            var full = inventory.getItem(0).copy();
            h.assertTrue(full.getHoverName().getString().equals("F2 vessel"), "Filling dropped custom item data");
            var tank = new StandardTank(2000);
            try (var tx = Transaction.openOuter()) {
                h.assertTrue(FluidHelper.drainContainers(tank, inventory, 0, 0, true) == FluidHelper.FillStatus.SUCCESS && inventory.isEmpty(), "Container not consumed by draining");
            }
            h.assertTrue(ItemStack.matches(full, inventory.getItem(0)) && tank.getFluid().isEmpty(), "Outer abort did not restore consumed vessel and tank");
            h.assertTrue(FluidHelper.drainContainers(tank, inventory, 0, 0, true) == FluidHelper.FillStatus.SUCCESS && inventory.isEmpty() && tank.getFluid().amount() == FluidConstants.BUCKET, "Committed drain did not consume exactly one vessel");
        }
        var wax = new SimpleContainer(FluidsItems.CONTAINERS.get(FluidContainerType.WAX_CAPSULE).stack());
        var handler = ContainerItemContext.ofSingleSlot(ContainerStorage.of(wax, null).getSlot(0)).find(FluidStorage.ITEM);
        try (var tx = Transaction.openOuter()) { h.assertTrue(handler.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx) == 0, "Wax accepted hot lava"); }
        h.succeed();
    }
    @GameTest public void charcoalPitSealingYieldAndNativeFuel(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new net.minecraft.core.BlockPos(3, 3, 3));
        var pile = forestry.arboriculture.features.CharcoalBlocks.LOG_PILE.block();
        for (var direction : net.minecraft.core.Direction.values()) level.setBlockAndUpdate(pos.relative(direction), net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        var active = pile.defaultBlockState().setValue(forestry.arboriculture.charcoal.LogPileBlock.IS_ACTIVE, true).setValue(forestry.arboriculture.charcoal.LogPileBlock.AGE, 7);
        level.setBlockAndUpdate(pos, active);
        for (int i = 0; i < 100 && level.getBlockState(pos).is(pile); i++) pile.tick(level.getBlockState(pos), level, pos, level.getRandom());
        var ash = level.getBlockState(pos);
        h.assertTrue(ash.is(forestry.arboriculture.features.CharcoalBlocks.ASH.block()) && ash.getValue(forestry.arboriculture.charcoal.AshBlock.AMOUNT) > 8, "Sealed charcoal pile lost original wall-dependent yield");
        h.assertTrue(level.fuelValues().burnDuration(forestry.arboriculture.features.CharcoalBlocks.CHARCOAL.stack()) == 16000, "Charcoal block native fuel missing");
        h.succeed();
    }
}
