package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.apiculture.bees.AbstractBeeHousingBlockEntity;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.arboriculture.sapling.SaplingBlockEntity;
import forestry.arboriculture.trees.genetics.TreePollenType;
import forestry.core.content.backpacks.features.BackpackItems;
import forestry.core.content.backpacks.items.BackpackItem;
import forestry.core.platform.util.SpeciesUtil;
import forestry.lepidopterology.cocoons.TileCocoon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static forestry.lifecycle.PostalTests.check;

/** Saves actual gameplay progress and compares chunks in a second JVM, without restoring from the oracle. */
@EventBusSubscriber(modid = LifecycleTests.ID)
public final class N4RuntimeTest {
    private static final BlockPos SAPLING = new BlockPos(3, 80, 3);
    private static final BlockPos LEAVES = new BlockPos(9, 84, 3);
    private static final BlockPos COCOON = new BlockPos(9, 83, 3);
    private static final BlockPos APIARY = new BlockPos(3, 80, 9);
    private static final BlockPos CHEST = new BlockPos(9, 80, 9);
    private static final List<BlockPos> POSITIONS = List.of(SAPLING, LEAVES, COCOON, APIARY, CHEST);

    @SubscribeEvent
    public static void started(ServerStartedEvent event) throws Exception {
        String phase = System.getProperty("forestry.lifecycle.n4Phase", "");
        if (phase.isEmpty()) return;
        var server = event.getServer();
        var level = server.overworld();
        try {
            for (var pos : POSITIONS) level.getChunk(pos);
            if (phase.equals("seed")) {
                for (var pos : POSITIONS) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                var oak = SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).createIndividual();
                level.setBlockAndUpdate(SAPLING.below(), Blocks.DIRT.defaultBlockState());
                var sapling = (SaplingBlockEntity) N2WorldTest.place(level, SAPLING, "sapling_ge");
                sapling.setTree(oak);
                sapling.onBlockTick(level, SAPLING, sapling.getBlockState(), level.getRandom());
                var leaves = (LeavesBlockEntity) N2WorldTest.place(level, LEAVES, "leaves");
                leaves.setTree(oak.copy());
                check(new TreePollenType().tryPollinate(level, LEAVES, SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.BIRCH).createIndividual(), null), "Pollinated saved leaves");
                var caterpillar = SpeciesUtil.BUTTERFLY_TYPE.get().getDefaultSpecies().createIndividual();
                leaves.setCaterpillar(caterpillar);
                leaves.onBlockTick(level, LEAVES, leaves.getBlockState(), level.getRandom());
                var cocoon = (TileCocoon) N2WorldTest.place(level, COCOON, "cocoon_solid");
                cocoon.setCaterpillar(caterpillar.copy());
                cocoon.onBlockTick();
                var housing = (AbstractBeeHousingBlockEntity) N2WorldTest.place(level, APIARY, "apiary");
                var bees = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST);
                housing.setItem(0, bees.createStack(BeeLifeStage.PRINCESS));
                housing.setItem(1, bees.createStack(BeeLifeStage.DRONE));
                for (int i = 0; i < 20; i++) {
                    check(housing.getBeekeepingLogic().canWork(), "Breeding before save");
                    housing.getBeekeepingLogic().doWork();
                }
                var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
                var backpack = BackpackItems.MINER_BACKPACK.stack();
                player.getInventory().setItem(0, backpack);
                var diamonds = new ItemStack(Items.DIAMOND, 17);
                BackpackItem.tryStowing(player, backpack, diamonds);
                check(diamonds.isEmpty(), "Backpack accepted saved diamonds");
                level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
                ((ChestBlockEntity) level.getBlockEntity(CHEST)).setItem(0, backpack.copy());
                player.getInventory().clearContent();
                NbtIo.writeCompressed(snapshot(level), Path.of("n4-expected.nbt"));
            } else if (phase.equals("verify")) {
                check(Files.exists(Path.of("n4-expected.nbt")), "Run N4 seed first");
                var expected = NbtIo.readCompressed(Path.of("n4-expected.nbt"), NbtAccounter.unlimitedHeap());
                var actual = snapshot(level);
                for (var pos : POSITIONS) check(expected.get(pos.toShortString()).equals(actual.get(pos.toShortString())), "Actual saved gameplay state changed at " + pos);
                var leaves = (LeavesBlockEntity) level.getBlockEntity(LEAVES);
                check(leaves.isPollinated() && leaves.getCaterpillar() != null, "Pollination and nursery coexist after restart");
                var sapling = (SaplingBlockEntity) level.getBlockEntity(SAPLING);
                for (int i = 0; i < 30 && level.getBlockEntity(SAPLING) == sapling; i++) sapling.tryGrow(level.getRandom(), true);
                check(level.getBlockState(SAPLING).is(net.minecraft.tags.BlockTags.LOGS), "Saved sapling continues growing");
                var housing = (AbstractBeeHousingBlockEntity) level.getBlockEntity(APIARY);
                for (int i = 0; i < 2000 && !housing.getItem(1).isEmpty(); i++) housing.getBeekeepingLogic().doWork();
                check(housing.getItem(1).isEmpty() && forestry.api.core.genetics.capability.IIndividualHandlerItem.getLifeStage(housing.getItem(0)) == BeeLifeStage.QUEEN, "Saved breeding progress finishes with one drone consumed");
            } else throw new IllegalArgumentException("Unknown N4 phase: " + phase);
            server.saveEverything(false, true, true);
            Files.writeString(Path.of("n4-" + phase + ".passed"), "five actual block entities, gameplay progress and restart\n");
            LogUtils.getLogger().info("FORESTRY_N4_WORLD_READY phase={} actual_chunks=true gameplay_progress=true", phase);
        } finally { server.halt(false); }
    }

    private static CompoundTag snapshot(ServerLevel level) {
        var result = new CompoundTag();
        for (var pos : POSITIONS) {
            check(level.getBlockEntity(pos) != null, "Gameplay block entity exists: " + pos);
            result.put(pos.toShortString(), level.getBlockEntity(pos).saveCustomOnly(level.registryAccess()));
        }
        return result;
    }
}
