package forestry.core.data;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.ITaxon;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.core.engine.genetics.GeneticsReloadHandler;
import forestry.core.engine.genetics.TaxonDefinition;
import forestry.core.data.taxonomy.ForestryTaxonomy;

/**
 * Generates {@code data/forestry/taxon/*.json} for base Forestry's whole taxonomy (domains down to genera, including the
 * genus default chromosomes bee species inherit). This provider is the single source of truth for the built-in taxa
 * (they are no longer code-registered at runtime); it must stay in sync with {@link ForestryTaxonomy}.
 * <p>
 * Add-on mods generate their own taxa by subclassing and overriding {@link #addTaxa()}, mirroring {@link MutationProvider}
 * and {@link FlowerTypeProvider}.
 */
public class TaxonProvider implements DataProvider {
	// Every definition seeded into the live taxonomy so far, core's first. Held across calls because the taxa are
	// re-applied as a whole, see seedLiveTaxa
	private static final List<TaxonDefinition> SEEDED = new ArrayList<>();

	private final PackOutput.PathProvider path;
	private final Map<Identifier, TaxonDefinition> pending = new LinkedHashMap<>();

	public TaxonProvider(PackOutput output) {
		this.path = output.createPathProvider(PackOutput.Target.DATA_PACK, "taxon");
	}

	// Collector used by seedLiveTaxaForDatagen: gathers the built-ins via addTaxa() without needing a PackOutput to
	// write to (it never runs the provider). Never call this to write JSON - path is null.
	protected TaxonProvider() {
		this.path = null;
	}

	/**
	 * Add your taxa here. Make sure NOT to call the super constructor in your mod.
	 */
	protected void addTaxa() {
		ForestryTaxonomy.buildDefaultTaxa().values().forEach(taxon -> add(toDefinition(taxon)));
	}

	protected void add(TaxonDefinition def) {
		this.pending.put(Identifier.fromNamespaceAndPath(ForestryConstants.MOD_ID, def.name()), def);
	}

	// Converts a live taxon into its serializable definition. Reference-chromosome defaults are stored by id only
	// (their dominance is intrinsic to the referenced value and re-resolved at projection time), so they serialize
	// without a dominance flag; data-chromosome defaults keep their inline allele (dominance included).
	protected static TaxonDefinition toDefinition(ITaxon taxon) {
		String parent = taxon.parent() == null ? null : taxon.parent().name();
		Map<IChromosome<?>, ITaxon.TaxonAllele> defaults = taxon.alleles();
		if (defaults.isEmpty()) {
			return new TaxonDefinition(parent, taxon.name(), taxon.rank(), null, Map.of());
		}

		// Sorted by chromosome id so the generated JSON is deterministic (taxon.alleles() is an IdentityHashMap).
		Map<Identifier, Allele<?>> alleles = new TreeMap<>(Comparator.comparing(Identifier::toString));
		for (Map.Entry<IChromosome<?>, ITaxon.TaxonAllele> entry : defaults.entrySet()) {
			IChromosome<?> chromosome = entry.getKey();
			ITaxon.TaxonAllele taxonAllele = entry.getValue();
			Allele<?> allele = taxonAllele.reference() != null
				? Allele.reference(taxonAllele.reference())
				: taxonAllele.allele();
			alleles.put(chromosome.id(), allele);
		}

		ISpeciesType<?, ?> type = findType(defaults.keySet().iterator().next());
		Identifier typeId = type == null ? null : type.id();
		return new TaxonDefinition(parent, taxon.name(), taxon.rank(), typeId, alleles);
	}

	// The species type whose karyotype owns the given chromosome: needed so the alleles field can be serialized against
	// that karyotype. All of a taxon's default chromosomes belong to the same karyotype, so the first suffices.
	@Nullable
	private static ISpeciesType<?, ?> findType(IChromosome<?> chromosome) {
		for (ISpeciesType<?, ?> type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			if (type.getKaryotype().contains(chromosome)) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Populates the live taxonomy directly from {@link #addTaxa()}, bypassing the datapack JSON round trip. Only for use
	 * by the standalone data generator ({@code Data#preDataGen}): a data-generator invocation never fires the datapack
	 * reload that loads taxa at real server start, but a species' genus must resolve to a taxon when the species is
	 * seeded for datagen. Mirrors {@code FlowerTypeProvider#seedLiveFlowerTypesForDatagen}; must run before the species
	 * providers seed their live species.
	 */
	public static void seedLiveTaxaForDatagen() {
		SEEDED.clear();
		seedLiveTaxa(new TaxonProvider());
	}

	/**
	 * Seeds the taxa a collector defines on top of the taxa already seeded, then applies the whole accumulated set.
	 * A content jar's provider seeds its own subtree through here, after core has seeded the shared ancestors.
	 * <p>
	 * {@code GeneticManager#applyDatapackTaxa} rebuilds the live taxonomy from the code-registered base on every
	 * call rather than adding to what is already live, so a subtree applied on its own would throw away every taxon
	 * core seeded. Applying the accumulated list is idempotent, so re-applying it is correct rather than merely
	 * tolerable.
	 *
	 * @param collector The provider whose {@link #addTaxa()} builds the taxa to seed
	 */
	protected static void seedLiveTaxa(TaxonProvider collector) {
		// A subclass seeds a subtree onto core's ancestors, so an empty accumulator here means core's
		// seedLiveTaxaForDatagen() has not run yet. Failing loudly beats 34 silent "parent taxon was
		// never registered" warnings followed by every content species failing its fail-soft build
		if (collector.getClass() != TaxonProvider.class && SEEDED.isEmpty()) {
			throw new IllegalStateException(
				"TaxonProvider.seedLiveTaxaForDatagen() must run before " + collector.getClass().getSimpleName()
				+ " seeds its taxa; the shared ancestors are not live yet");
		}

		collector.addTaxa();
		SEEDED.addAll(collector.pending.values());
		GeneticsReloadHandler.rebuildTaxa(SEEDED);
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		this.pending.clear();
		addTaxa();
		var futures = this.pending.entrySet().stream().map(entry -> {
			JsonElement json = TaxonDefinition.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).getOrThrow();
			return DataProvider.saveStable(output, json, this.path.json(entry.getKey()));
		}).toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures);
	}

	@Override
	public String getName() {
		return "Forestry Taxa";
	}
}
