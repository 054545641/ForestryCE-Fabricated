package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.apiculture.ForestryFlowerTypes;
import forestry.api.apiculture.IFlowerType;
import forestry.api.core.genetics.IFlowerTypeManager;
import forestry.core.engine.genetics.flowers.PhotosynthesisFlowerType;
import forestry.core.engine.genetics.flowers.TagFlowerType;
import forestry.core.engine.genetics.flowers.WaterTagFlowerType;
import forestry.core.engine.genetics.FlowerTypeManager;

@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlowerTypeTest {
	private static final Identifier[] ALL = {
		ForestryFlowerTypes.VANILLA, ForestryFlowerTypes.NETHER, ForestryFlowerTypes.CACTI,
		ForestryFlowerTypes.MUSHROOMS, ForestryFlowerTypes.END, ForestryFlowerTypes.JUNGLE,
		ForestryFlowerTypes.SNOW, ForestryFlowerTypes.WHEAT, ForestryFlowerTypes.GOURD,
		ForestryFlowerTypes.CAVE, ForestryFlowerTypes.PHOTOSYNTHESIS, ForestryFlowerTypes.ANCIENT,
		ForestryFlowerTypes.SEA, ForestryFlowerTypes.CORAL, ForestryFlowerTypes.SCULK,
	};

	// Butterflies carry the same flower type chromosome bees do, so resolving one must not route
	// through the bee species type. Before phase 9b it did, which meant lepidopterology installed
	// without apiculture could not project a single butterfly
	@GameTest(template = "empty")
	public static void flowerTypesResolveWithoutBeeSpeciesType(GameTestHelper helper) {
		for (Identifier id : ALL) {
			if (IForestryApi.INSTANCE.getFlowerTypeManager().getFlowerType(id) == null) {
				helper.fail("Flower type did not resolve from the core manager: " + id);
			}
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void allBuiltinsResolve(GameTestHelper helper) {
		IFlowerTypeManager bees = IForestryApi.INSTANCE.getFlowerTypeManager();
		for (Identifier id : ALL) {
			IFlowerType type = bees.getFlowerTypeSafe(id);
			if (type == null) {
				helper.fail("Built-in flower type did not load from datapack: " + id);
				return;
			}
		}
		// Spot-check serializer classes + dominance survived the JSON round-trip.
		if (!(bees.getFlowerType(ForestryFlowerTypes.END) instanceof TagFlowerType end) || end.biomes() == null) {
			helper.fail("END should be a TagFlowerType with a biomes tag");
			return;
		}
		if (!(bees.getFlowerType(ForestryFlowerTypes.SEA) instanceof WaterTagFlowerType)) {
			helper.fail("SEA should be a WaterTagFlowerType");
			return;
		}
		if (!(bees.getFlowerType(ForestryFlowerTypes.PHOTOSYNTHESIS) instanceof PhotosynthesisFlowerType)) {
			helper.fail("PHOTOSYNTHESIS should be a PhotosynthesisFlowerType");
			return;
		}
		if (!bees.getFlowerType(ForestryFlowerTypes.VANILLA).isDominant()) {
			helper.fail("VANILLA flower type should be dominant");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void tagFlowerTypeAcceptsTaggedBlock(GameTestHelper helper) {
		net.minecraft.core.BlockPos abs = helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 0));
		helper.getLevel().setBlockAndUpdate(abs, net.minecraft.world.level.block.Blocks.DANDELION.defaultBlockState());
		TagFlowerType tag = new TagFlowerType(net.minecraft.tags.BlockTags.FLOWERS, true);
		if (!tag.isAcceptableFlower(helper.getLevel(), abs)) {
			helper.fail("tag_flower_type should accept a #minecraft:flowers block");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void waterFlowerTypePlantablePositionIsWaterOnly(GameTestHelper helper) {
		net.minecraft.core.BlockPos water = helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 0));
		net.minecraft.core.BlockPos air = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 0));
		helper.getLevel().setBlockAndUpdate(water, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
		helper.getLevel().setBlockAndUpdate(air, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
		WaterTagFlowerType type = new WaterTagFlowerType(net.minecraft.tags.BlockTags.FLOWERS, false);
		if (!type.isPlantablePosition(helper.getLevel(), water) || type.isPlantablePosition(helper.getLevel(), air)) {
			helper.fail("water_tag_flower_type should be plantable only in water");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void datapackOverrideWinsAndUnionSwaps(GameTestHelper helper) {
		IFlowerTypeManager bees = IForestryApi.INSTANCE.getFlowerTypeManager();
		Identifier probe = ForestryConstants.forestry("gametest_probe_flower");
		// A synthetic datapack map that overlays the code base.
		java.util.Map<Identifier, IFlowerType> data = new java.util.HashMap<>();
		data.put(probe, new PhotosynthesisFlowerType());
		data.put(ForestryFlowerTypes.VANILLA, new TagFlowerType(net.minecraft.tags.BlockTags.FLOWERS, false)); // override: recessive
		// This mutation of the shared live flower-type map is safe only because mutate -> assert -> restore all run
		// synchronously in this method body with no tick yield between them, so no concurrent test observes the
		// transient state. Do not insert an await/runAtTickTime between the rebuild above and the finally restore.
		FlowerTypeManager.rebuild(data);
		try {
			if (!(bees.getFlowerTypeSafe(probe) instanceof PhotosynthesisFlowerType)) {
				helper.fail("datapack-only flower type should resolve after rebuild");
				return;
			}
			if (bees.getFlowerType(ForestryFlowerTypes.VANILLA).isDominant()) {
				helper.fail("datapack entry should override the built-in VANILLA dominance");
				return;
			}
		} finally {
			// Restore the real datapack-loaded map so later tests/gameplay are unaffected.
			FlowerTypeManager.rebuild(FlowerTypeManager.INSTANCE.getDefinitions());
		}
		helper.succeed();
	}
}
