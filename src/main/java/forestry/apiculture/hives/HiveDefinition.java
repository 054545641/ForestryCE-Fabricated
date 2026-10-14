package forestry.apiculture.hives;

import forestry.api.ForestryTags;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.hives.IHiveDefinition;
import forestry.api.apiculture.hives.IHivePlacement;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.core.ToleranceType;
import forestry.api.core.genetics.ClimateHelper;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// todo this should be data driven
public enum HiveDefinition implements IHiveDefinition {
	FOREST(ApicultureBlocks.HIVE.get(HiveBlockType.FOREST).defaultState(), 6.0f, ForestryBeeSpecies.FOREST, TreeHivePlacement.INSTANCE) {
		@Override
		public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
			postGenFlowers(level, rand, pos, flowerStates);
		}

		@Override
		public boolean isGoodBiome(Holder<Biome> biome) {
			//TODO: Forest bees now have slight cold tolerance. This tag restricts them to the warmer side. Should they require deciduous trees? investigate its not excluding the wrong biomes
			return super.isGoodBiome(biome) && !biome.is(Tags.Biomes.IS_SNOWY);
		}
	},
	MEADOWS(ApicultureBlocks.HIVE.get(HiveBlockType.MEADOWS).defaultState(), 1.0f, ForestryBeeSpecies.MEADOWS, new GroundHivePlacement(BlockTags.DIRT)) {
		@Override
		public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
			postGenFlowers(level, rand, pos, flowerStates);
		}

		@Override
		public boolean isGoodBiome(Holder<Biome> biome) {
			//TODO: find a good way to exclude meadows bee from forested areas. This tag seems to contain temperate forests. Sometimes they still generate in plain old forests for some reason but are rarer
			return super.isGoodBiome(biome) && !biome.is(BiomeTags.IS_FOREST);
		}
	},
	DESERT(ApicultureBlocks.HIVE.get(HiveBlockType.MODEST).defaultState(), 1.0f, ForestryBeeSpecies.MODEST, new GroundHivePlacement(ForestryTags.Blocks.MODEST_BEE_GROUND)) {
		@Override
		public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
			postGenFlowers(level, rand, pos, cactusStates);
		}
	},
	JUNGLE(ApicultureBlocks.HIVE.get(HiveBlockType.TROPICAL).defaultState(), 6.0f, ForestryBeeSpecies.TROPICAL, TreeHivePlacement.INSTANCE),
	END(ApicultureBlocks.HIVE.get(HiveBlockType.ENDER).defaultState(), 0.25f, ForestryBeeSpecies.ENDED, new GroundHivePlacement(ForestryTags.Blocks.ENDED_BEE_GROUND)) {
		@Override
		public boolean isGoodBiome(Holder<Biome> biome) {
			return biome.is(BiomeTags.IS_END);
		}
	},
	SNOW(ApicultureBlocks.HIVE.get(HiveBlockType.WINTRY).defaultState(), 2.0f, ForestryBeeSpecies.WINTRY, new GroundHivePlacement(ForestryTags.Blocks.WINTRY_BEE_GROUND)) {
		@Override
		public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
			BlockPos posAbove = pos.above();
			if (level.isEmptyBlock(posAbove)) {
				level.setBlock(posAbove, Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS);
			}

			postGenFlowers(level, rand, pos, flowerStates);
		}
	},
	SWAMP(ApicultureBlocks.HIVE.get(HiveBlockType.MARSHY).defaultState(), 2.0f, ForestryBeeSpecies.MARSHY, new GroundHivePlacement(BlockTags.DIRT)) {
		@Override
		public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
			postGenFlowers(level, rand, pos, mushroomStates);
		}

		@Override
		public boolean isGoodBiome(Holder<Biome> biome) {
			//No swamp bees bellow freezing
			return super.isGoodBiome(biome) && !biome.is(Tags.Biomes.IS_SNOWY);
		}
	},
	SAVANNA(ApicultureBlocks.HIVE.get(HiveBlockType.SAVANNA).defaultState(), 1.0f, ForestryBeeSpecies.SAVANNA, new GroundHivePlacement(BlockTags.DIRT)) {
		@Override
		public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
			//TODO: generate pumpkins in dry biomes and melons in normal ones
			//postGenFlowers(world,rand,pos,flowerStates);
		}
	},
	LUSH(ApicultureBlocks.HIVE.get(HiveBlockType.LUSH).defaultState(), 2.0F, ForestryBeeSpecies.LUSH, new CaveCeilingHivePlacement(ForestryTags.Blocks.LUSH_BEE_CEILING, ForestryTags.Blocks.CAVE_EXTRA_REPLACEABLES)) {
		@Override
		public boolean isGoodBiome(Holder<Biome> biome) {
			return super.isGoodBiome(biome) && biome.is(Tags.Biomes.IS_CAVE);
		}

		@Override
		public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
			if (level.getBlockState(pos.below()).canBeReplaced()) {
				level.setBlock(pos.below(), Blocks.CAVE_VINES.defaultBlockState().setValue(BlockStateProperties.BERRIES, rand.nextFloat() < 0.11F), Block.UPDATE_CLIENTS);
			}
		}
	},
	AQUATIC(ApicultureBlocks.HIVE.get(HiveBlockType.AQUATIC).defaultState(), 1.0F, ForestryBeeSpecies.AQUATIC, new OceanHivePlacement(BlockTags.SAND)) {
		@Override
		public boolean isGoodBiome(Holder<Biome> biome) {
			return biome.is(Biomes.WARM_OCEAN);
		}

		static final Block[] CORAL_FANS = new Block[]{Blocks.FIRE_CORAL_WALL_FAN, Blocks.BRAIN_CORAL_WALL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN, Blocks.HORN_CORAL_WALL_FAN, Blocks.TUBE_CORAL_WALL_FAN};
		static final Block[] CORAL_PLANTS = new Block[]{Blocks.FIRE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.HORN_CORAL_FAN, Blocks.TUBE_CORAL_FAN};

		@Override
		public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
			for (Direction direction : Direction.values()) {
				BlockPos pos2 = pos.relative(direction);
				if (direction.getAxis().isHorizontal() && level.getBlockState(pos2).getBlock() == Blocks.WATER) {
					level.setBlock(pos2, CORAL_FANS[rand.nextInt(5)].defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction), Block.UPDATE_CLIENTS);
				}
				if (level.getBlockState(pos.above()).getBlock() == Blocks.WATER) {
					level.setBlock(pos.above(), CORAL_PLANTS[rand.nextInt(5)].defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	},
	NETHER(ApicultureBlocks.HIVE.get(HiveBlockType.NETHER).defaultState(), 4.0F, ForestryBeeSpecies.EMBITTERED, new CaveCeilingHivePlacement(BlockTags.WART_BLOCKS, ForestryTags.Blocks.NETHER_EXTRA_REPLACEABLES)) {
		@Override
		public boolean isGoodBiome(Holder<Biome> biome) {
			return biome.is(BiomeTags.IS_NETHER);
		}
	},
	;

	private static final IHivePlacement FLOWER_GROUND = new GroundHivePlacement(ForestryTags.Blocks.PLANTABLE_FLOWERS_GROUND);
	private static final List<BlockState> flowerStates = new ArrayList<>();
	private static final List<BlockState> mushroomStates = new ArrayList<>();
	private static final List<BlockState> cactusStates = Collections.singletonList(Blocks.CACTUS.defaultBlockState());

	static {
		flowerStates.addAll(Blocks.POPPY.getStateDefinition().getPossibleStates());
		flowerStates.addAll(Blocks.DANDELION.getStateDefinition().getPossibleStates());
		mushroomStates.add(Blocks.RED_MUSHROOM.defaultBlockState());
		mushroomStates.add(Blocks.BROWN_MUSHROOM.defaultBlockState());
	}

	private final BlockState blockState;
	private final Identifier speciesId;
	private final IHivePlacement hiveGen;
	private final float defaultGenChance;

	HiveDefinition(BlockState hiveState, float defaultGenChance, Identifier beeTemplate, IHivePlacement hiveGen) {
		this.blockState = hiveState;
		this.defaultGenChance = defaultGenChance;
		this.speciesId = beeTemplate;
		this.hiveGen = hiveGen;
	}

	/**
	 * The chance value historically baked into each enum constant. Plugin registrations apply this through
	 * {@link forestry.api.plugin.IHiveBuilder#setGenerationChance} now that {@code IHiveDefinition.getGenChance}
	 * has been removed; exposing it as a field keeps the per-hive tuning numbers next to the rest of the
	 * definition instead of scattering them across {@code DefaultForestryPlugin}.
	 */
	public float defaultGenChance() {
		return this.defaultGenChance;
	}

	@Override
	public IHivePlacement getHiveGen() {
		return this.hiveGen;
	}

	@Override
	public BlockState getBlockState() {
		return this.blockState;
	}

	@Override
	public boolean isGoodBiome(Holder<Biome> biome) {
		return !biome.is(BiomeTags.IS_NETHER);
	}

	@Override
	public boolean isGoodHumidity(HumidityType humidity) {
		IBeeSpecies species = SpeciesUtil.getBeeSpecies(this.speciesId);
		HumidityType idealHumidity = species.getHumidity();
		ToleranceType humidityTolerance = species.getDefaultGenome().getActiveValue(BeeChromosomes.HUMIDITY_TOLERANCE);
		return ClimateHelper.isWithinLimits(humidity, idealHumidity, humidityTolerance);
	}

	@Override
	public boolean isGoodTemperature(TemperatureType temperature) {
		IBeeSpecies species = SpeciesUtil.getBeeSpecies(this.speciesId);
		TemperatureType idealTemperature = species.getTemperature();
		ToleranceType temperatureTolerance = species.getDefaultGenome().getActiveValue(BeeChromosomes.TEMPERATURE_TOLERANCE);
		return ClimateHelper.isWithinLimits(temperature, idealTemperature, temperatureTolerance);
	}

	@Override
	public void postGen(WorldGenLevel level, RandomSource rand, BlockPos pos) {
	}

	protected static void postGenFlowers(WorldGenLevel world, RandomSource rand, BlockPos hivePos, List<BlockState> flowerStates) {
		int plantedCount = 0;
		for (int i = 0; i < 10; i++) {
			int xOffset = rand.nextInt(8) - 4;
			int zOffset = rand.nextInt(8) - 4;
			BlockPos blockPos = hivePos.offset(xOffset, 0, zOffset);
			if ((xOffset == 0 && zOffset == 0) || !world.hasChunkAt(blockPos)) {
				continue;
			}

			blockPos = FLOWER_GROUND.getPosForHive(world, rand, blockPos.getX(), blockPos.getZ());
			if (blockPos == null) {
				continue;
			}

			BlockState state = flowerStates.get(rand.nextInt(flowerStates.size()));
			Block block = state.getBlock();
			if (!block.defaultBlockState().canSurvive(world, blockPos)) {
				continue;
			}

			world.setBlock(blockPos, state, Block.UPDATE_CLIENTS);
			plantedCount++;

			if (plantedCount >= 3) {
				break;
			}
		}
	}
}
