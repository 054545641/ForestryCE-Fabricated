package forestry.arboriculture.worldgen;

import forestry.api.IForestryApi;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.core.climate.IClimateManager;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.arboriculture.commands.TreeGenHelper;
import forestry.arboriculture.trees.genetics.Tree;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.data.LogicalData;
import forestry.core.platform.util.BlockUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TreeDecorator extends Feature<NoneFeatureConfiguration> {
	public TreeDecorator() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Nullable
	private static BlockPos getValidPos(WorldGenLevel world, int x, int z, ITree tree) {
		// get to the ground
		final BlockPos topPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
		if (topPos.getY() == 0) {
			return null;
		}

		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(topPos.getX(), topPos.getY(), topPos.getZ());

		BlockState blockState = world.getBlockState(pos);
		while (BlockUtil.canReplace(blockState, world, pos)) {
			pos.move(Direction.DOWN);
			if (pos.getY() <= 0) {
				return null;
			}

			blockState = world.getBlockState(pos);
		}

		if (Tree.canPlantTreeOn(blockState)) {
			return pos.above();
		}

		return null;
	}

	private static List<ITreeSpecies> getCandidates(Holder<Biome> biome) {
		IClimateManager manager = IForestryApi.INSTANCE.getClimateManager();
		TemperatureType temperature = manager.getTemperature(biome);
		HumidityType humidity = manager.getHumidity(biome);
		List<ITreeSpecies> candidates = new ArrayList<>();
		for (ITreeSpecies species : SpeciesUtil.getAllTreeSpecies()) {
			if (species.getRarity() > 0.0f && temperature == species.getTemperature() && humidity == species.getHumidity()) {
				candidates.add(species);
			}
		}
		return candidates;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		return LogicalData.withServerSnapshot(() -> placeTrees(context));
	}

	private boolean placeTrees(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource rand = context.random();
		BlockPos pos = context.origin();

		double globalRarity = ForestryConfig.SERVER.treesSpawnNaturally.get();
		if (globalRarity <= 0.0) {
			return false;
		}

		List<ITreeSpecies> candidates = getCandidates(level.getBiome(pos));

		for (int tries = 0; tries < 4; tries++) {
			int x = pos.getX() + rand.nextInt(16);
			int z = pos.getZ() + rand.nextInt(16);

			for (ITreeSpecies species : candidates) {
				if (species.getRarity() * globalRarity >= rand.nextFloat()) {
					BlockPos validPos = getValidPos(level, x, z, species.createIndividual());
					if (validPos == null) {
						continue;
					}

					if (TreeGenHelper.generateTree(species, null, level, context.random(), validPos)) {
						return true;
					}
				}
			}
		}

		return false;
	}
}
