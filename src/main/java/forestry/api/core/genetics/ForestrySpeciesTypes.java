package forestry.api.core.genetics;

import forestry.api.ForestryConstants;
import net.minecraft.resources.Identifier;

/**
 * The three types of species registered by base Forestry.
 */
public class ForestrySpeciesTypes {
	/**
	 * @see forestry.api.apiculture.genetics.IBeeSpeciesType
	 */
	public static final Identifier BEE = ForestryConstants.forestry("bee_species");
	/**
	 * @see forestry.api.arboriculture.genetics.ITreeSpeciesType
	 */
	public static final Identifier TREE = ForestryConstants.forestry("tree_species");
	/**
	 * @see forestry.api.lepidopterology.genetics.IButterflySpeciesType
	 */
	public static final Identifier BUTTERFLY = ForestryConstants.forestry("butterfly_species");
}
