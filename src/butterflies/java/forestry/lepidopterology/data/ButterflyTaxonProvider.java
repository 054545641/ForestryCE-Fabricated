package forestry.lepidopterology.data;

import java.util.Set;

import net.minecraft.data.PackOutput;

import forestry.apiimpl.plugin.GeneticRegistration;
import forestry.core.data.TaxonProvider;
import forestry.core.data.taxonomy.ForestryTaxonomy;

/**
 * Generates the lepidoptera subtree of {@code data/forestry/taxon}. Everything above the order (the
 * domains, kingdoms and the arthropod to insect spine) is shared with bees and ships in core, so the
 * spine is built here only to hang the order off and is then subtracted rather than written again.
 */
public class ButterflyTaxonProvider extends TaxonProvider {
	public ButterflyTaxonProvider(PackOutput output) {
		super(output);
	}

	// Collector used by seedLiveTaxaForDatagen, same as the one in TaxonProvider
	private ButterflyTaxonProvider() {
	}

	@Override
	protected void addTaxa() {
		GeneticRegistration spineOnly = new GeneticRegistration();
		ForestryTaxonomy.defineSpine(spineOnly);
		Set<String> shared = spineOnly.buildTaxa().keySet();

		GeneticRegistration genetics = new GeneticRegistration();
		ForestryTaxonomy.defineSpine(genetics);
		ButterflyTaxonomy.defineTaxa(genetics);
		genetics.buildTaxa().forEach((name, taxon) -> {
			if (!shared.contains(name)) {
				add(toDefinition(taxon));
			}
		});
	}

	/**
	 * Populates the live taxonomy with the lepidoptera subtree, on top of the taxa core has already seeded. Only for
	 * use by the standalone data generator ({@link LepidopterologyData#gather}); must run before the butterfly species
	 * are seeded, because a species resolves its genus through the taxonomy.
	 */
	public static void seedLiveTaxaForDatagen() {
		seedLiveTaxa(new ButterflyTaxonProvider());
	}
}
