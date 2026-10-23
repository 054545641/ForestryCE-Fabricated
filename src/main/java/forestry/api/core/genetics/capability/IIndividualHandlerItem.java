package forestry.api.core.genetics.capability;

import forestry.api.IForestryApi;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IIndividualItem;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Stack helpers for genetic individuals stored in data components.
 * As of 1.21.1, items no longer use capabilities, but Data Components instead.
 */
public interface IIndividualHandlerItem {
	static void ifPresent(ItemStack stack, BiConsumer<IIndividual, ILifeStage> action) {
		IIndividual individual = getIndividual(stack);
		ILifeStage lifeStage = getLifeStage(stack);
		if (individual != null && lifeStage != null) {
			action.accept(individual, lifeStage);
		}
	}

	static void ifPresent(ItemStack stack, Consumer<IIndividual> action) {
		IIndividual individual = getIndividual(stack);
		if (individual != null) {
			action.accept(individual);
		}
	}

	/**
	 * @return Whether the given item has an individual capability. (Vanilla saplings have a capability too)
	 */
	static boolean isIndividual(ItemStack stack) {
		return getIndividual(stack) != null;
	}

	/**
	 * @return Whether the stack carries a genome component. Weaker than {@link #isIndividual}: a stack can hold
	 * the component without resolving to an individual, ex. villager trade wildcard bees
	 */
	static boolean hasIndividual(ItemStack stack) {
		return stack.has(IForestryApi.INSTANCE.getGeneticManager().genomeComponent());
	}

	/**
	 * Checks if the individual in this stack is present and if it matches some predicate.
	 *
	 * @param stack     The item to retrieve the individual from.
	 * @param predicate The predicate to test on the individual.
	 * @return {@code true} if the individual was present and the predicate returned true, false otherwise.
	 */
	static boolean filter(ItemStack stack, Predicate<IIndividual> predicate) {
		IIndividual individual = getIndividual(stack);
		return individual != null && predicate.test(individual);
	}

	static boolean filter(ItemStack stack, BiPredicate<IIndividual, ILifeStage> predicate) {
		IIndividual individual = getIndividual(stack);
		ILifeStage lifeStage = getLifeStage(stack);
		return individual != null && lifeStage != null && predicate.test(individual, lifeStage);
	}

	@Nullable
	static IIndividual getIndividual(ItemStack stack) {
		return stack.getItem() instanceof IIndividualItem item ? item.getIndividualFromComponent(stack) : null;
	}

	@Nullable
	static IGenome getGenome(ItemStack stack) {
		return stack.get(IForestryApi.INSTANCE.getGeneticManager().genomeComponent());
	}

	@Nullable
	static ILifeStage getLifeStage(ItemStack stack) {
		return stack.getItem() instanceof IIndividualItem item ? item.getLifeStage() : null;
	}

	@Nullable
	static ISpeciesType<?, ?> getSpeciesType(ItemStack stack) {
		return stack.getItem() instanceof IIndividualItem item ? item.getSpeciesType() : null;
	}

	/**
	 * Gets the species of the current item stack, or returns the default species for the species type.
	 */
	@SuppressWarnings("unchecked")
	static <S extends ISpecies<?>> S getSpecies(ItemStack stack, ISpeciesType<S, ?> type) {
		IIndividual individual = getIndividual(stack);
		return individual != null ? (S) individual.getSpecies() : type.getDefaultSpecies();
	}
}
