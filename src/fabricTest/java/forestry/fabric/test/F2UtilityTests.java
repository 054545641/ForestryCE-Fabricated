package forestry.fabric.test;

import forestry.core.features.*;
import forestry.core.platform.inventory.InventoryPlain;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import static forestry.fabric.test.F1RegistrationTests.*;

public final class F2UtilityTests {
    @GameTest public void pipetteUsesHeldContextAndRetainsEveryDroplet(GameTestHelper test) {
        var inventory = new InventoryPlain(1, "pipette", 64);
        var stack = CoreUtilityItems.PIPETTE.stack();
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("F2 pipette"));
        inventory.setItem(0, stack);
        var storage = ContainerItemContext.ofSingleSlot(inventory.getTransferHandler().getSlot(0)).find(FluidStorage.ITEM);
        require(storage != null, "Pipette lookup missing");
        try (var outer = Transaction.openOuter()) {
            try (var inner = outer.openNested()) {
                require(storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET - 1, inner) == FluidConstants.BUCKET - 1, "Pipette insert rounded droplets");
                inner.commit();
            }
        }
        require(!inventory.getItem(0).has(CoreDataComponents.FLUID_CONTENT.get()), "Aborted pipette fill persisted");
        try (var tx = Transaction.openOuter()) { storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET - 1, tx); tx.commit(); }
        require(inventory.getItem(0).get(CoreDataComponents.FLUID_CONTENT.get()).amount() == FluidConstants.BUCKET - 1, "Pipette component lost precision");
        require(inventory.getItem(0).getHoverName().getString().equals("F2 pipette"), "Pipette overwrote unrelated components");
        var tag = inventory.write(new net.minecraft.nbt.CompoundTag(), test.getLevel().registryAccess());
        var restored = new InventoryPlain(1, "pipette", 64); restored.read(tag, test.getLevel().registryAccess());
        require(ItemStack.matches(inventory.getItem(0), restored.getItem(0)), "Pipette did not save");
        try (var tx = Transaction.openOuter()) { require(storage.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET - 1, "Pipette extract changed quantity"); tx.commit(); }
        require(!inventory.getItem(0).has(CoreDataComponents.FLUID_CONTENT.get()), "Empty pipette retains a fluid component");
        test.succeed();
    }

    @GameTest public void milkBucketUsesNativeStorageAndOriginalIdentifier(GameTestHelper test) {
        var milk = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(net.minecraft.resources.Identifier.parse("minecraft:milk"));
        require(milk != Fluids.EMPTY, "Legacy milk identity is missing");
        var inventory = new InventoryPlain(1, "milk", 64); inventory.setItem(0, new ItemStack(Items.MILK_BUCKET));
        var context = ContainerItemContext.ofSingleSlot(inventory.getTransferHandler().getSlot(0));
        var storage = context.find(FluidStorage.ITEM);
        require(storage != null, "Vanilla milk bucket storage is missing");
        try (var tx = Transaction.openOuter()) { require(storage.extract(FluidVariant.of(milk), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET, "Milk cannot be drained"); }
        require(inventory.getItem(0).is(Items.MILK_BUCKET), "Aborted milk conversion consumed its bucket");
        try (var tx = Transaction.openOuter()) { storage.extract(FluidVariant.of(milk), FluidConstants.BUCKET, tx); tx.commit(); }
        require(inventory.getItem(0).is(Items.BUCKET), "Milk did not return an empty bucket");
        storage = context.find(FluidStorage.ITEM);
        try (var tx = Transaction.openOuter()) { require(storage.insert(FluidVariant.of(milk), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET, "Milk cannot refill the original bucket"); tx.commit(); }
        require(inventory.getItem(0).is(Items.MILK_BUCKET), "Filled milk is not drinkable vanilla milk");
        test.succeed();
    }

    @GameTest public void nativeDamagePreservesRemnantsAndFrameStackLimit(GameTestHelper test) {
        var player = FakePlayer.get(test.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.fromString("33b9fe7c-323d-4c60-bf42-336b28a408ed"), "F2Tools"));
        player.getInventory().clearContent();
        var tool = CoreUtilityItems.SURVIVALISTS_PICKAXE.stack();
        tool.setDamageValue(tool.getMaxDamage() - 1);
        tool.hurtAndBreak(1, test.getLevel(), player, ignored -> {});
        require(tool.isEmpty() && player.getInventory().countItem(CoreUtilityItems.BROKEN_SURVIVALISTS_PICKAXE.item()) == 1, "A broken native tool did not leave exactly one remnant");
        var frame = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id("untreated_frame")));
        require(frame.getMaxStackSize() == 64 && frame.getMaxDamage() == 80, "Native frame lost the original stack or durability policy");
        var ops = test.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        frame.setCount(64);
        require(ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, frame).getOrThrow()).getOrThrow().getCount() == 64, "Stacked frames cannot round-trip");
        test.succeed();
    }
}
