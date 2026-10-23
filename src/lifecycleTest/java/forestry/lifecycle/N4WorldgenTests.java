package forestry.lifecycle;

import forestry.api.IForestryApi;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.apiculture.apiary.ApiaryBlockEntity;
import forestry.apiculture.hives.HiveDecorator;
import forestry.apiculture.hives.HiveDefinition;
import forestry.apiculture.worldgen.ApiaristPoolElement;
import forestry.gametest.MultiblockTestSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.ArrayList;

final class N4WorldgenTests {
    static void farmSize(GameTestHelper h, int width, int depth) {
        var level = h.getLevel();
        var base = h.absolutePos(new BlockPos(8, 3, 8));
        var members = new ArrayList<BlockPos>();
        for (int x = 0; x < width; x++) for (int z = 0; z < depth; z++) for (int y = 0; y < 4; y++) {
            var pos = base.offset(x, y, z);
            N2WorldTest.place(level, pos, x == 0 && y == 0 && z == 0 ? "stone_brick_farm_gearbox" : "stone_brick_farm_block");
            members.add(pos);
        }
        var corner = base.offset(width - 1, 3, depth - 1);
        var state = level.getBlockState(corner);
        h.startSequence().thenExecuteAfter(8, () -> {
            h.assertTrue(MultiblockTestSupport.isAssembled(level, base), "Farm assembles at " + width + "x4x" + depth);
            MultiblockTestSupport.insertItem(level, members, 22, 12, new ItemStack(Items.DIAMOND, 7));
            level.destroyBlock(corner, false);
        }).thenExecuteAfter(5, () -> {
            h.assertTrue(!MultiblockTestSupport.isAssembled(level, base), "Breaking a corner deactivates the farm");
            level.setBlockAndUpdate(corner, state);
        }).thenExecuteAfter(8, () -> {
            h.assertTrue(MultiblockTestSupport.isAssembled(level, base), "Farm reassembles after corner replacement");
            h.assertTrue(MultiblockTestSupport.keptAcross(level, members, Items.DIAMOND) == 7, "Rebuilt farm keeps inventory exactly once");
            h.assertTrue(MultiblockTestSupport.newDropCount(level, MultiblockTestSupport.dropBox(members), java.util.Set.of(), Items.DIAMOND) == 0, "No duplicate inventory spills on rebuild");
        }).thenSucceed();
    }

    static void hivesAndOres(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(16, 10, 16));
        h.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level, origin.offset(-8, -8, -8), origin.offset(8, 8, 8),
            level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS)).left().isPresent(), "Worldgen fixture biome set");
        var random = RandomSource.create(7042);
        var hive = IForestryApi.INSTANCE.getHiveManager().getHives().stream()
            .filter(candidate -> candidate.getDefinition() == HiveDefinition.MEADOWS).findFirst().orElseThrow();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.DIRT.defaultBlockState());
		// The GameTest arena has a barrier ceiling, which is not part of natural terrain.
		for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = 0; y < 40; y++) {
			level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
		}
		// WORLD_SURFACE_WG is a generation-time map, not maintained after a chunk becomes FULL.
		net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(level.getChunkAt(origin), java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG));
        var candidate = hive.getPosForHive(level, random, origin.getX(), origin.getZ());
        h.assertTrue(origin.equals(candidate), "Generation heightmap targets dirt surface: expected=" + origin + " actual=" + candidate);
        h.assertTrue(HiveDecorator.tryGenHive(level, random, origin.getX(), origin.getZ(), hive), "Real meadows hive placement succeeds on dirt in plains");
        h.assertTrue(level.getBlockState(origin).is(hive.getHiveBlockState().getBlock()), "Hive feature places the registered hive block");
        h.assertTrue(!HiveDecorator.tryGenHive(level, random, origin.getX(), origin.getZ(), hive), "Hive feature cannot replace its own hive");
        var configured = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        for (String ore : new String[]{"apatite", "tin"}) {
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) for (int y = -6; y <= 6; y++) {
                level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE.defaultBlockState());
            }
            var feature = configured.getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.parse("forestry:ore_" + ore))).value();
            h.assertTrue(feature.place(level, level.getChunkSource().getGenerator(), random, origin), "Configured " + ore + " feature places a vein");
            int count = 0;
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse("forestry:" + ore + "_ore"));
            for (var pos : BlockPos.betweenClosed(origin.offset(-6, -6, -6), origin.offset(6, 6, 6))) if (level.getBlockState(pos).is(block)) count++;
            h.assertTrue(count > 0, "World contains generated " + ore + " ore");
        }
        var plains = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS).value();
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        for (String id : new String[]{"hive", "tree", "ore_apatite", "ore_tin"}) {
            var feature = placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Identifier.parse("forestry:" + id)));
            h.assertTrue(plains.getGenerationSettings().features().stream().anyMatch(set -> set.contains(feature)), "Biome modifier attaches " + id + " to plains");
        }
        h.succeed();
    }

    static void village(GameTestHelper h) {
        var level = h.getLevel();
        var manager = level.getStructureManager();
        var pools = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var origin = h.absolutePos(new BlockPos(5, 3, 5));
        for (String biome : new String[]{"plains", "snowy", "savanna", "desert", "taiga"}) {
            var pool = pools.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, Identifier.parse("minecraft:village/" + biome + "/houses"))).value();
            var entries = pool.getTemplates().stream().filter(entry -> entry.getFirst() instanceof ApiaristPoolElement).toList();
            h.assertTrue(entries.size() == 1 && entries.getFirst().getSecond() == 15, "Exactly one weighted apiarist house in " + biome);
            var house = (ApiaristPoolElement) entries.getFirst().getFirst();
            var bounds = house.getBoundingBox(manager, origin, Rotation.NONE);
            h.assertTrue(house.place(manager, level, level.structureManager(), level.getChunkSource().getGenerator(), origin, origin,
                Rotation.NONE, bounds, RandomSource.create(710), LiquidSettings.IGNORE_WATERLOGGING, false), "Apiarist house template places: " + biome);
            int apiaries = 0;
            for (var pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                if (level.getBlockEntity(pos) instanceof ApiaryBlockEntity apiary) {
                    apiaries++;
                    h.assertTrue(IIndividualHandlerItem.getLifeStage(apiary.getItem(0)) == BeeLifeStage.QUEEN, "Village data marker supplies real queen");
                    h.assertTrue(!apiary.getItem(9).isEmpty() && !apiary.getItem(11).isEmpty(), "Village data marker supplies frames");
                }
            }
            h.assertTrue(apiaries > 0, "Village template contains an initialized apiary: " + biome);
        }
        h.succeed();
    }
}
