package forestry.api.plugin;

import forestry.api.core.genetics.pollen.IPollenType;

public interface IPollenRegistration {
	void registerPollenType(IPollenType<?> pollenType);
}
