package forestry.core.data.recipe;

import net.minecraft.resources.Identifier;

import forestry.api.ForestryConstants;

/**
 * The id a machine recipe is written under. Machine recipes carry no result-derived name of their own,
 * so every jar that ships one names it by hand. Shared, so the jars cannot drift into one idiom each.
 */
public final class RecipeIds {
	private RecipeIds() {
	}

	/**
	 * Ex. {@code id("carpenter", "impregnated_casing")} -> {@code "forestry:carpenter/impregnated_casing"}
	 *
	 * @param path The path segments, joined by a slash
	 * @return The forestry id the recipe is written under
	 */
	public static Identifier id(String... path) {
		return ForestryConstants.forestry(String.join("/", path));
	}
}
