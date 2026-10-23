package forestry.lepidopterology.butterflies;

import forestry.api.core.genetics.IEffectData;
import forestry.api.lepidopterology.IButterflyEffect;
import forestry.api.lepidopterology.IEntityButterfly;

public class DummyButterflyEffect implements IButterflyEffect {
	@Override
	public boolean isDominant() {
		return false;
	}

	@Override
	public IEffectData doEffect(IEntityButterfly butterfly, IEffectData storedData) {
		return storedData;
	}
}
