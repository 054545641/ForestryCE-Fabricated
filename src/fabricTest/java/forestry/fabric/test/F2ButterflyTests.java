package forestry.fabric.test;

import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.core.platform.util.SpeciesUtil;
import forestry.lepidopterology.cocoons.TileCocoon;
import forestry.lepidopterology.entities.EntityButterfly;
import forestry.lepidopterology.features.LepidopterologyEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import static forestry.fabric.test.F1RegistrationTests.require;

public final class F2ButterflyTests {
    @GameTest public void matingRecipePreservesBothInputGenomes(GameTestHelper test) {
        var type = SpeciesUtil.BUTTERFLY_TYPE.get();
        var butterfly = type.getSpecies(ForestryButterflySpecies.MONARCH).createIndividual();
        var serum = type.getSpecies(ForestryButterflySpecies.CABBAGE_WHITE).createIndividual();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
            java.util.List.of(butterfly.createStack(ButterflyLifeStage.BUTTERFLY), serum.createStack(ButterflyLifeStage.SERUM)));
        var recipe = test.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, test.getLevel()).orElseThrow();
        require(recipe.id().identifier().equals(F1RegistrationTests.id("butterfly_mating")), "Native butterfly mating recipe missing");
        var output = recipe.value().assemble(input);
        var child = (forestry.api.lepidopterology.genetics.IButterfly) forestry.api.core.genetics.capability.IIndividualHandlerItem.getIndividual(output);
        require(child != null && child.getMate() != null && child.getSpecies().id().equals(ForestryButterflySpecies.MONARCH), "Mating output lost parent or mate");
        require(butterfly.getMate() == null && input.getItem(0).getCount() == 1 && input.getItem(1).getCount() == 1, "Crafting preview mutated its input");
        test.succeed();
    }

    private static boolean enabled(GameTestHelper test) {
        if (FabricLoader.getInstance().isModLoaded("forestrybutterflies")) return true;
        test.succeed(); return false;
    }
    @GameTest public void originalSpeciesAndGenomeComponents(GameTestHelper test) {
        if (!enabled(test)) return;
        var type = SpeciesUtil.BUTTERFLY_TYPE.get();
        require(type.getSpeciesCount() == ForestryButterflySpecies.ALL.size(), "Butterfly species were dropped");
        var butterfly = type.getSpecies(ForestryButterflySpecies.MONARCH).createIndividual();
        butterfly.setMate(type.getSpecies(ForestryButterflySpecies.CABBAGE_WHITE).getDefaultGenome());
        var ops = test.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (var stage : ButterflyLifeStage.values()) {
            var stack = butterfly.createStack(stage);
            var data = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
            require(ItemStack.matches(stack, ItemStack.CODEC.parse(ops, data).getOrThrow()), "Butterfly component lost for " + stage);
        }
        test.succeed();
    }
    @GameTest public void nativeEntityPersistsGenomeAndRejectsDimensionTravel(GameTestHelper test) {
        if (!enabled(test)) return;
        var type = SpeciesUtil.BUTTERFLY_TYPE.get(); var individual = type.getSpecies(ForestryButterflySpecies.MONARCH).createIndividual();
        var entity = EntityButterfly.create(LepidopterologyEntities.BUTTERFLY.entityType(), test.getLevel(), individual, test.absolutePos(BlockPos.ZERO));
        require(entity.getButterfly().getSpecies().id().equals(ForestryButterflySpecies.MONARCH), "Entity lost species");
        require(!entity.canTeleport(test.getLevel(), test.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER)), "Butterfly dimension restriction lost");
        var tag = new CompoundTag();
        try (var reporter = new net.minecraft.util.ProblemReporter.ScopedCollector(org.slf4j.LoggerFactory.getLogger("F2ButterflyTests"))) {
            var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, test.getLevel().registryAccess());
            entity.addAdditionalSaveData(output); tag = output.buildResult();
        }
        var copy = new EntityButterfly(LepidopterologyEntities.BUTTERFLY.entityType(), test.getLevel());
        forestry.core.platform.util.ValueIOUtil.readTag(tag, test.getLevel().registryAccess(), copy::readAdditionalSaveData);
        require(copy.getButterfly().getSpecies().id().equals(ForestryButterflySpecies.MONARCH), "Entity save lost butterfly genome");
        test.succeed();
    }
    @GameTest public void cocoonPreservesCaterpillarAndMaturation(GameTestHelper test) {
        if (!enabled(test)) return;
        var leafPos = new BlockPos(2, 4, 2); test.setBlock(leafPos, ArboricultureBlocks.LEAVES.block());
        var leaves = test.getBlockEntity(leafPos, LeavesBlockEntity.class);
        leaves.setTree(SpeciesUtil.TREE_TYPE.get().getDefaultSpecies().createIndividual());
        var type = SpeciesUtil.BUTTERFLY_TYPE.get(); var individual = type.getSpecies(ForestryButterflySpecies.MONARCH).createIndividual();
        var position = type.plantCocoon(test.getLevel(), test.absolutePos(leafPos), individual, 0, true);
        require(position != null, "Cocoon placement failed");
        var cocoon = (TileCocoon) test.getLevel().getBlockEntity(position); cocoon.onBlockTick();
        var saved = cocoon.saveWithFullMetadata(test.getLevel().registryAccess());
        var copy = (TileCocoon) BlockEntity.loadStatic(position, cocoon.getBlockState(), saved, test.getLevel().registryAccess());
        require(copy != null && copy.getCaterpillar().getSpecies().id().equals(ForestryButterflySpecies.MONARCH), "Cocoon genotype lost");
        require(copy.saveWithoutMetadata(test.getLevel().registryAccess()).getIntOr("CATMAT", -1) == 1, "Cocoon maturation lost");
        test.succeed();
    }
}
