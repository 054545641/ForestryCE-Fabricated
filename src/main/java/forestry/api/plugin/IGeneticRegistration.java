package forestry.api.plugin;

import forestry.api.apiculture.IFlowerType;
import forestry.api.core.genetics.filter.IFilterRuleType;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/**
 * Handles registration of genetic-related data. Accessed from {@link IForestryPlugin#registerGenetics(IGeneticRegistration)}.
 */
public interface IGeneticRegistration {
	/**
	 * Defines a new taxon under an existing parent. Taxa are displayed as classifications in the Forestry analyzer.
	 * When defining a new tree of taxa, it is better to use {@link #defineTaxon(String, String, Consumer)}
	 * instead of making repeated calls to this method.
	 *
	 * @param parent The name of the parent taxon. Cannot be a {@link forestry.api.core.genetics.TaxonomicRank#GENUS}.
	 * @param name   The name of the taxon. Must be unique.
	 * @throws UnsupportedOperationException If the parent taxon is a GENUS.
	 * @see forestry.api.core.genetics.ForestryTaxa For built-in taxon names.
	 */
	void defineTaxon(String parent, String name);

	/**
	 * Register a flower type, a category of blocks that a pollinating species can work with. Bees and
	 * butterflies both carry a flower type chromosome, so this is registered through genetics rather
	 * than through apiculture.
	 *
	 * @param id   The unique ID for this flower type.
	 * @param type The flower type to register.
	 * @since 2.10.0
	 */
	void registerFlowerType(Identifier id, IFlowerType type);

	/**
	 * Defines a new taxon or retrieves an existing taxon, allows for adding subtaxa and species.
	 *
	 * @param parent The name of the parent taxon. Cannot be a {@link forestry.api.core.genetics.TaxonomicRank#GENUS}.
	 * @param name   The name of the taxon. Must be unique for all taxa in the same rank.
	 * @param action A consumer that adds additional information to the taxon after it is defined.
	 * @throws UnsupportedOperationException If the parent taxon is a GENUS.
	 * @see forestry.api.core.genetics.ForestryTaxa For builtin taxon names.
	 */
	void defineTaxon(String parent, String name, Consumer<ITaxonBuilder> action);

	/**
	 * Register a new type of species. Can only be called from {@link IForestryPlugin#registerGenetics}.
	 *
	 * @param id          The ID of the species.
	 * @param typeFactory The function to use to create the species type, given the completed karyotype.
	 * @return A builder that can be used to define properties of the species.
	 */
	ISpeciesTypeBuilder registerSpeciesType(Identifier id, ISpeciesTypeFactory typeFactory);

	/**
	 * Modify an existing species, for example, adding an extra chromosome to bees, or adding additional permitted alleles to chromosomes.
	 * Called after all species types are registered, but before the registry is finalized.
	 *
	 * @param id     The ID of the species to modify.
	 * @param action The modifications to be made.
	 */
	void modifySpeciesType(Identifier id, Consumer<ISpeciesTypeBuilder> action);

	/**
	 * Registers a new filter rule for the Genetic Filter block.
	 *
	 * @param ruleType The filter rule type to register.
	 */
	void registerFilterRuleType(IFilterRuleType ruleType);

	default void registerFilterRuleTypes(IFilterRuleType[] ruleTypes) {
		for (IFilterRuleType ruleType : ruleTypes) {
			registerFilterRuleType(ruleType);
		}
	}
}
