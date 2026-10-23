
package forestry.api.core.backpacks;

import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * The Backpack Interface allows you to add items to Forestry backpacks or create your own backpacks.
 * <p>
 * To create your own backpack, create an {@link IBackpackDefinition}.
 * Backpack definitions have a filter, which you can create here with
 * {@link #createNaturalistBackpackFilter(Identifier)}
 * or create your own.
 * <p>
 * After you've created your backpack definition, create the item with
 * {@link #createBackpack(IBackpackDefinition, EnumBackpackType, Item.Properties)}
 * or {@link #createNaturalistBackpack(IBackpackDefinition, Identifier, CreativeModeTab, Item.Properties)}
 * and then register the returned item like any other item.
 */
public interface IBackpackInterface {

	/**
	 * Creates a backpack with the given UID and type, returning the item.
	 *
	 * @param definition The backpack definition.
	 * @param type       Type of backpack.
	 * @param properties The item properties with its registry ID set
	 * @return The created backpack item
	 */
	Item createBackpack(IBackpackDefinition definition, EnumBackpackType type, Item.Properties properties);

	/**
	 * Create a backpack that can hold items from a specific {@link ISpeciesType}.
	 *
	 * @param definition    The backpack definition.
	 * @param speciesTypeId The species root.
	 * @param tab
	 * @param properties The item properties with its registry ID set
	 * @return The created backpack item
	 */
	Item createNaturalistBackpack(IBackpackDefinition definition, Identifier speciesTypeId, CreativeModeTab tab, Item.Properties properties);

	/**
	 * Makes a new naturalist backpack filter. Only accepts items from a specific {@link ISpeciesType}.
	 * Useful for implementing {@link IBackpackDefinition} for naturalist's backpacks.
	 *
	 * @param speciesTypeId The species root's unique ID.
	 * @return a new backpack filter for the specified species root
	 * @see #createNaturalistBackpack(IBackpackDefinition, Identifier, CreativeModeTab, Item.Properties)
	 */
	Predicate<ItemStack> createNaturalistBackpackFilter(Identifier speciesTypeId);
}
