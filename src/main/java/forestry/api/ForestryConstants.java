package forestry.api;

import net.minecraft.resources.Identifier;

/**
 * Common constants used throughout Forestry that might be useful for other mods.
 */
public class ForestryConstants {
	/**
	 * Forestry's mod ID.
	 */
	public static final String MOD_ID = "forestry";
	/**
	 * @return A new resource location under the Forestry namespace. In most cases, mods should use their own namespace instead.
	 */
	public static Identifier forestry(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
