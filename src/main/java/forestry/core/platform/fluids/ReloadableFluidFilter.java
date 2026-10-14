package forestry.core.platform.fluids;

import forestry.core.platform.recipes.RecipeManagers;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Supplier;

public class ReloadableFluidFilter implements Supplier<Set<Identifier>> {
	private final Supplier<Set<Identifier>> filterGetter;
	private int recipeReload;
	@Nullable
	private Set<Identifier> cachedFilter;

	public ReloadableFluidFilter(Supplier<Set<Identifier>> filterGetter) {
		this.filterGetter = filterGetter;
	}

	@Override
	public Set<Identifier> get() {
		int currentReloads = RecipeManagers.getRecipeReloads();
		// if null OR recipe manager has updated, create the cache
		if (currentReloads != this.recipeReload || this.cachedFilter == null) {
			this.recipeReload = currentReloads;
			this.cachedFilter = this.filterGetter.get();
		}
		return this.cachedFilter;
	}
}
