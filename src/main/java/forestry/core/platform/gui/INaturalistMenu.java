package forestry.core.platform.gui;

import forestry.api.core.genetics.ISpeciesType;

public interface INaturalistMenu {
	ISpeciesType<?, ?> getSpeciesType();

	int getScrollRow();

	void setScrollRow(int row);
}
