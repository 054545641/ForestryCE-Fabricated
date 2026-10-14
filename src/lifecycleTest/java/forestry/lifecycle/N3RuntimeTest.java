package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.core.content.analyzer.TileAnalyzer;
import forestry.core.content.machines.tiles.*;
import forestry.core.platform.tile.TilePowered;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static forestry.lifecycle.PostalTests.check;

/** Three separate JVMs: save paid work, resume actual chunks, then reload completed outputs. */
@EventBusSubscriber(modid = LifecycleTests.ID)
public final class N3RuntimeTest {
	private static final List<String> MACHINES = List.of("still", "carpenter", "fermenter", "squeezer", "centrifuge", "smelter", "analyzer", "bottler", "thermionic_fabricator");
	private static BlockPos pos(int index) { return new BlockPos(2 + index * 3, 80, 2); }
	private static Path expected(String phase) { return Path.of("n3-" + phase + "-expected.nbt"); }

	@SubscribeEvent
	public static void started(ServerStartedEvent event) throws Exception {
		String phase = System.getProperty("forestry.lifecycle.n3Phase", "");
		if (phase.isEmpty()) return;
		var server = event.getServer();
		var level = server.overworld();
		try {
			for (int i = 0; i < MACHINES.size(); i++) level.getChunk(pos(i));
			if (phase.equals("seed")) {
				for (int i = 0; i < MACHINES.size(); i++) {
					level.setBlockAndUpdate(pos(i), Blocks.AIR.defaultBlockState());
					level.setBlockAndUpdate(pos(i).below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
					TilePowered machine = (TilePowered) N2WorldTest.place(level, pos(i), MACHINES.get(i));
					prepare(machine);
					machine.getEnergyManager().setEnergyStored(machine.getEnergyManager().getMaxEnergyStored());
					int steps = machine instanceof TileFermenter ? 4 : machine instanceof TileStill ? 2 : 1;
					for (int step = 0; step < steps; step++) check(machine.processWorkStep(), "Seed paid step: " + MACHINES.get(i));
					check(machine.getWorkCounter() > 0, "Save unfinished work: " + MACHINES.get(i));
				}
				NbtIo.writeCompressed(snapshot(level), expected("seed"));
			} else if (phase.equals("resume")) {
				compare(level, "seed");
				for (int i = 0; i < MACHINES.size(); i++) {
					TilePowered machine = (TilePowered) level.getBlockEntity(pos(i));
					int beforeEnergy = machine.getEnergyManager().getEnergyStored();
					int remainingSteps = Math.max(0, machine.getStepsPerWorkCycle() - machine.getWorkCounter());
					int cost = (int) Math.ceil(machine.getEnergyPerWorkCycle() / (double) Math.max(1, machine.getStepsPerWorkCycle()));
					int calls = 0;
					do { check(machine.processWorkStep(), "Resume work: " + MACHINES.get(i)); }
					while (machine.getWorkCounter() != 0 && ++calls < 1000);
					check(machine.getWorkCounter() == 0, "Batch finishes: " + MACHINES.get(i));
					check(machine.getEnergyManager().getEnergyStored() == beforeEnergy - remainingSteps * cost, "Charge only remaining paid steps: " + MACHINES.get(i));
					checkOutput(machine);
				}
				NbtIo.writeCompressed(snapshot(level), expected("resume"));
			} else if (phase.equals("verify")) {
				compare(level, "resume");
				for (int i = 0; i < MACHINES.size(); i++) checkOutput((TilePowered) level.getBlockEntity(pos(i)));
			} else throw new IllegalArgumentException("Unknown N3 phase: " + phase);
			N3FuelWorldTest.run(level, phase);
			server.saveEverything(false, true, true);
			Files.writeString(Path.of("n3-" + phase + ".passed"), "fifteen actual machines, chunk persistence, resource conservation\n");
			LogUtils.getLogger().info("FORESTRY_N3_WORLD_READY phase={} machines=15 actual_chunks=true paid_progress=true exact_outputs=true", phase);
		} finally { server.halt(false); }
	}

	private static CompoundTag snapshot(ServerLevel level) {
		var result = new CompoundTag();
		for (int i = 0; i < MACHINES.size(); i++) {
			check(level.getBlockEntity(pos(i)) instanceof TilePowered, "Actual machine loaded: " + MACHINES.get(i));
			result.put(MACHINES.get(i), level.getBlockEntity(pos(i)).saveCustomOnly(level.registryAccess()));
		}
		return result;
	}

	private static void compare(ServerLevel level, String phase) throws Exception {
		check(Files.exists(expected(phase)), "Run N3 " + phase + " first");
		var expected = NbtIo.readCompressed(expected(phase), NbtAccounter.unlimitedHeap());
		var actual = snapshot(level);
		for (String id : MACHINES) check(expected.get(id).equals(actual.get(id)), "Restart changed " + id + ": expected=" + expected.get(id) + ", actual=" + actual.get(id));
	}

	private static void prepare(TilePowered machine) {
		switch (machine) {
			case TileStill m -> m.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 72));
			case TileCarpenter m -> {
				m.getCraftingInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD));
				m.getCraftingInventory().setItem(1, new ItemStack(Items.AMETHYST_SHARD));
				m.setItem(9, new ItemStack(Items.CHEST, 4)); m.setItem(12, new ItemStack(Items.AMETHYST_SHARD, 8));
				m.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 200));
			}
			case TileFermenter m -> {
				m.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 2)); m.setItem(1, new ItemStack(Items.COAL, 3));
				m.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 1000));
			}
			case TileSqueezer m -> m.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 3));
			case TileCentrifuge m -> m.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 3));
			case TileSmelter m -> m.setItem(0, new ItemStack(Items.AMETHYST_SHARD, 4));
			case TileAnalyzer m -> {
				m.setItem(2, SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createStack(BeeLifeStage.DRONE));
				m.getTankManager().getTank(0).setFluid(new FluidStack(BuiltInRegistries.FLUID.getValue(Identifier.parse("forestry:honey")), 500));
			}
			case TileBottler m -> {
				m.setItem(3, new ItemStack(Items.BUCKET));
				m.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 2000));
			}
			case TileFabricator m -> {
				m.getCraftingInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD));
				m.getCraftingInventory().setItem(1, new ItemStack(Items.AMETHYST_SHARD));
				m.setItem(3, new ItemStack(Items.AMETHYST_SHARD, 4));
				m.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.LAVA, 200));
			}
			default -> throw new AssertionError("Unhandled fixture");
		}
	}

	private static void checkOutput(TilePowered machine) {
		boolean valid = switch (machine) {
			case TileStill m -> m.getTankManager().getTank(0).getFluidAmount() == 36 && m.getTankManager().getTank(1).getFluidAmount() == 12;
			case TileCarpenter m -> m.getItem(9).getCount() == 3 && m.getItem(12).getCount() == 6 && m.getItem(10).getCount() == 2 && m.getTankManager().getTank(0).getFluidAmount() == 100;
			case TileFermenter m -> m.getItem(0).getCount() == 1 && m.getItem(1).getCount() == 2 && m.getTankManager().getTank(0).getFluidAmount() == 700 && m.getTankManager().getTank(1).getFluidAmount() == 600;
			case TileSqueezer m -> m.getItem(0).getCount() == 2 && m.getItem(9).getCount() == 1 && m.getTankManager().getTank(0).getFluidAmount() == 125;
			case TileCentrifuge m -> m.getItem(0).getCount() == 2 && m.getItem(1).is(Items.DIAMOND) && m.getItem(1).getCount() == 1 && m.getItem(2).is(Items.EMERALD) && m.getItem(2).getCount() == 1;
			case TileSmelter m -> m.getItem(0).getCount() == 2 && m.getItem(10).getCount() == 2;
			case TileAnalyzer m -> m.getItem(0).isEmpty() && m.getItem(8).getCount() == 1 && m.getItem(8).getOrDefault(forestry.core.features.CoreDataComponents.ANALYZED, false) && m.getTankManager().getTank(0).getFluidAmount() == 400;
			case TileBottler m -> m.getItem(3).isEmpty() && m.getItem(5).is(Items.WATER_BUCKET) && m.getItem(5).getCount() == 1 && m.getTankManager().getTank(0).getFluidAmount() == 1000;
			case TileFabricator m -> m.getItem(3).getCount() == 2 && m.getItem(2).getCount() == 2 && m.getTankManager().getTank(0).getFluidAmount() == 100 && m.getHeat() == 100;
			default -> false;
		};
		check(valid, "Exact single batch output: " + machine.getClass().getSimpleName());
	}
}
