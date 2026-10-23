package forestry.fabric.test;

import com.mojang.authlib.GameProfile;
import forestry.api.ForestryDataMaps;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.ForestryError;
import forestry.api.core.fluids.FluidUnits;
import forestry.api.core.fluids.FluidVolume;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.core.machines.IVariableFermentable;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import forestry.core.content.machines.gui.ContainerFermenter;
import forestry.core.content.machines.inventory.InventoryFermenter;
import forestry.core.content.machines.tiles.TileFermenter;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.platform.util.ValueIOUtil;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import team.reborn.energy.api.EnergyStorage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import static forestry.fabric.test.F1RegistrationTests.*;

public final class F3FermenterTests {
    @GameTest
    public void nativeMenuSynchronizesInitialProgressChangesAndCompletion(GameTestHelper test) {
        TileFermenter machine = freshMachine(test);
        insertItem(machine, new ItemStack(Items.OAK_SAPLING));
        insertItem(machine, new ItemStack(BuiltInRegistries.ITEM.getValue(id("fertilizer"))));
        insertFluid(machine, FluidVolume.milliBuckets(Fluids.WATER, 250));
        supplyEnergy(machine, machine.getEnergyPerWorkCycle());
        var player = FakePlayer.get(test.getLevel(), new GameProfile(UUID.randomUUID(), "FermenterMenuTest"));
        var menu = new ContainerFermenter(17, player.getInventory(), machine);
        var synchronizedData = new FermenterMenuSync();
        menu.setSynchronizer(synchronizedData);
        synchronizedData.assertData(0, 0, 0, 0);
        require(synchronizedData.initialUpdates == 1 && synchronizedData.dataUpdates == 0, "Initial menu state skipped the native synchronizer");

        for (int step = 0; step < machine.getStepsPerWorkCycle(); step++) {
            require(machine.processWorkStep(), "Menu fixture did not begin fermentation");
        }
        menu.broadcastChanges();
        synchronizedData.assertData(199, 200, 194, 250);
        require(synchronizedData.dataUpdates == 4, "Starting fermentation did not synchronize all four menu values");
        menu.broadcastChanges();
        require(synchronizedData.dataUpdates == 4, "Unchanged progress was resent through the native synchronizer");

        require(machine.processWorkStep(), "Menu fixture did not advance fermentation");
        menu.broadcastChanges();
        synchronizedData.assertData(198, 200, 138, 250);
        require(synchronizedData.dataUpdates == 6, "Advancing fermentation did not synchronize just the changed progress values");

        var reopenedMenu = new ContainerFermenter(18, player.getInventory(), machine);
        var reopenedData = new FermenterMenuSync();
        reopenedMenu.setSynchronizer(reopenedData);
        reopenedData.assertData(198, 200, 138, 250);
        require(reopenedData.initialUpdates == 1 && reopenedData.dataUpdates == 0, "Reopened menu did not receive its current initial progress");

        for (int step = 0; step < 3; step++) {
            require(machine.processWorkStep(), "Menu fixture did not complete fermentation");
        }
        reopenedMenu.broadcastChanges();
        reopenedData.assertData(195, 200, 0, 250);
        require(reopenedData.dataUpdates == 2, "Completed fermentation left stale menu progress");
        test.succeed();
    }

    @GameTest
    public void productionRecipesWorkWithEveryProductionFuel(GameTestHelper test) {
        var recipes = RecipeUtils.getRecipes(RecipeUtils.getRecipeMap(test.getLevel()), FactoryRecipeTypes.FERMENTER)
            .filter(recipe -> recipe.getId().getNamespace().equals("forestry")).toList();
        require(recipes.size() == 19, "Expected all 19 production fermenter recipes");
        for (var recipe : recipes) {
            Item resource = switch (recipe.getId().getPath().substring("fermenter/".length()).replace("_honey", "").replace("_juice", "")) {
                case "sapling" -> Items.OAK_SAPLING;
                case "cactus" -> Items.CACTUS;
                case "mushroom" -> Items.BROWN_MUSHROOM;
                case "potato" -> Items.POTATO;
                case "sugar_cane" -> Items.SUGAR_CANE;
                case "wheat" -> Items.WHEAT;
                case "honeydew" -> BuiltInRegistries.ITEM.getValue(id("honeydew"));
                default -> throw new AssertionError("Missing fermenter fixture: " + recipe.getId());
            };
            for (String fuelName : List.of("fertilizer", "compost", "mulch")) {
                TileFermenter machine = freshMachine(test);
                Item fuel = BuiltInRegistries.ITEM.getValue(id(fuelName));
                var definition = ForestryDataMaps.FERMENTER_FUELS.get(fuel.builtInRegistryHolder());
                require(definition != null, "Missing production fuel: " + fuelName);
                insertItem(machine, new ItemStack(resource));
                insertItem(machine, new ItemStack(fuel, 2));
                insertFluid(machine, recipe.getInputFluid().withAmount(FluidUnits.fromMilliBuckets(recipe.getFermentationValue() + 100)));
                supplyEnergy(machine, machine.getEnergyPerWorkCycle());
                require(machine.hasWork(), "Production recipe unavailable: " + recipe.getId() + " / " + fuelName);
                int cycles = Math.ceilDiv(recipe.getFermentationValue(), definition.fermentPerCycle());
                for (int step = 0; step < machine.getStepsPerWorkCycle() + cycles - 1; step++) {
                    require(machine.processWorkStep(), "Production recipe stalled: " + recipe.getId() + " / " + fuelName);
                }
                require(machine.getItem(InventoryFermenter.SLOT_RESOURCE).isEmpty()
                    && machine.getItem(InventoryFermenter.SLOT_FUEL).getCount() == 1, "Recipe consumed the wrong item counts: " + recipe.getId());
                require(machine.getTankManager().getTank(0).getAmount() == FluidUnits.fromMilliBuckets(100), "Recipe consumed the wrong fluid amount: " + recipe.getId());
                require(machine.getTankManager().getTank(1).getFluid().equals(FluidVolume.milliBuckets(recipe.getOutput(), Math.round(recipe.getFermentationValue() * recipe.getModifier()))), "Recipe produced the wrong fluid or amount: " + recipe.getId());
                require(machine.getEnergyManager().getAmount() == 0 && machine.getWorkCounter() == 0, "Batch charged the wrong energy: " + recipe.getId());
                require(machine.saveCustomOnly(test.getLevel().registryAccess()).getIntOr("FuelBurnTime", 0) == definition.burnDuration() - cycles, "Fuel burn credit changed: " + fuelName);
                var fluids = FluidStorage.SIDED.find(test.getLevel(), machine.getBlockPos(), Direction.DOWN);
                long productAmount = machine.getTankManager().getTank(1).getAmount();
                try (var transaction = Transaction.openOuter()) {
                    require(fluids.extract(recipe.getInputFluid().variant(), Long.MAX_VALUE, transaction) == 0, "Automation extracted unprocessed input fluid");
                    require(fluids.extract(FluidVariant.of(recipe.getOutput()), Long.MAX_VALUE, transaction) == productAmount, "Automation could not extract finished product");
                }
            }
        }
        test.succeed();
    }

    @GameTest(maxTicks = 380)
    public void worldTicksHandlePowerRedstoneBucketsAndAutomation(GameTestHelper test) {
        TileFermenter machine = freshMachine(test);
        insertItem(machine, new ItemStack(Items.OAK_SAPLING, 4));
        insertItem(machine, new ItemStack(BuiltInRegistries.ITEM.getValue(id("fertilizer")), 2));
        insertItem(machine, new ItemStack(Items.WATER_BUCKET));
        machine.setItem(InventoryFermenter.SLOT_CAN_INPUT, new ItemStack(Items.BUCKET));
        machine.setItem(InventoryFermenter.SLOT_CAN_OUTPUT, new ItemStack(Items.COBBLESTONE, 64));
        test.runAtTickTime(55, () -> {
            require(machine.getItem(InventoryFermenter.SLOT_INPUT).is(Items.BUCKET)
                && machine.getTankManager().getTank(0).getAmount() == FluidUnits.fromMilliBuckets(1000), "Ticking fermenter did not empty the water bucket");
            require(machine.getItem(InventoryFermenter.SLOT_RESOURCE).getCount() == 4 && machine.getItem(InventoryFermenter.SLOT_FUEL).getCount() == 2
                && machine.getWorkCounter() == 0 && machine.getErrorLogic().contains(ForestryError.NO_POWER), "Unpowered fermenter consumed resources or failed to report missing power");
            test.getLevel().setBlockAndUpdate(machine.getBlockPos().below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            supplyEnergy(machine, machine.getEnergyPerWorkCycle() * 4L);
        });
        test.runAtTickTime(95, () -> {
            require(machine.getWorkCounter() == 0 && machine.getTankManager().getTank(1).isEmpty()
                && machine.getEnergyManager().getAmount() == machine.getEnergyPerWorkCycle() * 4L
                && machine.getErrorLogic().contains(ForestryError.DISABLED_BY_REDSTONE), "Redstone failed to pause the fermenter");
            test.getLevel().setBlockAndUpdate(machine.getBlockPos().below(), Blocks.AIR.defaultBlockState());
        });
        test.runAtTickTime(300, () -> {
            require(machine.getItem(InventoryFermenter.SLOT_RESOURCE).isEmpty() && machine.getItem(InventoryFermenter.SLOT_FUEL).getCount() == 1
                && machine.getTankManager().getTank(0).isEmpty() && machine.getEnergyManager().getAmount() == 0, "Natural world ticks did not complete four sapling batches");
            require(machine.getTankManager().getTank(1).getFluid().equals(FluidVolume.milliBuckets(ForestryFluids.BIOMASS.getFluid(), 1000))
                && machine.getItem(InventoryFermenter.SLOT_CAN_INPUT).is(Items.BUCKET), "Blocked container output lost fluid or a bucket");
            machine.setItem(InventoryFermenter.SLOT_CAN_OUTPUT, ItemStack.EMPTY);
        });
        test.runAtTickTime(340, () -> {
            require(machine.getTankManager().getTank(1).isEmpty() && machine.getItem(InventoryFermenter.SLOT_CAN_INPUT).isEmpty()
                && machine.getItem(InventoryFermenter.SLOT_CAN_OUTPUT).is(ForestryFluids.BIOMASS.getBucket()), "Ticking fermenter did not fill the biomass bucket");
            var items = ItemStorage.SIDED.find(test.getLevel(), machine.getBlockPos(), Direction.DOWN);
            try (var transaction = Transaction.openOuter()) {
                require(items.extract(ItemVariant.of(ForestryFluids.BIOMASS.getBucket()), 1, transaction) == 1, "Automation could not retrieve the filled bucket");
                transaction.commit();
            }
            require(machine.getItem(InventoryFermenter.SLOT_CAN_OUTPUT).isEmpty(), "Extracted bucket remained in the machine");
            test.succeed();
        });
    }

    @GameTest
    public void geneticBatchPausesAndResumesAfterSaveWithoutLoss(GameTestHelper test) {
        var species = SpeciesUtil.TREE_TYPE.get().getAllSpecies().stream()
            .max(Comparator.comparingDouble(tree -> tree.getDefaultGenome().getActiveValue(TreeChromosomes.SAPPINESS))).orElseThrow();
        ItemStack sapling = species.createStack(TreeLifeStage.SAPLING);
        float modifier = ((IVariableFermentable) sapling.getItem()).getFermentationModifier(sapling);
        require(modifier > 1, "Genetic fixture must distinguish its sapling from a vanilla sapling");
        TileFermenter machine = freshMachine(test);
        insertItem(machine, sapling);
        insertFluid(machine, FluidVolume.milliBuckets(Fluids.WATER, 250));
        supplyEnergy(machine, machine.getEnergyPerWorkCycle());
        assertBlocked(machine, ForestryError.NO_FUEL);
        insertItem(machine, new ItemStack(BuiltInRegistries.ITEM.getValue(id("fertilizer"))));
        for (int step = 0; step < 4; step++) require(machine.processWorkStep(), "Genetic batch could not start");
        require(machine.getItem(InventoryFermenter.SLOT_RESOURCE).isEmpty() && machine.getItem(InventoryFermenter.SLOT_FUEL).isEmpty(), "Genetic batch did not reserve exactly one sapling and fertilizer");
        var produced = machine.getTankManager().getTank(1).getFluid();
        require(produced.getAmount() == FluidUnits.fromMilliBuckets(Math.round(56 * modifier)), "Genetic sapling lost its yield modifier");
        machine.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 55));
        assertBlocked(machine, ForestryError.NO_RESOURCE_LIQUID);
        machine.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 194));
        machine.getTankManager().getTank(1).setFluid(produced.withAmount(machine.getTankManager().getTank(1).getCapacity()));
        assertBlocked(machine, ForestryError.NO_SPACE_TANK);
        machine.getTankManager().getTank(1).setFluid(produced);
        try (var transaction = Transaction.openOuter()) {
            require(machine.processWorkStep(), "Nested active batch could not advance");
        }
        require(machine.getTankManager().getTank(1).getFluid().equals(produced)
            && machine.getTankManager().getTank(0).getAmount() == FluidUnits.fromMilliBuckets(194), "Aborted batch progress lost fluid");
        TileFermenter restored = F2MachineTests.machine(test, "fermenter", 2);
        ValueIOUtil.readTag(machine.saveCustomOnly(test.getLevel().registryAccess()), test.getLevel().registryAccess(), restored::loadCustomOnly);
        restored.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(ForestryFluids.HONEY.getFluid(), 194));
        assertBlocked(restored, ForestryError.NO_RESOURCE_LIQUID);
        restored.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(Fluids.WATER, 194));
        for (int step = 0; step < 4; step++) require(restored.processWorkStep(), "Loaded batch lost its paid work, fuel or active recipe");
        require(restored.getTankManager().getTank(0).isEmpty() && restored.getWorkCounter() == 0 && restored.getEnergyManager().getAmount() == 0, "Loaded batch consumed the wrong water or energy");
        require(restored.getTankManager().getTank(1).getAmount() == FluidUnits.fromMilliBuckets(4 * Math.round(56 * modifier) + Math.round(26 * modifier)), "Loaded batch lost its genetic yield modifier");
        require(restored.saveCustomOnly(test.getLevel().registryAccess()).getIntOr("FuelBurnTime", 0) == 195, "Saved fuel credit was lost or spent twice");
        test.succeed();
    }

    private static final class FermenterMenuSync implements ContainerSynchronizer {
        private int[] data = new int[0];
        private int initialUpdates;
        private int dataUpdates;

        @Override
        public void sendInitialData(AbstractContainerMenu menu, List<ItemStack> items, ItemStack carried, int[] values) {
            this.data = values.clone();
            this.initialUpdates++;
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
        }

        @Override
        public void sendDataChange(AbstractContainerMenu menu, int index, int value) {
            this.data[index] = value;
            this.dataUpdates++;
        }

        @Override
        public RemoteSlot createSlot() {
            return RemoteSlot.PLACEHOLDER;
        }

        private void assertData(int... expected) {
            require(Arrays.equals(this.data, expected), "Native fermenter menu data mismatch: " + Arrays.toString(this.data)
                + " expected " + Arrays.toString(expected));
        }
    }

    private static TileFermenter freshMachine(GameTestHelper test) {
        test.setBlock(1, 2, 1, Blocks.AIR);
        return F2MachineTests.machine(test, "fermenter", 1);
    }

    private static void insertItem(TileFermenter machine, ItemStack stack) {
        var storage = ItemStorage.SIDED.find(machine.getLevel(), machine.getBlockPos(), Direction.UP);
        require(storage != null, "Fermenter item lookup unavailable");
        try (var transaction = Transaction.openOuter()) {
            require(storage.insert(ItemVariant.of(stack), stack.getCount(), transaction) == stack.getCount(), "Fermenter rejected automation input: " + stack);
            transaction.commit();
        }
    }

    private static void insertFluid(TileFermenter machine, FluidVolume fluid) {
        var storage = FluidStorage.SIDED.find(machine.getLevel(), machine.getBlockPos(), Direction.NORTH);
        require(storage != null, "Fermenter fluid lookup unavailable");
        try (var transaction = Transaction.openOuter()) {
            require(storage.insert(fluid.variant(), fluid.amount(), transaction) == fluid.amount(), "Fermenter rejected automation fluid: " + fluid);
            transaction.commit();
        }
    }

    private static void supplyEnergy(TileFermenter machine, long amount) {
        var storage = EnergyStorage.SIDED.find(machine.getLevel(), machine.getBlockPos(), Direction.SOUTH);
        require(storage != null, "Fermenter energy lookup unavailable");
        try (var transaction = Transaction.openOuter()) {
            for (long remaining = amount; remaining > 0;) {
                long inserted = storage.insert(remaining, transaction);
                require(inserted > 0, "Fermenter refused power");
                remaining -= inserted;
            }
            transaction.commit();
        }
    }

    private static void assertBlocked(TileFermenter machine, ForestryError error) {
        var saved = machine.saveCustomOnly(machine.getLevel().registryAccess());
        for (int step = 0; step < 8; step++) require(!machine.processWorkStep(), "Blocked fermenter advanced: " + error);
        require(machine.getErrorLogic().contains(error), "Fermenter did not report: " + error);
        require(saved.equals(machine.saveCustomOnly(machine.getLevel().registryAccess())), "Blocked fermenter changed its saved resources or progress: " + error);
    }
}
