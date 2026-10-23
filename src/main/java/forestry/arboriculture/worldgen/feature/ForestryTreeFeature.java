package forestry.arboriculture.worldgen.feature;

import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.arboriculture.commands.TreeGenHelper;
import forestry.core.platform.data.LogicalData;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class ForestryTreeFeature extends Feature<ForestryTreeFeatureConfig> {
	public ForestryTreeFeature() {
		super(ForestryTreeFeatureConfig.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<ForestryTreeFeatureConfig> context) {
		return LogicalData.withServerSnapshot(() -> {
			IGenome genome = context.config().genome();
			ITreeSpecies species = genome.getActiveSpecies().cast();
			return TreeGenHelper.generateTree(species, genome, context.level(), context.random(), context.origin());
		});
	}
}
