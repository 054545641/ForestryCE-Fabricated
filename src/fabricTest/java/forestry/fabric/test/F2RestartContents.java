package forestry.fabric.test;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.features.CoreDataComponents;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.content.machines.tiles.TileStill;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import static forestry.fabric.test.F1RegistrationTests.*;
/** Real native chunks and SavedData, read in a new JVM before that process runs its GameTests. */
final class F2RestartContents {
    private static final BlockPos STILL = new BlockPos(2, 90, 0), LEAF = new BlockPos(4, 90, 0), FARM = new BlockPos(6, 90, 0);
    static void seed(ServerLevel level, ChestBlockEntity chest) {
        var vessel = forestry.core.features.FluidsItems.CONTAINERS.get(forestry.core.platform.item.FluidContainerType.REFRACTORY_CAPSULE).stack();
        vessel.set(CoreDataComponents.FLUID_CONTENT.get(), FluidVolume.milliBuckets(net.minecraft.world.level.material.Fluids.LAVA, 1000)); chest.setItem(1, vessel);
        var pack = forestry.core.content.backpacks.features.BackpackItems.MINER_BACKPACK.stack();
        pack.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.DIAMOND, 13)))); chest.setItem(2, pack);
        var tree = SpeciesUtil.TREE_TYPE.get().getSpecies(forestry.api.arboriculture.ForestryTreeSpecies.OAK).createIndividual();
        tree.setMate(SpeciesUtil.TREE_TYPE.get().getSpecies(forestry.api.arboriculture.ForestryTreeSpecies.BIRCH).getDefaultGenome());
        chest.setItem(3, tree.createStack(forestry.api.arboriculture.genetics.TreeLifeStage.SAPLING));
        var still = (TileStill) F2MachineTests.place(level, STILL, "still");
        still.getTankManager().getTank(0).setFluid(FluidVolume.milliBuckets(net.minecraft.world.level.material.Fluids.WATER, 72));
        still.getEnergyManager().setEnergyStored(800); require(still.processWorkStep() && still.processWorkStep(), "Cannot seed paid machine progress");
        still.getEnergyManager().setEnergyStored(0); still.setChanged();
        var state = ArboricultureBlocks.LEAVES.defaultState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT)) state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT, true);
        level.setBlockAndUpdate(LEAF, state); ((LeavesBlockEntity) level.getBlockEntity(LEAF)).setTree(tree); level.getBlockEntity(LEAF).setChanged();
        if (FabricLoader.getInstance().isModLoaded("forestrybutterflies")) {
            var butterfly = SpeciesUtil.BUTTERFLY_TYPE.get().getSpecies(forestry.api.lepidopterology.ForestryButterflySpecies.MONARCH).createIndividual();
            butterfly.setMate(SpeciesUtil.BUTTERFLY_TYPE.get().getSpecies(forestry.api.lepidopterology.ForestryButterflySpecies.CABBAGE_WHITE).getDefaultGenome());
            chest.setItem(4, butterfly.createStack(forestry.api.lepidopterology.genetics.ButterflyLifeStage.BUTTERFLY));
        }
        if (FabricLoader.getInstance().isModLoaded("forestryfarms")) {
            var farm = (forestry.agriculture.minifarm.tiles.AbstractMinifarmBlockEntity) F2MachineTests.place(level, FARM, "arboretum_managed");
            farm.getTankManager().getTank(0).setFluid(new FluidVolume(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER), 81001));
            farm.setItem(forestry.agriculture.minifarm.inventory.MinifarmInventory.CONFIG.fertilizerStart, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id("fertilizer")), 2));
            farm.setChanged();
        }
        if (FabricLoader.getInstance().isModLoaded("forestrymail")) seedMail(level);
    }
    static void verify(ServerLevel level, ChestBlockEntity chest) {
        require(chest.getItem(1).get(CoreDataComponents.FLUID_CONTENT.get()).amount() == 81000, "Capsule lost exact fluid components across JVMs");
        require(chest.getItem(2).get(net.minecraft.core.component.DataComponents.CONTAINER).allItemsCopyStream().findFirst().orElseThrow().getCount() == 13, "Backpack contents lost across JVMs");
        require(chest.getItem(3).has(CoreDataComponents.GENOME.get()) && chest.getItem(3).has(CoreDataComponents.MATE_GENOME.get()), "Tree genome or mate lost across JVMs");
        var still = (TileStill) level.getBlockEntity(STILL);
        require(still != null && still.getWorkCounter() == 2 && still.getEnergyManager().getEnergyStored() == 0 && still.getTankManager().getTank(0).getAmount() == 72 * 81, "Paid machine progress lost across JVMs");
        still.getEnergyManager().setEnergyStored(400); require(still.processWorkStep() && still.processWorkStep(), "Saved machine could not resume");
        require(still.getWorkCounter() == 0 && still.getEnergyManager().getEnergyStored() == 0 && still.getTankManager().getTank(0).getAmount() == 36 * 81 && still.getTankManager().getTank(1).getAmount() == 12 * 81, "Restart charged wrong resources or duplicated work");
        require(level.getBlockEntity(LEAF) instanceof LeavesBlockEntity leaves && leaves.getTree().getMate() != null, "Leaf pollination lost across JVMs");
        if (FabricLoader.getInstance().isModLoaded("forestrybutterflies")) require(chest.getItem(4).has(CoreDataComponents.GENOME.get()) && chest.getItem(4).has(CoreDataComponents.MATE_GENOME.get()), "Butterfly genetics lost across JVMs");
        if (FabricLoader.getInstance().isModLoaded("forestryfarms")) {
            var farm = (forestry.agriculture.minifarm.tiles.AbstractMinifarmBlockEntity) level.getBlockEntity(FARM);
            require(farm != null && farm.getTankManager().getTank(0).getAmount() == 81001 && farm.getItem(forestry.agriculture.minifarm.inventory.MinifarmInventory.CONFIG.fertilizerStart).getCount() == 2, "Farm rounded droplets or lost inventory across JVMs");
        }
        if (FabricLoader.getInstance().isModLoaded("forestrymail")) verifyMail(level);
    }
    private static forestry.mail.letters.MailAddress recipient() { return new forestry.mail.letters.MailAddress(new com.mojang.authlib.GameProfile(java.util.UUID.fromString("1ae6b7cd-d55d-4fc7-bf42-336b28a408ff"), "F2RestartMail")); }
    private static void seedMail(ServerLevel level) {
        var letter = new forestry.mail.letters.Letter(recipient(), recipient());
        letter.addStamps(forestry.mail.MailFabricModule.STAMPS.stack(forestry.mail.letters.EnumStampDefinition.P_5, 1));
        letter.addAttachment(new ItemStack(Items.EMERALD, 11));
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            require(forestry.mail.postoffice.PostOffice.getOrCreate(level).lodgeLetter(level, forestry.mail.letters.LetterUtils.createLetterStack(letter), tx).isOk(), "Cannot seed real mail delivery"); tx.commit();
        }
    }
    private static void verifyMail(ServerLevel level) {
        var box = forestry.mail.carriers.players.POBoxRegistry.getOrCreate(level).getPOBox(recipient());
        require(box != null && box.getPOBoxInfo().playerLetters() == 1, "Mailbox SavedData lost or duplicated delivery across JVMs");
        var letter = forestry.mail.letters.LetterUtils.getLetterData(box.getItem(0));
        require(letter.isProcessed() && letter.getItem(0).is(Items.EMERALD) && letter.getItem(0).getCount() == 11, "Delivered letter components lost across JVMs");
    }
}
