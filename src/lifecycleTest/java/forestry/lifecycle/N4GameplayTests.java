package forestry.lifecycle;

import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.apiculture.bees.AbstractBeeHousingBlockEntity;
import forestry.apiculture.features.ApicultureItems;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.arboriculture.sapling.SaplingBlockEntity;
import forestry.arboriculture.trees.genetics.TreePollenType;
import forestry.core.content.backpacks.BackpackMode;
import forestry.core.content.backpacks.BackpackResupplyHandler;
import forestry.core.content.backpacks.features.BackpackItems;
import forestry.core.content.backpacks.inventory.BackpackInventory;
import forestry.core.content.backpacks.items.BackpackItem;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.platform.util.ValueIOUtil;
import forestry.agriculture.minifarm.tiles.AbstractMinifarmBlockEntity;
import forestry.lepidopterology.cocoons.TileCocoon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Business entry points on registered production blocks, items and species. */
final class N4GameplayTests {
    static void bees(GameTestHelper h) {
        var housing = (AbstractBeeHousingBlockEntity) N2WorldTest.place(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 3)), "apiary");
        var species = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST);
        housing.setItem(0, species.createStack(BeeLifeStage.PRINCESS));
        housing.setItem(1, species.createStack(BeeLifeStage.DRONE));
        housing.setItem(9, ApicultureItems.FRAME_CREATIVE.stack());
        var logic = housing.getBeekeepingLogic();
        for (int i = 0; i < 2000 && IIndividualHandlerItem.getLifeStage(housing.getItem(0)) == BeeLifeStage.PRINCESS; i++) {
            h.assertTrue(logic.canWork(), "Princess with drone can breed");
            logic.doWork();
        }
        h.assertTrue(IIndividualHandlerItem.getLifeStage(housing.getItem(0)) == BeeLifeStage.QUEEN && housing.getItem(1).isEmpty(), "Breeding creates queen and consumes one drone");
        logic.setWorkThrottle(1);
        h.assertTrue(logic.canWork(), "Creative frame permits the newly bred queen to work");
        logic.doWork();
        h.assertTrue(countProducts(housing) > 0, "Actual queen work produces combs");
        logic.canWork();
        h.assertTrue(housing.getItem(0).isEmpty(), "Aged queen dies");
        boolean princess = false, drone = false;
        for (int i = 2; i < 9; i++) {
            var stage = IIndividualHandlerItem.getLifeStage(housing.getItem(i));
            princess |= stage == BeeLifeStage.PRINCESS;
            drone |= stage == BeeLifeStage.DRONE;
        }
        h.assertTrue(princess && drone, "Queen leaves both princess and drone offspring");
        h.succeed();
    }

    static void beeOverflow(GameTestHelper h) {
        var housing = (AbstractBeeHousingBlockEntity) N2WorldTest.place(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 3)), "apiary");
        IBee queen = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST).createIndividual();
        queen.setMate(queen.getGenome());
        queen.setHealth(0);
        housing.setItem(0, queen.createStack(BeeLifeStage.QUEEN));
        for (int i = 2; i < 9; i++) housing.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        housing.getBeekeepingLogic().canWork();
        var saved = housing.saveCustomOnly(h.getLevel().registryAccess());
        ValueIOUtil.readTag(saved, h.getLevel().registryAccess(), housing::loadCustomOnly);
        for (int i = 2; i < 9; i++) housing.setItem(i, ItemStack.EMPTY);
        housing.getBeekeepingLogic().canWork();
        int offspring = countProducts(housing);
        h.assertTrue(offspring >= 2, "Full housing persists pending offspring and delivers when space returns");
        housing.getBeekeepingLogic().canWork();
        h.assertTrue(countProducts(housing) == offspring, "Pending offspring delivered only once");
        h.succeed();
    }

    private static int countProducts(Container housing) {
        int count = 0;
        for (int i = 2; i < 9; i++) count += housing.getItem(i).getCount();
        return count;
    }

    static void trees(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(8, 3, 8));
        level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
        var sapling = (SaplingBlockEntity) N2WorldTest.place(level, pos, "sapling_ge");
        var oak = SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).createIndividual();
        sapling.setTree(oak);
        level.getChunkAt(pos).tryMarkSaved();
        sapling.onBlockTick(level, pos, sapling.getBlockState(), level.getRandom());
        h.assertTrue(level.getChunkAt(pos).isUnsaved(), "Sapling maturity marks chunk dirty");
        for (int i = 0; i < 30 && level.getBlockEntity(pos) == sapling; i++) sapling.tryGrow(level.getRandom(), true);
        h.assertTrue(level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS), "Real sapling generator grows a trunk");
        var leavesPos = h.absolutePos(new BlockPos(3, 4, 3));
        var leaves = (LeavesBlockEntity) N2WorldTest.place(level, leavesPos, "leaves");
        leaves.setTree(oak.copy());
        var pollenType = new TreePollenType();
        var birch = SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.BIRCH).createIndividual();
        level.getChunkAt(leavesPos).tryMarkSaved();
        h.assertTrue(pollenType.tryPollinate(level, leavesPos, birch, null) && leaves.isPollinated(), "Birch pollen fertilizes oak leaves");
        h.assertTrue(level.getChunkAt(leavesPos).isUnsaved(), "Pollination marks chunk dirty");
        var saved = leaves.saveCustomOnly(level.registryAccess());
        ValueIOUtil.readTag(saved, level.registryAccess(), leaves::loadCustomOnly);
        h.assertTrue(leaves.isPollinated() && leaves.getTree().getMate() != null, "Pollinated mate survives load");
        h.assertTrue(pollenType.tryCollectPollen(level, leavesPos, null) != null, "Tree pollen can be collected");
        var fruitTree = SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.SOUR_CHERRY).createIndividual();
        leaves.setTree(fruitTree);
        leaves.setFruit(fruitTree, true);
        h.assertTrue(leaves.hasFruit(), "Cherry leaves bear fruit");
        level.getChunkAt(leavesPos).tryMarkSaved();
        leaves.addRipeness(1);
        h.assertTrue(leaves.getRipeness() == 1 && level.getChunkAt(leavesPos).isUnsaved(), "Fruit ripening persists");
        h.assertTrue(!leaves.pickFruit(ItemStack.EMPTY).isEmpty() && leaves.getRipeness() == 0, "Ripe fruit harvest resets ripeness");
        h.assertTrue(leaves.pickFruit(ItemStack.EMPTY).isEmpty(), "Unripe leaves cannot harvest twice");
        h.succeed();
    }

    static void butterflies(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(8, 6, 8));
        var leaves = (LeavesBlockEntity) N2WorldTest.place(level, pos, "leaves");
        leaves.setTree(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).createIndividual());
        var caterpillar = SpeciesUtil.BUTTERFLY_TYPE.get().getAllSpecies().stream().map(s -> s.createIndividual())
            .filter(b -> b.canTakeFlight(level, pos.getX(), pos.getY(), pos.getZ())).findFirst().orElseThrow();
        h.assertTrue(caterpillar.spawnCaterpillar(leaves) == null, "Unmated butterflies cannot lay fertile caterpillars");
        caterpillar.setMate(caterpillar.getGenome());
        caterpillar = caterpillar.spawnCaterpillar(leaves);
        h.assertTrue(caterpillar != null, "Mated butterfly produces a nursery offspring");
        level.getChunkAt(pos).tryMarkSaved();
        leaves.setCaterpillar(caterpillar);
        h.assertTrue(level.getChunkAt(pos).isUnsaved(), "Nursery insertion marks chunk dirty");
        for (int i = 0; i < 1000 && leaves.getCaterpillar() != null; i++) leaves.onBlockTick(level, pos, leaves.getBlockState(), level.getRandom());
        h.assertTrue(level.getBlockEntity(pos.below()) instanceof TileCocoon, "Nursery grows a hanging cocoon");
        var cocoon = (TileCocoon) level.getBlockEntity(pos.below());
        level.getChunkAt(pos).tryMarkSaved();
        cocoon.onBlockTick();
        h.assertTrue(level.getChunkAt(pos).isUnsaved(), "Cocoon maturation marks chunk dirty");
        for (int i = 0; i < 1000 && level.getBlockEntity(pos.below()) == cocoon; i++) cocoon.onBlockTick();
        h.assertTrue(level.getBlockState(pos.below()).isAir(), "Mature cocoon hatches");
        var entities = level.getEntitiesOfClass(forestry.lepidopterology.entities.EntityButterfly.class, new net.minecraft.world.phys.AABB(pos).inflate(3));
        h.assertTrue(entities.size() == 1 && entities.getFirst().getButterfly().getSpecies().id().equals(caterpillar.getSpecies().id()), "Hatching creates one butterfly with the nursery species");
        h.succeed();
    }

    static void backpacks(GameTestHelper h) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        player.getInventory().clearContent();
        var pack = BackpackItems.MINER_BACKPACK.stack();
        player.getInventory().setItem(0, pack);
        var diamonds = new ItemStack(Items.DIAMOND, 17);
        BackpackItem.tryStowing(player, pack, diamonds);
        h.assertTrue(diamonds.isEmpty(), "Miner backpack stows accepted resources");
        var apples = new ItemStack(Items.APPLE, 3);
        BackpackItem.tryStowing(player, pack, apples);
        h.assertTrue(apples.getCount() == 3, "Miner backpack rejects food");
        BackpackMode.LOCKED.write(pack, CoreDataComponents.BACKPACK_MODE.get());
        var more = new ItemStack(Items.DIAMOND, 2);
        BackpackItem.tryStowing(player, pack, more);
        h.assertTrue(more.getCount() == 2, "Locked backpack rejects pickup");
        BackpackMode.RESUPPLY.write(pack, CoreDataComponents.BACKPACK_MODE.get());
        player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 63));
        BackpackResupplyHandler.resupply(player);
        h.assertTrue(player.getInventory().getItem(1).getCount() == 64, "Resupply tops up existing stack");
        var inventory = new BackpackInventory(player, 15, pack);
        h.assertTrue(inventory.getItem(0).getCount() == 16, "Resupply consumes exactly one stored item");
        var chestPos = h.absolutePos(new BlockPos(8, 2, 8));
        h.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) h.getLevel().getBlockEntity(chestPos);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pack);
        player.setShiftKeyDown(true);
        var context = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chestPos), Direction.UP, chestPos, false));
        ((BackpackItem) pack.getItem()).onItemUseFirst(pack, context);
        h.assertTrue(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 16, "Sneak-use deposits backpack contents into a real chest");
        h.assertTrue(new BackpackInventory(player, 15, pack).isEmpty(), "Chest transfer persisted the empty backpack");
        chest.setItem(1, new ItemStack(Items.APPLE, 3));
        BackpackMode.RECEIVE.write(pack, CoreDataComponents.BACKPACK_MODE.get());
        ((BackpackItem) pack.getItem()).onItemUseFirst(pack, context);
        h.assertTrue(chest.getItem(0).isEmpty() && chest.getItem(1).getCount() == 3
            && new BackpackInventory(player, 15, pack).getItem(0).getCount() == 16, "Receive mode retrieves accepted items and leaves rejected food");
        player.setShiftKeyDown(false);
        player.getInventory().clearContent();
        h.succeed();
    }

    static void blockedNursery(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(8, 8, 8));
        var leaves = (LeavesBlockEntity) N2WorldTest.place(level, pos, "leaves");
        leaves.setTree(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).createIndividual());
        var caterpillar = SpeciesUtil.BUTTERFLY_TYPE.get().getAllSpecies().stream().map(s -> s.createIndividual())
            .min(java.util.Comparator.comparingInt(b -> b.getGenome().getActiveValue(forestry.api.core.genetics.alleles.ButterflyChromosomes.METABOLISM))).orElseThrow();
        leaves.setCaterpillar(caterpillar);
        int matureTime = Math.round((float) caterpillar.getGenome().getActiveValue(forestry.api.core.genetics.alleles.ButterflyChromosomes.LIFESPAN)
            / (caterpillar.getGenome().getActiveValue(forestry.api.core.genetics.alleles.ButterflyChromosomes.FERTILITY) * 2));
        var saved = leaves.saveCustomOnly(level.registryAccess());
        saved.putInt("CATMAT", matureTime - 1);
        ValueIOUtil.readTag(saved, level.registryAccess(), leaves::loadCustomOnly);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = -5; y < 0; y++) {
            level.setBlockAndUpdate(pos.offset(x, y, z), Blocks.STONE.defaultBlockState());
        }
        leaves.onBlockTick(level, pos, leaves.getBlockState(), level.getRandom());
        h.assertTrue(leaves.getCaterpillar() != null, "Blocked cocoon placement retains the mature caterpillar");
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        leaves.onBlockTick(level, pos, leaves.getBlockState(), level.getRandom());
        h.assertTrue(leaves.getCaterpillar() == null && level.getBlockEntity(pos.below()) instanceof TileCocoon,
            "Opening space lets the retained caterpillar create one cocoon");
        h.succeed();
    }

    static void crops(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(8, 4, 8));
        var farm = (AbstractMinifarmBlockEntity) N2WorldTest.place(level, pos, "farm_crops_managed");
        farm.setManual(false);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            level.setBlockAndUpdate(pos.offset(x, -2, z), Blocks.STONE_BRICKS.defaultBlockState());
            level.setBlockAndUpdate(pos.offset(x, -1, z), Blocks.FARMLAND.defaultBlockState());
        }
        for (int i = 4; i < 8; i++) farm.setItem(i, new ItemStack(Items.WHEAT_SEEDS, 32));
        farm.setItem(12, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("forestry:fertilizer")), 64));
        farm.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 10000));
        farm.getEnergyManager().setEnergyStored(100000);
        for (int i = 0; i < 200; i++) farm.processWorkStep();
        int planted = 0;
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            var cropPos = pos.offset(x, 0, z);
            if (level.getBlockState(cropPos).is(Blocks.WHEAT)) {
                planted++;
                level.setBlockAndUpdate(cropPos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
            }
        }
        h.assertTrue(planted > 0, "Powered farm plants actual wheat (errors: " + farm.getErrorLogic().getErrors() + ")");
        farm.getEnergyManager().setEnergyStored(100000);
        for (int i = 0; i < 300; i++) farm.processWorkStep();
        int wheat = 0;
        for (int i = 8; i < 12; i++) if (farm.getItem(i).is(Items.WHEAT)) wheat += farm.getItem(i).getCount();
        h.assertTrue(wheat > 0 && farm.getTankManager().getTank(0).getFluidAmount() < 10000, "Farm harvests actual crop loot and consumes water");
        h.succeed();
    }
}
