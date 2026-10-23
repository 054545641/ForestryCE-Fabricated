package forestry.apiculture.bees.genetics.effects;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;
import forestry.apiculture.bees.genetics.Bee;
import forestry.core.platform.util.VecUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;

public class SculkSpreadBeeEffect extends ThrottledBeeEffect {
	public SculkSpreadBeeEffect() {
		super(false, 200, true, true);
	}

	@Override
	IEffectData doEffectThrottled(IGenome genome, IEffectData storedData, IBeeHousing housing) {
		Level level = housing.getLevel();
		if (level.isClientSide()) {
			return storedData;
		}
		RandomSource random = level.getRandom();
		Vec3i area = Bee.getParticleArea(genome, housing);

		BlockPos randomPos = VecUtil.getRandomPositionInArea(random, area);

		BlockPos posBlock = randomPos.offset(housing.getBlockPos()).offset(VecUtil.center(area));

		if (level.hasChunkAt(posBlock)) {
			BlockState state = level.getBlockState(posBlock);

			if (state.isAir() && !level.getBlockState(posBlock.below()).isAir()) {
				SculkSpreader spreader = SculkSpreader.createLevelSpreader();
				spreader.addCursors(posBlock, random.nextInt(5) + 1);
				spreader.updateCursors(level, housing.getBlockPos(), random, true);
				spreader.updateCursors(level, housing.getBlockPos(), random, true);
				spreader.updateCursors(level, housing.getBlockPos(), random, true);
				spreader.updateCursors(level, housing.getBlockPos(), random, true);
				spreader.updateCursors(level, housing.getBlockPos(), random, true);
			} else if (state.getBlock() == Blocks.SCULK_SHRIEKER) {
				level.setBlockAndUpdate(posBlock.above(), Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true));
			}
		}

		return storedData;
	}
}
