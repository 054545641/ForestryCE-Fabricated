package forestry.lifecycle;

import forestry.core.content.energy.EnergyHelper;
import forestry.core.content.energy.tiles.EngineBlockEntity;
import forestry.core.content.machines.tiles.TileBottler;
import forestry.core.content.machines.tiles.TileStill;
import forestry.core.platform.util.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class N3TransferTests {
	static void farmCredit(GameTestHelper h) {
		var level = h.getLevel();
		var farm = (forestry.agriculture.minifarm.tiles.AbstractMinifarmBlockEntity) N2WorldTest.place(level, h.absolutePos(new BlockPos(1, 2, 1)), "arboretum_managed");
		farm.addPendingProduct(new ItemStack(Items.DIAMOND, 2));
		farm.getEnergyManager().setEnergyStored(10);
		var before = farm.saveCustomOnly(level.registryAccess());
		try (var tx = Transaction.openRoot()) {
			h.assertTrue(farm.processWorkStep() && farm.processWorkStep(), "Farm reaches fully paid credit");
			h.assertTrue(farm.getItem(8).isEmpty() && !farm.processWorkStep(), "World action waits for root commit and cannot be scheduled twice");
		}
		h.assertTrue(before.equals(farm.saveCustomOnly(level.registryAccess())), "Farm rollback retains pending harvest and energy");
		h.assertTrue(farm.processWorkStep() && farm.processWorkStep() && farm.getItem(8).getCount() == 2 && farm.getWorkCounter() == 0, "Committed farm delivers once");
		farm.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 500));
		try (var tx = Transaction.openRoot()) { farm.removeLiquid(new FluidStack(Fluids.WATER, 125)); h.assertTrue(farm.getTankManager().getTank(0).getFluidAmount() == 375, "Internal farm water consumption bypasses automation restriction"); }
		h.assertTrue(farm.getTankManager().getTank(0).getFluidAmount() == 500, "Farm water consumption shares native journal");
		var gearbox = (forestry.agriculture.multifarm.tiles.TileFarmGearbox) N2WorldTest.place(level, h.absolutePos(new BlockPos(4, 2, 1)), "stone_brick_farm_gearbox");
		gearbox.getEnergyManager().setEnergyStored(200);
		before = gearbox.saveCustomOnly(level.registryAccess());
		try (var tx = Transaction.openRoot()) { gearbox.updateServer(1); gearbox.updateServer(2); }
		h.assertTrue(before.equals(gearbox.saveCustomOnly(level.registryAccess())), "Gearbox progress and energy roll back together");
		gearbox.updateServer(1); gearbox.updateServer(2);
		var paid = gearbox.saveCustomOnly(level.registryAccess());
		h.assertTrue(paid.getIntOr("WorkCounter", 0) == 2 && paid.getIntOr("Energy", 0) == 100, "Gearbox persists paid progress");
		h.succeed();
	}

	static void bottlerRecovery(GameTestHelper h) {
		TileBottler source = N3MachineTests.machine(h, "bottler", 1);
		TileStill target = N3MachineTests.machine(h, "still", 2);
		source.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 2000));
		target.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 10000));
		h.runAfterDelay(30, () -> {
			h.assertTrue(source.getTankManager().getTank(0).getFluidAmount() == 2000, "Full neighbour does not drain bottler");
			target.getTankManager().getTank(0).setFluid(FluidStack.EMPTY);
			h.runAfterDelay(30, () -> {
				int delivered = target.getTankManager().getTank(0).getFluidAmount();
				h.assertTrue(delivered > 0 && delivered + source.getTankManager().getTank(0).getFluidAmount() == 2000, "Neighbour space recovery resumes actual tick transfer without block replacement");
				h.succeed();
			});
		});
	}

	static void rainmaker(GameTestHelper h) {
		var level = h.getLevel();
		level.getWeatherData().setRaining(false);
		var machine = (forestry.core.content.machines.tiles.TileMillRainmaker) N2WorldTest.place(level, h.absolutePos(new BlockPos(1, 2, 1)), "rainmaker");
		var cap = level.getCapability(Capabilities.Item.BLOCK, machine.getBlockPos(), Direction.UP);
		var fuel = ItemResource.of(BuiltInRegistries.ITEM.getValue(Identifier.parse("forestry:iodine_capsule")));
		try (var tx = Transaction.openRoot()) { h.assertTrue(cap.insert(0, fuel, 64, tx) == 1, "Rainmaker accepts one substrate"); h.assertTrue(machine.processSubstrate(), "Consume charge in caller transaction"); }
		h.assertTrue(machine.charge == 0 && machine.getItem(0).isEmpty(), "Aborted insertion and charging leave no resources or charge");
		try (var tx = Transaction.openRoot()) { h.assertTrue(cap.insert(0, fuel, 1, tx) == 1, "Insert substrate"); tx.commit(); }
		h.assertTrue(machine.processSubstrate() && machine.charge == 1 && machine.getItem(0).isEmpty(), "Committed substrate is consumed once");
		try (var tx = Transaction.openRoot()) { h.assertTrue(cap.insert(0, fuel, 1, tx) == 0, "Busy rainmaker rejects additional substrate"); }
		h.succeed();
	}

	static void capabilities(GameTestHelper h) {
		TileBottler machine = N3MachineTests.machine(h, "bottler", 1);
		var level = h.getLevel();
		var pos = machine.getBlockPos();
		var bucket = ItemResource.of(Items.BUCKET);
		for (Direction face : new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			var items = level.getCapability(Capabilities.Item.BLOCK, pos, face);
			h.assertTrue(items != null, "Real item capability " + face);
			try (var tx = Transaction.openRoot()) {
				h.assertTrue(items.insert(1, bucket, 2, tx) == 2, "Input accepts containers");
				h.assertTrue(items.extract(1, bucket, 1, tx) == 0 && items.insert(5, bucket, 1, tx) == 0, "Automation cannot extract inputs or insert outputs, including null side");
				h.assertTrue(InventoryUtil.tryAddStack(machine, new ItemStack(Items.BUCKET), 5, 1, true), "Internal output shares inventory journal");
				h.assertTrue(items.extract(5, bucket, 1, tx) == 1, "Automation extracts produced output");
			}
			h.assertTrue(machine.getItem(1).isEmpty() && machine.getItem(5).isEmpty(), "Multiple inventory views roll back together");
		}
		var before = machine.saveCustomOnly(level.registryAccess());
		try (var tx = Transaction.openRoot()) {
			h.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, pos, Direction.UP).insert(1000, tx) == 1000, "Actual energy provider");
			h.assertTrue(level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP).insert(FluidResource.of(Fluids.WATER), 1000, tx) == 1000, "Actual fluid provider");
			h.assertTrue(InventoryUtil.tryAddStack(machine, new ItemStack(Items.BUCKET), 1, 1, true), "Coupled item insertion");
		}
		h.assertTrue(before.equals(machine.saveCustomOnly(level.registryAccess())), "All three resources restore on one abort");
		int[] invalidations = {0};
		var items = BlockCapabilityCache.create(Capabilities.Item.BLOCK, level, pos, Direction.UP, () -> true, () -> invalidations[0]++);
		var fluids = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, level, pos, Direction.UP, () -> true, () -> invalidations[0]++);
		var energy = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, pos, Direction.UP, () -> true, () -> invalidations[0]++);
		h.assertTrue(items.getCapability() != null && fluids.getCapability() != null && energy.getCapability() != null, "Caches acquired");
		level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		h.assertTrue(invalidations[0] == 3 && items.getCapability() == null && fluids.getCapability() == null && energy.getCapability() == null, "Removal invalidates all native caches");
		N2WorldTest.place(level, pos, "bottler");
		h.assertTrue(items.getCapability() != null && fluids.getCapability() != null && energy.getCapability() != null, "Replacement resolves fresh providers");
		h.succeed();
	}

	static void containers(GameTestHelper h) {
		for (String id : new String[]{"forestry:can", "forestry:wax_capsule", "forestry:refractory_capsule", "forestry:pipette", "minecraft:bucket"}) {
			var inventory = new SimpleContainer(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))));
			var access = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(inventory), 0);
			var handler = access.getCapability(Capabilities.Fluid.ITEM);
			h.assertTrue(handler != null, "Registered fluid item capability: " + id);
			var empty = inventory.getItem(0).copy();
			var water = FluidResource.of(Fluids.WATER);
			try (var tx = Transaction.openRoot()) { h.assertTrue(handler.insert(water, 1000, tx) == 1000, "Fill inside abort: " + id); }
			h.assertTrue(ItemStack.matches(empty, inventory.getItem(0)), "Abort restores original components: " + id);
			try (var tx = Transaction.openRoot()) { h.assertTrue(handler.insert(water, 1000, tx) == 1000, "Fill exactly one bucket: " + id); tx.commit(); }
			h.assertTrue(FluidUtil.getFirstStackContained(inventory.getItem(0)).getAmount() == 1000, "Committed item holds exact fluid: " + id);
			var full = inventory.getItem(0).copy();
			var filledHandler = access.getCapability(Capabilities.Fluid.ITEM);
			try (var outer = Transaction.openRoot()) {
				try (var child = Transaction.open(outer)) { h.assertTrue(filledHandler.extract(water, 1000, child) == 1000, "Drain committed child: " + id); child.commit(); }
			}
			h.assertTrue(ItemStack.matches(full, inventory.getItem(0)), "Parent abort restores consumed capsule/bucket/pipette: " + id);
			try (var tx = Transaction.openRoot()) { h.assertTrue(access.getCapability(Capabilities.Fluid.ITEM).extract(water, 1000, tx) == 1000, "Drain once: " + id); tx.commit(); }
			h.assertTrue(id.endsWith("capsule") || id.endsWith(":can") ? inventory.getItem(0).isEmpty() : !inventory.getItem(0).isEmpty(), "Container consumption policy: " + id);
		}
		var wax = new SimpleContainer(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("forestry:wax_capsule"))));
		var handler = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.of(wax), 0).getCapability(Capabilities.Fluid.ITEM);
		try (var tx = Transaction.openRoot()) {
			h.assertTrue(handler.insert(FluidResource.of(Fluids.LAVA), 1000, tx) == 0, "Wax rejects hot fluid");
			h.assertTrue(handler.insert(FluidResource.of(Fluids.WATER), 500, tx) == 0, "Capsules cannot partially fill");
			tx.commit();
		}
		h.assertTrue(FluidUtil.getFirstStackContained(wax.getItem(0)).isEmpty(), "Rejected fills leave no fluid component");
		h.succeed();
	}

	static void energyTransfer(GameTestHelper h) {
		var engine = (EngineBlockEntity) N2WorldTest.place(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1)), "peat_engine");
		TileStill receiver = N3MachineTests.machine(h, "still", 2);
		engine.getEnergyManager().setEnergyStored(2000);
		try (var tx = Transaction.openRoot()) {
			h.assertTrue(EnergyHelper.sendEnergy(engine.getEnergyManager(), Direction.EAST, receiver) == 1100, "Actual receiver transfer limit");
		}
		h.assertTrue(engine.getEnergyManager().getEnergyStored() == 2000 && receiver.getEnergyManager().getEnergyStored() == 0, "Sender and receiver roll back together");
		receiver.getEnergyManager().setEnergyStored(receiver.getEnergyManager().getMaxEnergyStored() - 50);
		h.assertTrue(EnergyHelper.sendEnergy(engine.getEnergyManager(), Direction.EAST, receiver) == 50 && engine.getEnergyManager().getEnergyStored() == 1950, "Near-full receiver accepts exactly remaining space");
		h.assertTrue(EnergyHelper.sendEnergy(engine.getEnergyManager(), Direction.EAST, receiver) == 0 && engine.getEnergyManager().getEnergyStored() == 1950, "Full receiver does not consume source energy");
		h.succeed();
	}
}
