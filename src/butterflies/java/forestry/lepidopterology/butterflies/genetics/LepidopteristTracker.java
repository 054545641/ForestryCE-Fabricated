package forestry.lepidopterology.butterflies.genetics;

import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.ISpecies;
import forestry.api.lepidopterology.ILepidopteristTracker;
import forestry.api.lepidopterology.genetics.IButterfly;
import forestry.core.engine.genetics.BreedingTracker;

public class LepidopteristTracker extends BreedingTracker implements ILepidopteristTracker {
	public LepidopteristTracker() {
		super(ForestrySpeciesTypes.BUTTERFLY);
	}

	@Override
	public void registerCatch(IButterfly butterfly) {
		registerSpecies(butterfly.getSpecies());
		registerSpecies(butterfly.getInactiveSpecies());
	}

	@Override
	public void registerPickup(ISpecies<?> species) {
		registerSpecies(species);
	}
}
