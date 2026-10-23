package forestry.fabric.test;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import forestry.api.core.fluids.FluidUnits;

import forestry.core.content.energy.tiles.*;
import forestry.core.content.machines.tiles.TileMoistener;
import forestry.core.platform.tile.TileForestry;
import forestry.core.platform.util.ValueIOUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import forestry.api.core.fluids.FluidVolume;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

public final class F2FuelTests {
	@GameTest public void climate(GameTestHelper h) {
		int[] changes = {0, 0};
		var climate = new forestry.api.core.climate.IClimateControlled() {
			public void addTemperatureChange(byte steps) { changes[0] += steps; }
			public void addHumidityChange(byte steps) { changes[1] += steps; }
		};
		var level = h.getLevel();
		String[] ids = {"alveary_heater", "alveary_fan"};
		for (int i = 0; i < ids.length; i++) {
			var tile = (forestry.apiculture.alveary.multiblock.AlvearyClimatizerBlockEntity) F2MachineTests.place(level, h.absolutePos(new BlockPos(1 + i * 3, 2, 1)), ids[i]);
			try (var tx = Transaction.openOuter()) { tile.getEnergyStorage(null).insert(50, tx); tx.commit(); }
			var before = tile.saveCustomOnly(level.registryAccess());
			changes[0] = 0;
			try (var tx = Transaction.openOuter()) { tile.changeClimate(1, climate); h.assertTrue(changes[0] == 0 && !tile.isActive(), "Climate and active display wait for commit"); }
			h.assertTrue(before.equals(tile.saveCustomOnly(level.registryAccess())) && changes[0] == 0, "Climate credit and energy restore on abort");
			tile.changeClimate(1, climate);
			h.assertTrue(tile.getEnergyStorage(null).getAmount() == 0 && changes[0] != 0, "Committed climate step spends energy once");
			var saved = tile.saveCustomOnly(level.registryAccess());
			var restored = (forestry.apiculture.alveary.multiblock.AlvearyClimatizerBlockEntity) F2MachineTests.place(level, h.absolutePos(new BlockPos(2 + i * 3, 2, 1)), ids[i]);
			ValueIOUtil.readTag(saved, level.registryAccess(), restored::loadCustomOnly);
			int previous = changes[0]; restored.changeClimate(2, climate);
			h.assertTrue(changes[0] != previous && restored.getEnergyStorage(null).getAmount() == 0 && restored.saveCustomOnly(level.registryAccess()).getIntOr("Heating", 0) == 3, "Loaded paid climate credit works without another energy charge");
		}
		var hygro = (forestry.apiculture.alveary.multiblock.AlvearyHygroregulatorBlockEntity) F2MachineTests.place(level, h.absolutePos(new BlockPos(7, 2, 1)), "alveary_hygroregulator");
		hygro.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 10));
		var before = hygro.saveCustomOnly(level.registryAccess()); changes[0] = 0; changes[1] = 0;
		try (var tx = Transaction.openOuter()) { hygro.changeClimate(1, climate); }
		h.assertTrue(before.equals(hygro.saveCustomOnly(level.registryAccess())) && changes[0] == 0 && changes[1] == 0, "Humidity regulator restores fluid and duration without emitting climate effects");
		hygro.changeClimate(1, climate);
		h.assertTrue(hygro.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(9) && changes[1] == 1, "Real humidity recipe commits once");
		var saved = hygro.saveCustomOnly(level.registryAccess());
		ValueIOUtil.readTag(saved, level.registryAccess(), hygro::loadCustomOnly); hygro.changeClimate(2, climate);
		h.assertTrue(hygro.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(9) && changes[1] == 2, "Loaded humidity credit does not drain another fluid unit");
		h.succeed();
	}

	static void abort(GameTestHelper h, TileForestry tile, Runnable operation) {
		var before = tile.saveCustomOnly(h.getLevel().registryAccess());
		try (var tx = Transaction.openOuter()) { operation.run(); }
		h.assertTrue(before.equals(tile.saveCustomOnly(h.getLevel().registryAccess())), "Fuel, thermal state, buffers and resources all roll back: " + tile.getClass().getSimpleName());
	}

	static void heat(EngineBlockEntity engine, int heat) {
		var tag = engine.saveCustomOnly(engine.getLevel().registryAccess()); tag.putInt("EngineHeat", heat);
		ValueIOUtil.readTag(tag, engine.getLevel().registryAccess(), engine::loadCustomOnly);
	}

	@GameTest public void engines(GameTestHelper h) {
		var level = h.getLevel();
		var base = h.absolutePos(new BlockPos(1, 2, 1));
		var peat = (PeatEngineBlockEntity) F2MachineTests.place(level, base, "peat_engine");
		level.setBlockAndUpdate(base.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		peat.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("forestry:peat")), 2));
		abort(h, peat, peat::burn);
		peat.burn();
		h.assertTrue(peat.getItem(0).getCount() == 1, "Reserve exactly one peat");
		abort(h, peat, peat::processEngineStep);
		peat.burn();
		h.assertTrue(peat.getEnergyManager().getEnergyStored() == 20, "Real peat DataMap generation");
		peat.getEnergyManager().setEnergyStored(peat.getEnergyManager().getMaxEnergyStored());
		var full = peat.saveCustomOnly(level.registryAccess()); peat.burn();
		h.assertTrue(full.equals(peat.saveCustomOnly(level.registryAccess())), "Full energy buffer preserves peat burn credit");

		var biogas = (BiogasEngineBlockEntity) F2MachineTests.place(level, base.offset(3, 0, 0), "biogas_engine");
		level.setBlockAndUpdate(biogas.getBlockPos().below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		heat(biogas, 3000);
		biogas.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(BuiltInRegistries.FLUID.getValue(Identifier.parse("forestry:biomass")), 2000));
		abort(h, biogas, biogas::burn);
		biogas.burn();
		h.assertTrue(biogas.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(1000) && biogas.getTankManager().getTank(2).getFluidAmount() == FluidUnits.fromMilliBuckets(2500), "Bucket fuel becomes persisted burn credit");
		abort(h, biogas, biogas::processEngineStep);
		biogas.burn();
		h.assertTrue(biogas.getEnergyManager().getEnergyStored() == 50 && biogas.getTankManager().getTank(2).getFluidAmount() == FluidUnits.fromMilliBuckets(2499), "One burn step generates exact energy");
		biogas.getEnergyManager().setEnergyStored(biogas.getEnergyManager().getMaxEnergyStored());
		full = biogas.saveCustomOnly(level.registryAccess()); biogas.burn();
		h.assertTrue(full.equals(biogas.saveCustomOnly(level.registryAccess())), "Full buffer pauses biogas consumption");

		var combustion = (CombustionEngineBlockEntity) F2MachineTests.place(level, base.offset(6, 0, 0), "combustion_engine");
		level.setBlockAndUpdate(combustion.getBlockPos().below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		combustion.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(BuiltInRegistries.FLUID.getValue(Identifier.parse("forestry:ethanol")), 2000));
		combustion.getTankManager().getTank(1).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 2000));
		abort(h, combustion, combustion::processEngineStep);
		combustion.processEngineStep();
		h.assertTrue(combustion.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(1000) && combustion.getTankManager().getTank(1).getFluidAmount() == FluidUnits.fromMilliBuckets(1000), "Fuel and coolant each reserve one bucket");
		combustion.getTankManager().getTank(3).setFluid(FluidVolume.EMPTY);
		full = combustion.saveCustomOnly(level.registryAccess()); combustion.burn();
		h.assertTrue(full.equals(combustion.saveCustomOnly(level.registryAccess())), "Missing coolant does not overwrite an unfinished fuel batch");
		combustion.processEngineStep();
		h.assertTrue(combustion.getEnergyManager().getEnergyStored() == 40 && combustion.getTankManager().getTank(2).getFluidAmount() == FluidUnits.fromMilliBuckets(14999), "Refilled coolant resumes same fuel credit");
		h.succeed();
	}

	@GameTest public void moistener(GameTestHelper h) {
		var level = h.getLevel();
		var moistener = (TileMoistener) F2MachineTests.place(level, h.absolutePos(new BlockPos(1, 2, 1)), "moistener");
		moistener.setItem(6, new ItemStack(Items.WHEAT, 2));
		moistener.setItem(11, new ItemStack(Items.AMETHYST_SHARD, 3));
		moistener.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 100));
		abort(h, moistener, () -> moistener.processWorkStep(4));
		moistener.processWorkStep(4); moistener.processWorkStep(4);
		var saved = moistener.saveCustomOnly(level.registryAccess());
		var restored = (TileMoistener) F2MachineTests.place(level, h.absolutePos(new BlockPos(2, 2, 1)), "moistener");
		ValueIOUtil.readTag(saved, level.registryAccess(), restored::loadCustomOnly);
		h.assertTrue(saved.equals(restored.saveCustomOnly(level.registryAccess())), "Reload retains moistening progress and burn credit");
		restored.setItem(10, new ItemStack(Items.COBBLESTONE, 64));
		abort(h, restored, () -> restored.processWorkStep(4));
		restored.processWorkStep(4);
		h.assertTrue(restored.getItem(11).getCount() == 2 && restored.getTankManager().getTank(0).getFluidAmount() == FluidUnits.fromMilliBuckets(98), "Completed output retained as pending buffer when inventory is full");
		var blocked = restored.saveCustomOnly(level.registryAccess());
		restored.processWorkStep(4);
		h.assertTrue(blocked.equals(restored.saveCustomOnly(level.registryAccess())), "Blocked pending output consumes no additional water/fuel");
		restored.setItem(10, ItemStack.EMPTY);
		abort(h, restored, () -> restored.processWorkStep(4));
		restored.processWorkStep(4);
		h.assertTrue(restored.getItem(10).is(Items.DIAMOND) && restored.getItem(10).getCount() == 2 && restored.getItem(11).getCount() == 2, "Pending buffer flushes exactly once");
		h.succeed();
	}
}
