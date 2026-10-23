package forestry.fabric.test;

import forestry.Forestry;
import forestry.api.core.fluids.FluidUnits;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.content.machines.gui.ContainerFermenter;
import forestry.core.content.machines.gui.GuiFermenter;
import forestry.core.content.machines.inventory.InventoryFermenter;
import forestry.core.content.machines.tiles.TileFermenter;
import forestry.core.platform.fluids.ForestryFluids;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import team.reborn.energy.api.EnergyStorage;

import static forestry.fabric.test.F1RegistrationTests.*;

@SuppressWarnings("UnstableApiUsage")
public final class F3FermenterClientTest implements FabricClientGameTest {
    private static final BlockPos MACHINE_POS = new BlockPos(0, 100, 3);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("fill -4 99 -4 4 99 4 minecraft:stone");
            world.getServer().runCommand("tp @p 0.5 100 0.5 0 15");
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                server.getPlayerList().getPlayers().getFirst().getInventory().clearContent();
                level.setBlockAndUpdate(MACHINE_POS.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                level.setBlockAndUpdate(MACHINE_POS, BuiltInRegistries.BLOCK.getValue(id("fermenter")).defaultBlockState());
                var machine = (TileFermenter) level.getBlockEntity(MACHINE_POS);
                machine.setItem(InventoryFermenter.SLOT_RESOURCE, new ItemStack(Items.OAK_SAPLING));
                machine.setItem(InventoryFermenter.SLOT_FUEL, new ItemStack(BuiltInRegistries.ITEM.getValue(id("fertilizer")), 2));
                var fluidStorage = FluidStorage.SIDED.find(level, MACHINE_POS, Direction.NORTH);
                var energyStorage = EnergyStorage.SIDED.find(level, MACHINE_POS, Direction.SOUTH);
                require(fluidStorage != null && energyStorage != null, "Fermenter client fixture lacks transfer storage");
                try (var transaction = Transaction.openOuter()) {
                    long water = FluidUnits.fromMilliBuckets(4000);
                    require(fluidStorage.insert(FluidVariant.of(Fluids.WATER), water, transaction) == water, "Client fixture rejected water");
                    for (long remaining = 42000; remaining > 0;) {
                        long inserted = energyStorage.insert(remaining, transaction);
                        require(inserted > 0, "Client fixture rejected energy");
                        remaining -= inserted;
                    }
                    transaction.commit();
                }
            });
            world.getConnection().waitForChunksRender();
            context.waitTicks(25);
            var stableBlockState = world.getServer().computeOnServer(server -> server.overworld().getBlockState(MACHINE_POS));
            openWithInput(context);
            assertSynchronized(context, world, "ready");

            advanceToProduct(world, 56);
            var first = assertSynchronized(context, world, "first");
            require(first.fermentationTime() == 194 && first.fuelBurnTime() == 199
                && first.bioBar() > 0 && first.fuelBar() > 0, "First real work step did not expose nonzero GUI bars");
            context.takeScreenshot("forestry-fermenter-client-first-production");

            advanceToProduct(world, 112);
            var second = assertSynchronized(context, world, "second");
            require(second.fermentationTime() == 138 && second.fuelBurnTime() == 198
                && second.bioBar() < first.bioBar(), "Open fermenter menu did not advance both production counters");
            context.takeScreenshot("forestry-fermenter-client-second-production");

            int previousMenuId = context.computeOnClient(client -> client.player.containerMenu.containerId);
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitFor(client -> client.gui.screen() == null);
            openWithInput(context);
            context.runOnClient(client -> require(client.player.containerMenu.containerId != previousMenuId, "Fermenter was not reopened through a fresh server menu"));
            require(assertSynchronized(context, world, "reopened").equals(second), "Reopening changed paused fermenter state");
            context.takeScreenshot("forestry-fermenter-client-reopened");

            advanceToProduct(world, 250);
            var complete = assertSynchronized(context, world, "complete");
            require(complete.fermentationTime() == 0 && complete.bioBar() == 0 && complete.fuelBurnTime() == 195,
                "Completed batch left stale production progress in the GUI");
            require(complete.input().equals(FluidVolume.milliBuckets(Fluids.WATER, 3750))
                && complete.output().equals(FluidVolume.milliBuckets(ForestryFluids.BIOMASS.getFluid(), 250))
                && complete.energy() == 37800 && complete.resources() == 0 && complete.fuel() == 1,
                "Client menu displays incorrect completed-batch resources");
            world.getServer().runOnServer(server -> require(server.overworld().getBlockState(MACHINE_POS).equals(stableBlockState),
                "Fermenter fixture crossed a tank visual threshold and could hide broken menu synchronization"));
            context.takeScreenshot("forestry-fermenter-client-complete");
            context.getInput().pressKey(options -> options.keyInventory);
            context.waitFor(client -> client.gui.screen() == null);
        }
        Forestry.LOGGER.info("FORESTRY_FERMENTER_CLIENT_PROGRESS_FLUID_ENERGY_PASSED");
    }

    private static void openWithInput(ClientGameTestContext context) {
        context.getInput().lookAt(MACHINE_POS);
        context.waitTick();
        context.getInput().pressKey(options -> options.keyUse);
        context.waitFor(client -> client.gui.screen() instanceof GuiFermenter
            && client.player.containerMenu instanceof ContainerFermenter menu && menu.getTile().getBlockPos().equals(MACHINE_POS));
    }

    private static void advanceToProduct(TestSingleplayerContext world, int amount) {
        world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(MACHINE_POS.below(), Blocks.STONE.defaultBlockState()));
        world.getServer().waitFor(server -> {
            var level = server.overworld();
            var machine = (TileFermenter) level.getBlockEntity(MACHINE_POS);
            if (machine.getTankManager().getTank(1).getAmount() < FluidUnits.fromMilliBuckets(amount)) return false;
            level.setBlockAndUpdate(MACHINE_POS.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            require(machine.getTankManager().getTank(1).getAmount() == FluidUnits.fromMilliBuckets(amount), "Natural production skipped the requested observation point");
            return true;
        });
    }

    private static MachineState assertSynchronized(ClientGameTestContext context, TestSingleplayerContext world, String stage) {
        var expected = world.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            require(player.containerMenu instanceof ContainerFermenter, "Server has no real fermenter menu");
            return MachineState.capture((TileFermenter) server.overworld().getBlockEntity(MACHINE_POS));
        });
        Forestry.LOGGER.info("FORESTRY_FERMENTER_CLIENT_EXPECTED stage={} state={}", stage, expected);
        context.waitFor(client -> client.gui.screen() instanceof GuiFermenter
            && client.player.containerMenu instanceof ContainerFermenter menu
            && MachineState.capture(menu.getTile()).equals(expected), 40);
        Forestry.LOGGER.info("FORESTRY_FERMENTER_CLIENT_STAGE stage={} state={}", stage, expected);
        return expected;
    }

    private record MachineState(int fuelBurnTime, int fuelTotalTime, int fermentationTime, int fermentationTotalTime,
                                int fuelBar, int bioBar, FluidVolume input, FluidVolume output, long energy, int resources, int fuel) {
        private static MachineState capture(TileFermenter machine) {
            var saved = machine.saveCustomOnly(machine.getLevel().registryAccess());
            return new MachineState(saved.getIntOr("FuelBurnTime", 0), saved.getIntOr("FuelTotalTime", 0),
                saved.getIntOr("FermentationTime", 0), saved.getIntOr("FermentationTotalTime", 0),
                machine.getBurnTimeRemainingScaled(16), machine.getFermentationProgressScaled(16),
                machine.getTankManager().getTank(0).getFluid(), machine.getTankManager().getTank(1).getFluid(),
                machine.getEnergyManager().getAmount(), machine.getItem(InventoryFermenter.SLOT_RESOURCE).getCount(),
                machine.getItem(InventoryFermenter.SLOT_FUEL).getCount());
        }
    }
}
