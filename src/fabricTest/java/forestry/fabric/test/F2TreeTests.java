package forestry.fabric.test;

import forestry.api.IForestryApi;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.arboriculture.sapling.SaplingBlockEntity;
import forestry.arboriculture.trees.TreeUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import static forestry.fabric.test.F1RegistrationTests.require;

public final class F2TreeTests {
    @GameTest public void fiftySpeciesAndTreeGenomePersistence(GameTestHelper test) {
        var type = SpeciesUtil.TREE_TYPE.get();
        require(type.getSpeciesCount() == 50, "Tree definitions missing");
        var oak = type.getSpecies(ForestryTreeSpecies.OAK);
        require(!type.getMutations().getMutationsFrom(oak).isEmpty(), "Tree mutation recipes not projected");
        var tree = oak.createIndividual(); tree.setMate(type.getSpecies(ForestryTreeSpecies.BIRCH).getDefaultGenome());
        var stack = tree.createStack(TreeLifeStage.SAPLING);
        var ops = test.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var data = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        require(ItemStack.matches(stack, ItemStack.CODEC.parse(ops, data).getOrThrow()), "Tree genome/mate lost in item component persistence");
        require(IForestryApi.INSTANCE.getTreeManager().getRegisteredWoodTypes().size() >= 40, "Original wood bindings missing");
        test.succeed();
    }

    @GameTest public void saplingBlockEntityAndOwnerSurviveNativeSave(GameTestHelper test) {
        var type = SpeciesUtil.TREE_TYPE.get();
        var tree = type.getSpecies(ForestryTreeSpecies.LIME).createIndividual();
        var pos = new BlockPos(2, 2, 2); test.setBlock(pos.below(), Blocks.DIRT);
        var owner = new com.mojang.authlib.GameProfile(java.util.UUID.fromString("e7a12f56-8797-4e99-8d1f-a77822210005"), "F2Arborist");
        require(type.plantSapling(test.getLevel(), tree, owner, test.absolutePos(pos)), "Real sapling placement failed");
        var sapling = test.getBlockEntity(pos, SaplingBlockEntity.class);
        var saved = sapling.saveWithFullMetadata(test.getLevel().registryAccess());
        var copy = (SaplingBlockEntity) BlockEntity.loadStatic(sapling.getBlockPos(), sapling.getBlockState(), saved, test.getLevel().registryAccess());
        require(copy != null && copy.getTree().getSpecies().id().equals(ForestryTreeSpecies.LIME), "Native block entity save lost tree species");
        require(copy.getOwnerHandler().getOwner().equals(owner), "Sapling ownership was lost");
        test.succeed();
    }

    @GameTest public void leafPollinationAndNurseryPayloadPersist(GameTestHelper test) {
        var type = SpeciesUtil.TREE_TYPE.get();
        var pos = new BlockPos(3, 3, 3); test.setBlock(pos, ArboricultureBlocks.LEAVES.block());
        var leaves = test.getBlockEntity(pos, LeavesBlockEntity.class);
        leaves.setTree(type.getSpecies(ForestryTreeSpecies.OAK).createIndividual());
        require(TreeUtil.tryMate(leaves, type.getSpecies(ForestryTreeSpecies.BIRCH).createIndividual()), "Leaf pollination failed");
        var saved = leaves.saveWithFullMetadata(test.getLevel().registryAccess());
        var copy = (LeavesBlockEntity) BlockEntity.loadStatic(leaves.getBlockPos(), leaves.getBlockState(), saved, test.getLevel().registryAccess());
        require(copy != null && copy.getTree().getMate() != null, "Pollinated leaf lost mate on reload");
        require(IForestryApi.INSTANCE.getPollenManager().getAllPollenTypes().size() >= 1, "Tree pollen manager not installed");
        test.succeed();
    }
}
