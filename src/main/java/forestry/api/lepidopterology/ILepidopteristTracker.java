package forestry.api.lepidopterology;

import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.lepidopterology.genetics.IButterfly;

public interface ILepidopteristTracker extends IBreedingTracker {
	void registerCatch(IButterfly butterfly);
}
