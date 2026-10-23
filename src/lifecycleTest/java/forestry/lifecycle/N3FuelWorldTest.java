package forestry.lifecycle;

import forestry.agriculture.minifarm.tiles.AbstractMinifarmBlockEntity;
import forestry.core.content.energy.tiles.*;
import forestry.core.content.machines.tiles.TileMoistener;
import forestry.core.platform.tile.TileForestry;
import forestry.core.platform.util.ValueIOUtil;
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Path;
import java.util.List;

import static forestry.lifecycle.PostalTests.check;

final class N3FuelWorldTest {
	private static final List<String> IDS = List.of("peat_engine", "biogas_engine", "combustion_engine", "clockwork_engine", "moistener", "arboretum_managed");
	private static BlockPos pos(int i) { return new BlockPos(2 + 3 * i, 80, 8); }
	private static Path expected(String phase) { return Path.of("n3-fuel-" + phase + ".nbt"); }
	static void run(ServerLevel level, String phase) throws Exception {
		for (int i = 0; i < IDS.size(); i++) level.getChunk(pos(i));
		if (phase.equals("seed")) {
			for (int i = 0; i < IDS.size(); i++) {
				level.setBlockAndUpdate(pos(i), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(pos(i).below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
				TileForestry tile = (TileForestry) N2WorldTest.place(level, pos(i), IDS.get(i));
				switch (tile) {
					case PeatEngineBlockEntity m -> { m.setItem(0, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("forestry:peat")), 2)); m.burn(); }
					case BiogasEngineBlockEntity m -> {
						N3FuelTests.heat(m, 3000);
						m.getTankManager().getTank(0).setFluid(new FluidStack(BuiltInRegistries.FLUID.getValue(Identifier.parse("forestry:biomass")), 2000)); m.burn();
					}
					case CombustionEngineBlockEntity m -> {
						m.getTankManager().getTank(0).setFluid(new FluidStack(BuiltInRegistries.FLUID.getValue(Identifier.parse("forestry:ethanol")), 2000));
						m.getTankManager().getTank(1).setFluid(new FluidStack(Fluids.WATER, 2000)); m.processEngineStep();
					}
					case ClockworkEngineBlockEntity m -> {
						var tag = m.saveCustomOnly(level.registryAccess()); tag.putFloat("tension", 2);
						ValueIOUtil.readTag(tag, level.registryAccess(), m::loadCustomOnly);
					}
					case TileMoistener m -> {
						level.setBlockAndUpdate(pos(i).above(), Blocks.GLOWSTONE.defaultBlockState());
						m.setItem(6, new ItemStack(Items.WHEAT, 2)); m.setItem(11, new ItemStack(Items.AMETHYST_SHARD, 3));
						m.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 100)); m.processWorkStep(4); m.processWorkStep(4);
					}
					case AbstractMinifarmBlockEntity m -> {
						m.addPendingProduct(new ItemStack(Items.DIAMOND, 2));
						for (int slot = 8; slot < 12; slot++) m.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
						m.getEnergyManager().setEnergyStored(10); check(m.processWorkStep(), "Save half-paid farm work");
					}
					default -> throw new AssertionError("Unhandled fuel fixture");
				}
			}
		} else {
			var before = NbtIo.readCompressed(expected(phase.equals("resume") ? "seed" : "resume"), NbtAccounter.unlimitedHeap());
			var actual = snapshot(level);
			for (String id : IDS) check(before.get(id).equals(actual.get(id)), "Fuel world restart changed " + id + ": expected=" + before.get(id) + ", actual=" + actual.get(id));
			if (phase.equals("resume")) {
				for (int i = 0; i < 4; i++) ((EngineBlockEntity) level.getBlockEntity(pos(i))).burn();
				int[] energy = {20, 50, 40, 2};
				for (int i = 0; i < 4; i++) check(((EngineBlockEntity) level.getBlockEntity(pos(i))).getEnergyManager().getEnergyStored() == energy[i], "Exact fuel credit after JVM restart: " + IDS.get(i));
				var moistener = (TileMoistener) level.getBlockEntity(pos(4));
				moistener.setItem(10, new ItemStack(Items.COBBLESTONE, 64)); moistener.processWorkStep(4);
				check(moistener.getItem(11).getCount() == 2 && moistener.getTankManager().getTank(0).getFluidAmount() == 98, "Resume moistener paid progress into pending output");
				var farm = (AbstractMinifarmBlockEntity) level.getBlockEntity(pos(5));
				check(farm.processWorkStep() && farm.getEnergyManager().getEnergyStored() == 0 && farm.getWorkCounter() == 2, "Full farm stores paid work credit");
			} else {
				var moistener = (TileMoistener) level.getBlockEntity(pos(4));
				moistener.setItem(10, ItemStack.EMPTY); moistener.processWorkStep(4);
				check(moistener.getItem(10).is(Items.DIAMOND) && moistener.getItem(10).getCount() == 2 && moistener.getTankManager().getTank(0).getFluidAmount() == 98, "Reloaded pending moistener output flushes once");
				var farm = (AbstractMinifarmBlockEntity) level.getBlockEntity(pos(5));
				farm.setItem(8, ItemStack.EMPTY); check(farm.processWorkStep(), "Resume fully paid farm without power");
				check(farm.getItem(8).is(Items.DIAMOND) && farm.getItem(8).getCount() == 2 && farm.getWorkCounter() == 0 && farm.getEnergyManager().getEnergyStored() == 0, "Pending farm harvest survives restart without duplication or a second charge");
			}
		}
		NbtIo.writeCompressed(snapshot(level), expected(phase));
	}
	private static CompoundTag snapshot(ServerLevel level) {
		var result = new CompoundTag();
		for (int i = 0; i < IDS.size(); i++) {
			check(level.getBlockEntity(pos(i)) != null, "Fuel fixture loaded: " + IDS.get(i));
			result.put(IDS.get(i), level.getBlockEntity(pos(i)).saveCustomOnly(level.registryAccess()));
		}
		return result;
	}
}
