package forestry.core.platform.worldgen;

import forestry.api.core.genetics.IGenome;
import forestry.core.platform.data.LogicalData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class AbstractForestryFeature extends Feature<NoneFeatureConfiguration> {
	protected AbstractForestryFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		return LogicalData.withServerSnapshot(() -> place(getDefaultGenome(), context.level(), context.random(), context.origin(), false));
	}

	public abstract IGenome getDefaultGenome();

	public abstract boolean place(IGenome genome, LevelAccessor level, RandomSource rand, BlockPos pos, boolean forced);
}
