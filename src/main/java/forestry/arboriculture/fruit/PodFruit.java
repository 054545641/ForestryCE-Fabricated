package forestry.arboriculture.fruit;

import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.core.IProduct;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.fruit.ForestryPodType;
import forestry.core.platform.util.BlockUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

import java.util.List;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.fruit.Fruit;

// Fruits that grow on the side of a tree's trunk, like cocoa beans
public class PodFruit extends Fruit {
	private final ForestryPodType type;

	public PodFruit(boolean dominant, ForestryPodType type, List<IProduct> products) {
		super(dominant, 2, products);

		this.type = type;
	}

	@Override
	public boolean requiresFruitBlocks() {
		return true;
	}

	@Override
	public boolean trySpawnFruitBlock(IGenome genome, LevelAccessor world, RandomSource rand, BlockPos pos) {
		if (rand.nextFloat() > getFruitChance(genome, world)) {
			return false;
		}

		if (this.type == ForestryPodType.COCOA) {
			return BlockUtil.tryPlantCocoaPod(world, pos);
		} else {
			IFruit activeAllele = genome.resolveActive(TreeChromosomes.FRUIT);
			return SpeciesUtil.TREE_TYPE.get().setFruitBlock(world, genome, activeAllele, genome.getActiveValue(TreeChromosomes.YIELD), pos);
		}
	}

	@Override
	public TagKey<Block> getLogTag() {
		return switch (this.type) {
			case DATES -> ForestryWoodType.PALM.blockTag;
			case PAPAYA -> ForestryWoodType.PAPAYA.blockTag;
			case COCONUT -> ForestryWoodType.COCONUT.blockTag;
			default -> BlockTags.JUNGLE_LOGS;
		};
	}

	public ForestryPodType getType() {
		return this.type;
	}
}
