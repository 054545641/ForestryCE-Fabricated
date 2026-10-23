package forestry.core.platform.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import forestry.api.plugin.IGeneticRegistration;
import forestry.api.plugin.ISpeciesTypeBuilder;
import forestry.api.plugin.ITaxonBuilder;
import net.minecraft.resources.Identifier;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import forestry.core.platform.compat.kubejs.apiculture.KubeFlowerType;

public class GeneticsEventJS implements KubeEvent {
	private final IGeneticRegistration wrapped;

	public GeneticsEventJS(IGeneticRegistration wrapped) {
		this.wrapped = wrapped;
	}

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
	// Flower types moved from the apiculture event in phase 9b: they are shared by bees and
	// butterflies, so an addon must be able to register one without apiculture installed
	public void registerFlowerType(Identifier id, BiPredicate<Level, BlockPos> isAcceptableFlower, KubeFlowerType.PlantRandomFlowerFunction plantRandomFlower, boolean dominant) {
		this.wrapped.registerFlowerType(id, new KubeFlowerType(isAcceptableFlower, plantRandomFlower, dominant));
	}

	public void defineTaxon(String parent, String name) {
		this.wrapped.defineTaxon(parent, name);
	}

	/**
	 * Defines a new taxon or retrieves an existing taxon, allows for adding subtaxa and species.
	 *
	 * @param parent The name of the parent taxon. Cannot be a {@link forestry.api.core.genetics.TaxonomicRank#GENUS}.
	 * @param name   The name of the taxon. Must be unique for all taxa in the same rank.
	 * @param action A consumer that adds additional information to the taxon after it is defined.
	 * @throws UnsupportedOperationException If the parent taxon is a GENUS.
	 * @see forestry.api.core.genetics.ForestryTaxa For builtin taxon names.
	 */
	public void defineTaxon(String parent, String name, Consumer<ITaxonBuilder> action) {
		this.wrapped.defineTaxon(parent, name, action);
	}

	/**
	 * Modify an existing species, for example, adding an extra chromosome to bees, or adding additional permitted alleles to chromosomes.
	 * Called after all species types are registered, but before the registry is finalized.
	 *
	 * @param id     The ID of the species to modify.
	 * @param action The modifications to be made.
	 */
	public void modifySpeciesType(Identifier id, Consumer<ISpeciesTypeBuilder> action) {
		this.wrapped.modifySpeciesType(id, action);
	}
}
