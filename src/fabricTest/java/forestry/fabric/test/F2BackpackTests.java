package forestry.fabric.test;
import forestry.core.content.backpacks.*;
import forestry.core.content.backpacks.features.BackpackItems;
import forestry.core.content.backpacks.inventory.BackpackInventory;
import forestry.core.content.backpacks.items.BackpackItem;
import forestry.core.features.CoreDataComponents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
public final class F2BackpackTests {
    @GameTest public void nestedStowAndResupplyRestoreSourceAndDestination(GameTestHelper h) {
        var player = net.fabricmc.fabric.api.entity.FakePlayer.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.fromString("1ae6b7cd-d55d-4fc7-bf42-336b28a408ed"), "F2BagAbort"));
        player.getInventory().clearContent();
        var pack = BackpackItems.MINER_BACKPACK.stack(); player.getInventory().setItem(0, pack);
        var diamonds = new ItemStack(Items.DIAMOND, 10);
        try (var tx = Transaction.openOuter()) { BackpackItem.tryStowing(player, pack, diamonds); h.assertTrue(diamonds.isEmpty(), "Nested stow did not execute"); }
        h.assertTrue(diamonds.getCount() == 10 && new BackpackInventory(player, 15, pack).isEmpty(), "Aborted stow lost its external source stack");
        BackpackItem.tryStowing(player, pack, diamonds);
        BackpackMode.RESUPPLY.write(pack, CoreDataComponents.BACKPACK_MODE.get());
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 63));
        try (var tx = Transaction.openOuter()) { BackpackResupplyHandler.resupply(player); }
        h.assertTrue(player.getInventory().getItem(1).getCount() == 63 && new BackpackInventory(player, 15, pack).getItem(0).getCount() == 10, "Aborted resupply lost or duplicated items");
        h.succeed();
    }
    @GameTest public void realPickupPreservesDelayAndOwnership(GameTestHelper h) {
        var player = net.fabricmc.fabric.api.entity.FakePlayer.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.fromString("2ae6b7cd-d55d-4fc7-bf42-336b28a408ed"), "F2BagPickup"));
        player.getInventory().clearContent();
        var pack = BackpackItems.MINER_BACKPACK.stack(); player.getInventory().setItem(0, pack);
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        var item = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.DIAMOND, 5));
        h.getLevel().addFreshEntity(item);
        item.setDefaultPickUpDelay(); item.playerTouch(player);
        h.assertTrue(item.getItem().getCount() == 5 && new BackpackInventory(player, 15, pack).isEmpty(), "Backpack bypassed pickup delay");
        item.setNoPickUpDelay(); item.setTarget(java.util.UUID.randomUUID()); item.playerTouch(player);
        h.assertTrue(item.getItem().getCount() == 5 && new BackpackInventory(player, 15, pack).isEmpty(), "Backpack bypassed item ownership");
        item.setTarget(player.getUUID()); item.playerTouch(player);
        h.assertTrue(item.isRemoved() && new BackpackInventory(player, 15, pack).getItem(0).getCount() == 5, "Native item pickup did not stow into the backpack exactly once");
        h.succeed();
    }
    @GameTest public void backpacks(GameTestHelper h) {
        var player = net.fabricmc.fabric.api.entity.FakePlayer.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.fromString("0ae6b7cd-d55d-4fc7-bf42-336b28a408ed"), "F2Backpacks"));
        player.getInventory().clearContent();
        var pack = BackpackItems.MINER_BACKPACK.stack();
        player.getInventory().setItem(0, pack);
        var diamonds = new ItemStack(Items.DIAMOND, 17);
        BackpackItem.tryStowing(player, pack, diamonds);
        h.assertTrue(diamonds.isEmpty(), "Miner backpack stows accepted resources");
        var apples = new ItemStack(Items.APPLE, 3);
        BackpackItem.tryStowing(player, pack, apples);
        h.assertTrue(apples.getCount() == 3, "Miner backpack rejects food");
        BackpackMode.LOCKED.write(pack, CoreDataComponents.BACKPACK_MODE.get());
        var more = new ItemStack(Items.DIAMOND, 2);
        BackpackItem.tryStowing(player, pack, more);
        h.assertTrue(more.getCount() == 2, "Locked backpack rejects pickup");
        BackpackMode.RESUPPLY.write(pack, CoreDataComponents.BACKPACK_MODE.get());
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 63));
        BackpackResupplyHandler.resupply(player);
        h.assertTrue(player.getInventory().getItem(1).getCount() == 64, "Resupply tops up existing stack");
        var inventory = new BackpackInventory(player, 15, pack);
        h.assertTrue(inventory.getItem(0).getCount() == 16, "Resupply consumes exactly one stored item");
        var chestPos = h.absolutePos(new BlockPos(3, 2, 3));
        h.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) h.getLevel().getBlockEntity(chestPos);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pack);
        player.setShiftKeyDown(true);
        var context = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chestPos), Direction.UP, chestPos, false));
        ((BackpackItem) pack.getItem()).useBeforeBlock(pack, context);
        h.assertTrue(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 16, "Sneak-use deposits backpack contents into a real chest");
        h.assertTrue(new BackpackInventory(player, 15, pack).isEmpty(), "Chest transfer persisted the empty backpack");
        chest.setItem(1, new ItemStack(Items.APPLE, 3));
        BackpackMode.RECEIVE.write(pack, CoreDataComponents.BACKPACK_MODE.get());
        ((BackpackItem) pack.getItem()).useBeforeBlock(pack, context);
        h.assertTrue(chest.getItem(0).isEmpty() && chest.getItem(1).getCount() == 3
            && new BackpackInventory(player, 15, pack).getItem(0).getCount() == 16, "Receive mode retrieves accepted items and leaves rejected food");
        player.setShiftKeyDown(false);
        player.getInventory().clearContent();
        h.succeed();
    }

}
