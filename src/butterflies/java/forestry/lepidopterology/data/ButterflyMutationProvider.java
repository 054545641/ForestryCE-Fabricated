package forestry.lepidopterology.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import forestry.core.data.MutationProvider;

import static forestry.api.lepidopterology.ForestryButterflySpecies.BOMBYX_MORI;
import static forestry.api.lepidopterology.ForestryButterflySpecies.BRIMSTONE;
import static forestry.api.lepidopterology.ForestryButterflySpecies.LATTICED_HEATH;

/**
 * Generates the built-in butterfly mutations as {@code forestry:butterfly_mutation} recipe JSON.
 */
public class ButterflyMutationProvider extends MutationProvider {
	public ButterflyMutationProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider);
	}

	@Override
	protected void addMutations() {
		butterfly(LATTICED_HEATH, BRIMSTONE, BOMBYX_MORI, 0.07f);
	}
}
