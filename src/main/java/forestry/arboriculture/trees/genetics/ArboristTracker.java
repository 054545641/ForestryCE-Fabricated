package forestry.arboriculture.trees.genetics;

import com.mojang.authlib.GameProfile;
import forestry.api.arboriculture.IArboristTracker;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.ISpecies;
import forestry.core.engine.genetics.BreedingTracker;
import forestry.core.platform.advancements.ForestryAdvancementTriggers;
import net.minecraft.world.level.Level;

public class ArboristTracker extends BreedingTracker implements IArboristTracker {
	public ArboristTracker() {
		super(ForestrySpeciesTypes.TREE);
	}

	@Override
	public void registerPickup(ISpecies<?> species) {
	}

	@Override
	public void registerProgress(Level level, GameProfile profile, ISpecies<?> species) {
		double researchPercentage = (double) getSpeciesBred() / species.getType().getSpeciesCount();
		ForestryAdvancementTriggers.ARBORICULTURE_RESEARCH.trigger(level, profile, researchPercentage);
	}
}
