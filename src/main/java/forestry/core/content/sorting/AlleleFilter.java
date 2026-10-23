package forestry.core.content.sorting;

import forestry.api.core.genetics.ISpecies;

import javax.annotation.Nullable;

public class AlleleFilter {
	@Nullable
	public ISpecies<?> activeSpecies;

	@Nullable
	public ISpecies<?> inactiveSpecies;

	public boolean isValid(ISpecies<?> active, ISpecies<?> inactive) {
		return (this.activeSpecies == null || active.id().equals(this.activeSpecies.id()))
			&& (this.inactiveSpecies == null || inactive.id().equals(this.inactiveSpecies.id()));
	}

	public boolean isEmpty() {
		return this.activeSpecies == null && this.inactiveSpecies == null;
	}
}
