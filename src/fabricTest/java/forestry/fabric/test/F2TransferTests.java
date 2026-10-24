package forestry.fabric.test;

import forestry.api.core.fluids.FluidUnits;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.content.energy.EnergyHelper;
import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import forestry.core.content.energy.blocks.EngineBlock;
import forestry.core.content.energy.tiles.EngineBlockEntity;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.inventory.InventoryPlain;
import forestry.core.platform.inventory.InventoryTransferView;
import forestry.core.platform.util.InventoryData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static forestry.fabric.test.F1RegistrationTests.require;

public final class F2TransferTests {
    private record EnergyLocation(Level level, BlockPos pos) {}

    private static final class ExternalEnergy extends SimpleEnergyStorage {
        private static final Map<EnergyLocation, ExternalEnergy> STORAGES = new HashMap<>();
        static {
            EnergyStorage.SIDED.registerForBlocks((level, pos, state, entity, side) ->
                side == null || side == Direction.WEST ? STORAGES.get(new EnergyLocation(level, pos)) : null, Blocks.LODESTONE);
        }
        private int insertions;
        private int commits;

        private ExternalEnergy(long capacity, long maxInsert, long maxExtract) {
            super(capacity, maxInsert, maxExtract);
        }

        @Override public long insert(long maxAmount, TransactionContext transaction) {
            insertions++;
            return super.insert(maxAmount, transaction);
        }

        @Override protected void onFinalCommit() { commits++; }
    }

    @GameTest public void externalEnergySourcePushesOneToOneIntoForestry(GameTestHelper test) {
        var machine = (forestry.core.platform.tile.TilePowered) F2MachineTests.place(test.getLevel(),
            test.absolutePos(new BlockPos(1, 1, 1)), "squeezer");
        var destination = EnergyStorage.SIDED.find(test.getLevel(), machine.getBlockPos(), Direction.WEST);
        require(destination != null && destination.supportsInsertion() && !destination.supportsExtraction(), "Powered machine must expose an input");
        var source = new ExternalEnergy(1000, 0, 1000);
        source.amount = 400;
        var notifications = new AtomicInteger();
        machine.getEnergyManager().onChange(notifications::incrementAndGet);
        try (var outer = Transaction.openOuter()) {
            require(EnergyStorageUtil.move(source, destination, 173, outer) == 173, "External E source did not push into Forestry");
            require(destination.getAmount() == 173 && source.amount == 227, "1 E must equal 1 FE inside the transaction");
            require(source.commits == 0 && notifications.get() == 0, "Nested transfer notified before outer commit");
        }
        require(destination.getAmount() == 0 && source.amount == 400 && source.commits == 0 && notifications.get() == 0,
            "Parent abort did not restore both mods' energy");
        require(EnergyStorageUtil.move(source, destination, 173, null) == 173, "Committed external push failed");
        require(destination.getAmount() == 173 && source.amount == 227 && source.commits == 1 && notifications.get() == 1,
            "Committed transfer changed units or notified more than once");
        try (var transaction = Transaction.openOuter()) {
            require(destination.extract(Long.MAX_VALUE, transaction) == 0, "External extraction bypassed the machine input boundary");
        }
        test.succeed();
    }

    @GameTest public void enginePushesToExternalSidedBlockStorage(GameTestHelper test) {
        var level = test.getLevel();
        var enginePos = test.absolutePos(new BlockPos(1, 1, 1));
        var engine = (EngineBlockEntity) F2MachineTests.place(level, enginePos, "peat_engine");
        level.setBlockAndUpdate(enginePos, engine.getBlockState().setValue(EngineBlock.VERTICAL_FACING, Direction.EAST));
        engine = (EngineBlockEntity) level.getBlockEntity(enginePos);
        require(engine != null, "Engine missing after rotation");
        var source = engine.getEnergyManager();
        source.setEnergyStored(1000);
        var changes = new AtomicInteger();
        source.onChange(changes::incrementAndGet);
        require(EnergyStorage.SIDED.find(level, enginePos, null) == source, "Null-side lookup must expose the full engine storage");
        for (var side : Direction.values()) {
            require((EnergyStorage.SIDED.find(level, enginePos, side) != null) == (side == Direction.EAST), "Engine exposed an incorrect output face");
        }
        require(!source.supportsInsertion() && source.supportsExtraction(), "Engine must remain output-only");
        var target = enginePos.east();
        level.setBlockAndUpdate(target, Blocks.LODESTONE.defaultBlockState());
        require(level.getBlockEntity(target) == null, "External fixture must exercise a block without a block entity");
        var receiver = new ExternalEnergy(500, 137, 0);
        var location = new EnergyLocation(level, target);
        ExternalEnergy.STORAGES.put(location, receiver);
        try {
            receiver.amount = receiver.capacity;
            require(EnergyHelper.isEnergyReceiverOrEngine(Direction.WEST, level, target), "Full receivers must retain their connection");
            require(!EnergyHelper.isEnergyReceiverOrEngine(Direction.EAST, level, target), "Receiver accepted its disabled face");
            require(receiver.insertions == 0, "Connection discovery must not attempt even a simulated insertion");
            receiver.amount = 0;
            require(EnergyHelper.sendEnergy(source, Direction.WEST, level, target, 1000, false) == 0, "Sender queried the wrong receiving face");
            require(EnergyHelper.canSendEnergy(source, Direction.EAST, level, target), "External block receiver was not found");
            require(source.getAmount() == 1000 && receiver.amount == 0 && receiver.commits == 0 && changes.get() == 0,
                "Transfer simulation mutated storage or notified either owner");
            try (var outer = Transaction.openOuter()) {
                require(EnergyHelper.sendEnergy(source, Direction.EAST, level, target, 1000, false) == 137, "Receiver's transfer limit was ignored");
            }
            require(source.getAmount() == 1000 && receiver.amount == 0 && receiver.commits == 0 && changes.get() == 0,
                "Aborted external transfer lost energy or notified an owner");
            require(EnergyHelper.sendEnergy(source, Direction.EAST, level, target, 1000, false) == 137, "Engine failed to push to an external receiver");
            require(source.getAmount() == 863 && receiver.amount == 137 && receiver.commits == 1 && changes.get() == 1,
                "Committed engine output must conserve FE/E and notify once");
            engine.stagePiston = 1;
            engine.serverTick(level, enginePos, engine.getBlockState());
            require(receiver.amount == 274, "Engine tick failed to push into a receiver without a block entity");
            receiver.amount = 490;
            require(EnergyHelper.sendEnergy(source, Direction.EAST, level, target, 1000, false) == 10 && receiver.amount == 500,
                "Near-full receiver did not accept exactly its remaining space");
            long remaining = source.getAmount();
            require(EnergyHelper.sendEnergy(source, Direction.EAST, level, target, 1000, false) == 0 && source.getAmount() == remaining,
                "Full receiver consumed source energy");
            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            require(EnergyHelper.sendEnergy(source, Direction.EAST, level, target, 1000, false) == 0 && source.getAmount() == remaining,
                "Removed receiver retained stale storage");
        } finally {
            ExternalEnergy.STORAGES.remove(location);
        }
        test.succeed();
    }

    @GameTest public void energyApiLongAmountsRemainBounded(GameTestHelper test) {
        var energy = new ForestryEnergyStorage(Long.MAX_VALUE, Long.MAX_VALUE, EnergyTransferMode.BOTH);
        try (var transaction = Transaction.openOuter()) {
            require(energy.insert(Long.MAX_VALUE, transaction) == Long.MAX_VALUE, "Long energy amount was truncated");
            require(energy.insert(Long.MAX_VALUE, transaction) == 0, "Full long storage overflowed");
            require(energy.extract(Long.MAX_VALUE, transaction) == Long.MAX_VALUE && energy.getAmount() == 0, "Long extraction changed units");
            try { energy.insert(-1, transaction); throw new AssertionError("Negative insertion accepted"); }
            catch (IllegalArgumentException expected) {}
            try { energy.extract(-1, transaction); throw new AssertionError("Negative extraction accepted"); }
            catch (IllegalArgumentException expected) {}
            transaction.commit();
        }
        energy.setEnergyStored(Long.MAX_VALUE - 1);
        energy.generateEnergy(Long.MAX_VALUE);
        require(energy.getAmount() == Long.MAX_VALUE, "Internal generation overflowed");
        energy.drainEnergy(Long.MAX_VALUE);
        require(energy.getAmount() == 0, "Internal drain underflowed");
        test.succeed();
    }

    @GameTest public void bottlingOutputFailureAndParentAbortConserveFluid(GameTestHelper test) {
        var inventory = new InventoryPlain(2, "bottler", 64);
        inventory.setItem(0, new ItemStack(Items.BUCKET, 2));
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 64));
        var tank = new StandardTank(2000);
        tank.setFluid(FluidVolume.milliBuckets(Fluids.WATER, 2000));
        require(forestry.core.platform.fluids.FluidHelper.fillContainers(tank, inventory, 0, 1, Fluids.WATER, true) == forestry.core.platform.fluids.FluidHelper.FillStatus.NO_SPACE, "Full output should reject bottling");
        require(tank.getAmount() == FluidConstants.BUCKET * 2 && inventory.getItem(0).getCount() == 2, "Failed bottling lost resources");
        inventory.setItem(1, ItemStack.EMPTY);
        try (var tx = Transaction.openOuter()) {
            require(forestry.core.platform.fluids.FluidHelper.fillContainers(tank, inventory, 0, 1, Fluids.WATER, tx) == forestry.core.platform.fluids.FluidHelper.FillStatus.SUCCESS, "Bucket fill failed");
        }
        require(tank.getAmount() == FluidConstants.BUCKET * 2 && inventory.getItem(0).getCount() == 2 && inventory.getItem(1).isEmpty(), "Parent abort did not undo bucket conversion");
        require(forestry.core.platform.fluids.FluidHelper.fillContainers(tank, inventory, 0, 1, Fluids.WATER, true) == forestry.core.platform.fluids.FluidHelper.FillStatus.SUCCESS, "Committed bottling failed");
        require(inventory.getItem(1).is(Items.WATER_BUCKET) && tank.getAmount() == FluidConstants.BUCKET && inventory.getItem(0).getCount() == 1, "Bottling changed fluid units");
        require(forestry.core.platform.fluids.FluidHelper.drainContainers(tank, inventory, 1, 0, true) == forestry.core.platform.fluids.FluidHelper.FillStatus.SUCCESS, "Bucket draining failed");
        require(tank.getAmount() == FluidConstants.BUCKET * 2 && inventory.getItem(0).getCount() == 2 && inventory.getItem(1).isEmpty(), "Bucket round-trip not conserved");
        test.succeed();
    }

    @GameTest public void multiTankMalformedSaveAndPacketApplyAtomically(GameTestHelper test) {
        var first = new StandardTank(1000); var second = new StandardTank(2000);
        first.setFluid(FluidVolume.milliBuckets(Fluids.WATER, 500)); second.setFluid(FluidVolume.milliBuckets(Fluids.LAVA, 1500));
        var manager = new forestry.core.platform.fluids.TankManager(null, first, second);
        var saved = manager.write(new CompoundTag(), test.getLevel().registryAccess());
        var bad = saved.copy(); bad.getList("tanks").orElseThrow().getCompound(1).orElseThrow().putLong("amount", Long.MAX_VALUE);
        try { manager.read(bad, test.getLevel().registryAccess()); throw new AssertionError("Oversized second tank accepted"); }
        catch (IllegalArgumentException expected) {}
        require(saved.equals(manager.write(new CompoundTag(), test.getLevel().registryAccess())), "Malformed second tank partially changed the first");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), test.getLevel().registryAccess());
        try {
            FluidVolume.STREAM_CODEC.encode(buffer, FluidVolume.milliBuckets(Fluids.WATER, 1)); buffer.writeVarLong(-1);
            try { manager.readData(buffer); throw new AssertionError("Negative tank packet accepted"); } catch (IllegalArgumentException expected) {}
            require(saved.equals(manager.write(new CompoundTag(), test.getLevel().registryAccess())), "Malformed packet partially changed tanks");
        } finally { buffer.release(); }
        var empty = new forestry.core.platform.fluids.TankManager(null, new StandardTank(1000), new StandardTank(2000));
        empty.read(saved, test.getLevel().registryAccess());
        require(saved.equals(empty.write(new CompoundTag(), test.getLevel().registryAccess())), "Multi-tank persistence changed amounts");
        test.succeed();
    }

    @GameTest public void adjacentLookupDoesNotRetainReplacedInventories(GameTestHelper test) {
        var sourcePos = new net.minecraft.core.BlockPos(1, 1, 1); var targetPos = sourcePos.east();
        test.setBlock(sourcePos, net.minecraft.world.level.block.Blocks.BARREL);
        test.setBlock(targetPos, net.minecraft.world.level.block.Blocks.BARREL);
        var source = test.getBlockEntity(sourcePos, net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        var cache = new forestry.core.platform.inventory.AdjacentInventoryCache(source, new forestry.core.platform.tile.AdjacentTileCache(source));
        var original = cache.getAdjacentInventory(net.minecraft.core.Direction.EAST);
        require(original != null, "Native barrel lookup unavailable");
        test.setBlock(targetPos, net.minecraft.world.level.block.Blocks.AIR);
        require(cache.getAdjacentInventory(net.minecraft.core.Direction.EAST) == null, "Removed inventory remained cached");
        test.setBlock(targetPos, net.minecraft.world.level.block.Blocks.CHEST);
        var replacement = cache.getAdjacentInventory(net.minecraft.core.Direction.EAST);
        require(replacement != null && replacement != original, "Replacement inventory reused stale provider");
        source.setRemoved();
        require(cache.getAdjacentInventory(net.minecraft.core.Direction.EAST) == null, "Removed source can still access adjacent storage");
        test.succeed();
    }

    @GameTest
    public void childCommitParentAbortRestoresEveryResource(GameTestHelper test) {
        var inventory = new InventoryPlain(2, "items", 64);
        inventory.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
        var tank = new StandardTank(1000);
        tank.setFluid(FluidVolume.milliBuckets(Fluids.WATER, 250));
        var energy = new ForestryEnergyStorage(1000, 1000, EnergyTransferMode.BOTH);
        energy.setEnergyStored(400);
        var notifications = new AtomicInteger();
        inventory.addCommitListener(slots -> notifications.incrementAndGet());
        tank.setTankUpdateHandler(value -> notifications.incrementAndGet());
        energy.onChange(notifications::incrementAndGet);
        var first = InventoryTransferView.internal(inventory);
        var second = InventoryTransferView.internal(inventory);
        try (var outer = Transaction.openOuter()) {
            require(first.extract(ItemVariant.of(Items.IRON_INGOT), 3, outer) == 3, "First view extraction");
            try (var inner = outer.openNested()) {
                require(second.extract(ItemVariant.of(Items.IRON_INGOT), 2, inner) == 2, "Shared view extraction");
                require(tank.extract(FluidVariant.of(Fluids.WATER), 81, inner) == 81, "Fluid extraction");
                require(energy.extract(75, inner) == 75, "Energy extraction");
                inner.commit();
            }
            require(notifications.get() == 0, "Notifications escaped an open outer transaction");
        }
        require(inventory.getItem(0).getCount() == 8 && tank.getAmount() == FluidUnits.fromMilliBuckets(250)
            && energy.getAmount() == 400 && notifications.get() == 0, "Outer rollback did not conserve resources");
        test.succeed();
    }

    @GameTest
    public void finalNotificationsAndEnergyDirection(GameTestHelper test) {
        var energy = new ForestryEnergyStorage(100, 1000, EnergyTransferMode.RECEIVE);
        var calls = new AtomicInteger();
        energy.onChange(calls::incrementAndGet);
        try (var outer = Transaction.openOuter()) {
            require(energy.insert(Long.MAX_VALUE, outer) == 100, "Receive rate changed");
            try (var inner = outer.openNested()) { energy.insert(100, inner); inner.commit(); }
            require(energy.extract(1, outer) == 0 && !energy.supportsExtraction(), "Receive-only boundary bypassed");
            require(calls.get() == 0, "Premature change notification");
            outer.commit();
        }
        require(energy.getAmount() == 200 && calls.get() == 1, "Final commit must notify once");
        try (var outer = Transaction.openOuter()) {
            energy.insert(10, outer);
            energy.extractInternal(10, outer);
            outer.commit();
        }
        require(calls.get() == 1, "A net-zero transaction dirtied the owner");
        energy.generateEnergy(Long.MAX_VALUE);
        require(energy.getAmount() == 1000, "Energy overflow or changed capacity");
        test.succeed();
    }

    @GameTest
    public void legacyFluidAndFractionalPersistence(GameTestHelper test) {
        var registries = test.getLevel().registryAccess();
        var legacy = new CompoundTag();
        legacy.putString("id", "minecraft:water");
        legacy.putInt("amount", 1000);
        var tank = new StandardTank(1000);
        tank.readFromNBT(registries, legacy);
        require(tank.getAmount() == FluidConstants.BUCKET, "Legacy mB amount changed meaning");
        try (var tx = Transaction.openOuter()) { tank.extract(FluidVariant.of(Fluids.WATER), 1, tx); tx.commit(); }
        var encoded = tank.writeToNBT(registries, new CompoundTag());
        require(encoded.getIntOr("schema", 0) == 2 && encoded.getStringOr("unit", "").equals("droplet"), "New fluid save lacks schema/unit");
        var loaded = new StandardTank(1000);
        loaded.readFromNBT(registries, encoded);
        require(loaded.getAmount() == FluidConstants.BUCKET - 1, "Fractional mB was truncated");
        var bad = encoded.copy(); bad.putInt("schema", 999);
        try { loaded.readFromNBT(registries, bad); throw new AssertionError("Unknown schema accepted"); }
        catch (IllegalStateException | IllegalArgumentException expected) { }
        require(loaded.getAmount() == FluidConstants.BUCKET - 1, "Failed decode partially replaced a tank");
        try { FluidUnits.fromMilliBuckets(Long.MAX_VALUE); throw new AssertionError("Overflow accepted"); }
        catch (ArithmeticException expected) { }
        test.succeed();
    }

    @GameTest
    public void actualBucketContextRollbackAndCommit(GameTestHelper test) {
        var inventory = new InventoryPlain(1, "items", 64);
        inventory.setItem(0, new ItemStack(Items.WATER_BUCKET));
        var context = ContainerItemContext.ofSingleSlot(inventory.getTransferHandler().getSlot(0));
        var fluid = context.find(FluidStorage.ITEM);
        require(fluid != null, "Vanilla water bucket FluidStorage lookup failed");
        try (var tx = Transaction.openOuter()) {
            require(fluid.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET, "Bucket extraction failed");
        }
        require(inventory.getItem(0).is(Items.WATER_BUCKET), "Aborted container conversion lost its water bucket");
        try (var tx = Transaction.openOuter()) {
            var current = context.find(FluidStorage.ITEM);
            require(current != null && current.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx) == FluidConstants.BUCKET, "Retry failed");
            tx.commit();
        }
        require(inventory.getItem(0).is(Items.BUCKET) && inventory.getItem(0).getCount() == 1, "Container conversion duplicated/lost the empty bucket");
        test.succeed();
    }

    @GameTest
    public void inventoryPersistenceIsAtomic(GameTestHelper test) {
        var inventory = new InventoryPlain(2, "items", 64);
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 3));
        var saved = inventory.write(new CompoundTag(), test.getLevel().registryAccess());
        var reloaded = new InventoryPlain(2, "items", 64);
        reloaded.read(saved, test.getLevel().registryAccess());
        require(ItemStack.matches(inventory.getItem(0), reloaded.getItem(0)), "Inventory round trip changed contents");
        var broken = saved.copy();
        var list = broken.getList("items").orElseThrow();
        var duplicate = list.getCompound(0).orElseThrow().copy();
        list.add(duplicate);
        try { reloaded.read(broken, test.getLevel().registryAccess()); throw new AssertionError("Duplicate slot accepted"); }
        catch (IllegalArgumentException expected) { }
        require(reloaded.getItem(0).getCount() == 3, "Invalid inventory partially applied");
        reloaded.read(new CompoundTag(), test.getLevel().registryAccess());
        require(reloaded.isEmpty(), "Missing saved slots retained stale inventory");
        test.succeed();
    }
}
