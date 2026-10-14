package forestry.api.core.genetics;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Implemented by items that carry a genetic individual in a data component. Lets the API read an
 * individual out of a stack without naming the item implementation.
 *
 * Ex. the bee, tree, and butterfly items
 *
 * @see IIndividual
 */
public interface IIndividualItem {
	/**
	 * @return The individual stored in the stack, or {@code null} if it has none
	 */
	@Nullable
	IIndividual getIndividualFromComponent(ItemStack stack);

	/**
	 * @return The life stage this item represents. Ex. drone, princess, queen
	 */
	ILifeStage getLifeStage();

	/**
	 * @return The species type this item belongs to
	 */
	ISpeciesType<?, ?> getSpeciesType();
}
