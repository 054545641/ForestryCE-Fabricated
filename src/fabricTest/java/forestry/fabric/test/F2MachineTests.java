package forestry.fabric.test;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import forestry.api.core.fluids.FluidUnits;

import forestry.core.content.energy.EnergyHelper;
import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import forestry.core.content.machines.tiles.TileStill;
import forestry.core.content.machines.tiles.TileCarpenter;
import forestry.core.content.machines.tiles.TileFermenter;
import forestry.core.content.machines.tiles.TileSqueezer;
import forestry.core.content.machines.tiles.TileCentrifuge;
import forestry.core.content.machines.tiles.TileSmelter;
import forestry.core.content.machines.tiles.TileFabricator;
import forestry.core.content.machines.tiles.TileBottler;
import forestry.core.platform.tile.TilePowered;
import forestry.core.content.analyzer.TileAnalyzer;
import forestry.core.platform.util.SpeciesUtil;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.util.ValueIOUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import forestry.api.core.fluids.FluidVolume;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** F2 ports of the N3 resource-conservation regressions, using real registered machines and Fabric transactions. */
public final class F2MachineTests {
	@GameTest public void analyzer(GameTestHelper h) {
		TileAnalyzer machine = machine(h, "analyzer", 1);
		ItemStack specimen = SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createStack(BeeLifeStage.DRONE);
		machine.setItem(2, specimen);
		machine.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(BuiltInRegistries.FLUID.getValue(Identifier.parse("forestry:honey")), 500));
		h.assertTrue(machine.hasWork(), "Analyzer staged specimen");
		abort(h, machine, () -> h.assertTrue(machine.workCycle(), "Analyzer nested conversion"));
		for (int i = 8; i < 12; i++) machine.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
		var before = machine.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!machine.workCycle() && before.equals(machine.saveCustomOnly(h.getLevel().registryAccess())), "Analyzer full output rolls back honey and analysis component");
		ItemStack analyzed = specimen.copy();
		analyzed.set(CoreDataComponents.ANALYZED.get(), true);
		analyzed.setCount(63);
		machine.setItem(8, analyzed);
		h.assertTrue(machine.hasWork() && machine.workCycle(), "Prospective analyzed stack fits existing analyzed output");
		h.assertTrue(machine.getItem(8).getCount() == 64 && machine.getItem(0).isEmpty() && machine.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(400), "Analyzer exact conversion");
		h.succeed();
	}

    static net.minecraft.world.level.block.entity.BlockEntity place(net.minecraft.server.level.ServerLevel level, BlockPos pos, String path) {
        var block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("forestry", path));
        if (block == net.minecraft.world.level.block.Blocks.AIR) throw new AssertionError("Missing real machine " + path);
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        return java.util.Objects.requireNonNull(level.getBlockEntity(pos));
    }
	@GameTest public void upgradedProgress(GameTestHelper h) {
		var circuits = forestry.api.IForestryApi.INSTANCE.getCircuitManager();
		var board = forestry.core.engine.circuits.ItemCircuitBoard.createCircuitboard(forestry.core.engine.circuits.EnumCircuitBoardType.BASIC,
			circuits.getLayout(forestry.api.core.circuits.ForestryCircuitLayouts.MACHINE_UPGRADE),
			new forestry.api.core.circuits.ICircuit[]{circuits.getCircuit("machine.speed.boost.1")});
		String[] ids = {"smelter", "squeezer", "centrifuge"};
		for (int i = 0; i < ids.length; i++) {
			TilePowered machine = machine(h, ids[i], 1);
			((forestry.core.engine.circuits.ISocketable) machine).setSocket(0, board.copy());
			machine.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 4));
			machine.getEnergyManager().setEnergyStored(10000);
			h.assertTrue(machine.processWorkStep() && machine.getWorkCounter() == 1, "Upgraded machine pays first step: " + ids[i]);
			var saved = machine.saveCustomOnly(h.getLevel().registryAccess());
			TilePowered restored = machine(h, ids[i], 2);
			ValueIOUtil.readTag(saved, h.getLevel().registryAccess(), restored::loadCustomOnly);
			ValueIOUtil.readTag(saved, h.getLevel().registryAccess(), restored::loadCustomOnly);
			h.assertTrue(restored.getWorkCounter() == 1 && restored.getStepsPerWorkCycle() == machine.getStepsPerWorkCycle()
				&& restored.getEnergyPerWorkCycle() == machine.getEnergyPerWorkCycle(), "Repeated load preserves progress and does not stack upgrades: " + ids[i]);
		}
		h.succeed();
	}

	static <T extends TilePowered> T machine(GameTestHelper h, String id, int x) {
		return (T) F2MachineTests.place(h.getLevel(), h.absolutePos(new BlockPos(x, 2, 1)), id);
	}

	static void abort(GameTestHelper h, TilePowered machine, Runnable operation) {
		var before = machine.saveCustomOnly(h.getLevel().registryAccess());
		try (var transaction = Transaction.openOuter()) { operation.run(); }
		h.assertTrue(before.equals(machine.saveCustomOnly(h.getLevel().registryAccess())), "Parent rollback restores complete machine state: " + machine.getClass().getSimpleName());
	}

	@GameTest public void progress(GameTestHelper h) {
		TileStill still = machine(h, "still", 1);
		still.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 72));
		still.getEnergyManager().setEnergyStored(800);
		abort(h, still, () -> h.assertTrue(still.processWorkStep(), "Step can be simulated"));
		h.assertTrue(still.processWorkStep() && still.processWorkStep(), "Two paid steps");
		h.assertTrue(still.getWorkCounter() == 2 && still.getEnergyManager().getEnergyStored() == 400, "Energy and progress advance together");
		TileStill restored = machine(h, "still", 2);
		ValueIOUtil.readTag(still.saveCustomOnly(h.getLevel().registryAccess()), h.getLevel().registryAccess(), restored::loadCustomOnly);
		restored.getEnergyManager().setEnergyStored(0);
		var paused = restored.saveCustomOnly(h.getLevel().registryAccess());
		for (int i = 0; i < 8; i++) h.assertTrue(!restored.processWorkStep(), "No work without energy");
		h.assertTrue(paused.equals(restored.saveCustomOnly(h.getLevel().registryAccess())), "Power loss preserves paid progress and resources");
		restored.getEnergyManager().setEnergyStored(400);
		h.assertTrue(restored.processWorkStep(), "Resume third step");
		abort(h, restored, () -> h.assertTrue(restored.processWorkStep(), "Final step with inputs, output and energy can roll back"));
		h.assertTrue(restored.processWorkStep(), "Finish resumed batch");
		h.assertTrue(restored.getWorkCounter() == 0 && restored.getEnergyManager().getEnergyStored() == 0
			&& restored.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(36) && restored.getTankManager().getTank(1).getFluidAmount() == FluidUnits.fromMilliBuckets(12), "Restart consumes exactly remaining energy and one batch");
		h.succeed();
	}

	@GameTest public void carpenter(GameTestHelper h) {
		TileCarpenter machine = machine(h, "carpenter", 1);
		machine.getCraftingInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD));
		machine.getCraftingInventory().setItem(1, new ItemStack(Items.AMETHYST_SHARD));
		machine.setItem(9, new ItemStack(Items.CHEST, 4));
		machine.setItem(12, new ItemStack(Items.AMETHYST_SHARD, 8));
		machine.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 200));
		machine.checkRecipe(h.getLevel().registryAccess());
		h.assertTrue(machine.hasWork(), "Carpenter fixture recipe available");
		abort(h, machine, () -> h.assertTrue(machine.workCycle(), "Carpenter nested conversion"));
		machine.setItem(10, new ItemStack(Items.COBBLESTONE, 64));
		var full = machine.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!machine.workCycle() && full.equals(machine.saveCustomOnly(h.getLevel().registryAccess())), "Late full output restores fluid, box and ingredients");
		machine.setItem(10, ItemStack.EMPTY);
		h.assertTrue(machine.workCycle() && machine.getItem(10).getCount() == 2 && machine.getItem(12).getCount() == 6
			&& machine.getItem(9).getCount() == 3 && machine.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(100), "Carpenter exact committed quantities");
		h.succeed();
	}

	@GameTest public void fermenter(GameTestHelper h) {
		TileFermenter machine = machine(h, "fermenter", 1);
		machine.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 2));
		machine.setItem(1, new ItemStack(Items.COAL, 3));
		machine.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 1000));
		h.assertTrue(machine.hasWork() && machine.getItem(0).getCount() == 2 && machine.getItem(1).getCount() == 3, "Fermenter readiness preserves inputs and fuel");
		abort(h, machine, machine::workCycle);
		machine.getTankManager().getTank(1).setFluid(FluidVolume.milliBuckets(Fluids.LAVA, 10000));
		var full = machine.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!machine.workCycle() && full.equals(machine.saveCustomOnly(h.getLevel().registryAccess())), "Full tank restores newly reserved fermentable and fuel");
		machine.getTankManager().getTank(1).setFluid(FluidVolume.EMPTY);
		machine.getEnergyManager().setEnergyStored(4200);
		for (int i = 0; i < 4; i++) h.assertTrue(machine.processWorkStep(), "Fermenter paid step " + i);
		h.assertTrue(machine.getItem(0).getCount() == 1 && machine.getItem(1).getCount() == 2 && machine.getTankManager().getTank(1).getFluidAmount() == FluidUnits.fromMilliBuckets(200), "One resource/fuel reserved for an active batch");
		TileFermenter restored = machine(h, "fermenter", 2);
		ValueIOUtil.readTag(machine.saveCustomOnly(h.getLevel().registryAccess()), h.getLevel().registryAccess(), restored::loadCustomOnly);
		restored.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.LAVA, 900));
		var wrongFluid = restored.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!restored.processWorkStep() && wrongFluid.equals(restored.saveCustomOnly(h.getLevel().registryAccess())), "Active fermenter batch cannot consume a replaced, incompatible fluid");
		restored.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 900));
		h.assertTrue(restored.processWorkStep() && restored.processWorkStep(), "Resume already-paid fermenter batch");
		h.assertTrue(restored.getItem(0).getCount() == 1 && restored.getItem(1).getCount() == 2 && restored.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(700)
			&& restored.getTankManager().getTank(1).getFluidAmount() == FluidUnits.fromMilliBuckets(600) && restored.getEnergyManager().getEnergyStored() == 0, "Active recipe and counters resume without consuming another item or energy charge");
		h.succeed();
	}

	@GameTest public void squeezer(GameTestHelper h) {
		TileSqueezer machine = machine(h, "squeezer", 1);
		machine.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 3));
		h.assertTrue(machine.hasWork(), "Squeezer ready");
		abort(h, machine, () -> h.assertTrue(machine.workCycle(), "Squeezer nested conversion"));
		machine.setItem(9, new ItemStack(Items.COBBLESTONE, 64));
		var before = machine.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!machine.workCycle() && before.equals(machine.saveCustomOnly(h.getLevel().registryAccess())), "Remnant rejection restores input, fluid and random state");
		machine.setItem(9, ItemStack.EMPTY);
		h.assertTrue(machine.workCycle() && machine.getItem(0).getCount() == 2 && machine.getItem(9).getCount() == 1 && machine.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(125), "Squeezer exact output");
		h.succeed();
	}

	@GameTest public void centrifuge(GameTestHelper h) {
		TileCentrifuge machine = machine(h, "centrifuge", 1);
		machine.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 3));
		h.assertTrue(machine.hasWork(), "Centrifuge ready");
		abort(h, machine, () -> h.assertTrue(machine.workCycle(), "Centrifuge nested conversion"));
		for (int i = 1; i < 10; i++) machine.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
		machine.setItem(1, ItemStack.EMPTY);
		var before = machine.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!machine.workCycle() && before.equals(machine.saveCustomOnly(h.getLevel().registryAccess())), "Second product failure restores first insertion and input");
		machine.setItem(2, ItemStack.EMPTY);
		h.assertTrue(machine.workCycle() && machine.getItem(0).getCount() == 2 && machine.getItem(1).is(Items.DIAMOND) && machine.getItem(2).is(Items.EMERALD), "Both centrifuge products commit once");
		h.succeed();
	}

	@GameTest public void smelter(GameTestHelper h) {
		TileSmelter machine = machine(h, "smelter", 1);
		machine.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 4));
		machine.getEnergyManager().setEnergyStored(10000);
		machine.setItem(10, new ItemStack(Items.COBBLESTONE, 64));
		var before = machine.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!machine.processWorkStep() && before.equals(machine.saveCustomOnly(h.getLevel().registryAccess())), "Full smelter preserves input and energy");
		machine.setItem(10, ItemStack.EMPTY);
		abort(h, machine, () -> { for (int i = 0; i < 8 && machine.getItem(10).isEmpty(); i++) machine.processWorkStep(); });
		for (int i = 0; i < 8 && machine.getItem(10).isEmpty(); i++) machine.processWorkStep();
		h.assertTrue(machine.getItem(0).getCount() == 2 && machine.getItem(10).getCount() == 2, "Smelter commits full sized ingredients/output");
		h.succeed();
	}

	@GameTest public void bottler(GameTestHelper h) {
		TileBottler machine = machine(h, "bottler", 1);
		machine.setItem(3, new ItemStack(Items.BUCKET));
		machine.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 2000));
		h.assertTrue(machine.hasWork(), "Real bucket filling recipe available");
		abort(h, machine, () -> h.assertTrue(machine.workCycle(), "Bottler nested conversion"));
		machine.setItem(5, new ItemStack(Items.COBBLESTONE, 64));
		var before = machine.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!machine.workCycle() && before.equals(machine.saveCustomOnly(h.getLevel().registryAccess())), "Bottler full output keeps bucket/fluid");
		machine.setItem(5, ItemStack.EMPTY);
		h.assertTrue(machine.workCycle() && machine.getItem(5).is(Items.WATER_BUCKET) && machine.getItem(3).isEmpty() && machine.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(1000), "Bucket fill commits once");
		h.succeed();
	}

	@GameTest public void fabricator(GameTestHelper h) {
		TileFabricator machine = machine(h, "thermionic_fabricator", 1);
		machine.getCraftingInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD));
		machine.getCraftingInventory().setItem(1, new ItemStack(Items.AMETHYST_SHARD));
		machine.setItem(3, new ItemStack(Items.AMETHYST_SHARD, 4));
		machine.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.LAVA, 200));
		abort(h, machine, () -> h.assertTrue(machine.workCycle(), "Fabricator heat and conversion are transactional"));
		machine.setItem(2, new ItemStack(Items.COBBLESTONE, 64));
		machine.workCycle();
		h.assertTrue(machine.getItem(3).getCount() == 4 && machine.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(200), "Blocked fabrication retains all crafting resources while retaining paid heat");
		machine.setItem(2, ItemStack.EMPTY);
		h.assertTrue(machine.workCycle() && machine.getItem(2).getCount() == 2 && machine.getItem(3).getCount() == 2 && machine.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(100), "Fabricator exact conversion");
		h.succeed();
	}
	@GameTest public void energyRollback(GameTestHelper h) {
		int[] notifications = {0};
		var energy = new ForestryEnergyStorage(1000, 10000, EnergyTransferMode.RECEIVE).onChange(() -> notifications[0]++);
		energy.setEnergyStored(1000);
		notifications[0] = 0;
		try (var outer = Transaction.openOuter()) {
			h.assertTrue(EnergyHelper.consumeEnergyToDoWork(energy, 4, 400), "Work step draws energy");
			h.assertTrue(energy.getEnergyStored() == 900, "Energy visible inside transaction");
		}
		h.assertTrue(energy.getEnergyStored() == 1000 && notifications[0] == 0, "Aborted work step restores energy without dirty callback");
		h.succeed();
	}

	@GameTest public void stillReadiness(GameTestHelper h) {
		var still = (TileStill) F2MachineTests.place(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1)), "still");
		still.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 36));
		var before = still.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(still.hasWork(), "Deterministic still recipe available");
		h.assertTrue(still.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(36) && still.getTankManager().getTank(1).isEmpty(), "Readiness does not reserve or consume resources");
		still.getTankManager().getTank(1).setFluid(FluidVolume.milliBuckets(Fluids.LAVA, 10000));
		before = still.saveCustomOnly(h.getLevel().registryAccess());
		h.assertTrue(!still.hasWork(), "Full output tank blocks work");
		h.assertTrue(before.equals(still.saveCustomOnly(h.getLevel().registryAccess())), "Full output keeps all inputs and energy");
		h.succeed();
	}

	@GameTest public void stillOuterRollback(GameTestHelper h) {
		var still = (TileStill) F2MachineTests.place(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1)), "still");
		still.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 36));
		h.assertTrue(still.hasWork(), "Still ready");
		var before = still.saveCustomOnly(h.getLevel().registryAccess());
		try (var outer = Transaction.openOuter()) {
			h.assertTrue(still.workCycle(), "Still completes inside caller transaction");
		}
		h.assertTrue(before.equals(still.saveCustomOnly(h.getLevel().registryAccess())), "Caller rollback restores input, output and processing state");
		h.succeed();
	}
}
