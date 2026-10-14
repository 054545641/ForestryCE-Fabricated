package forestry.api.core;

import forestry.api.client.ITextureManager;
import net.minecraft.resources.Identifier;

/**
 * An error describes when a certain working condition is not met and how to resolve the error.
 */
public interface IError {
	/**
	 * @return The unique ID for this error.
	 */
	Identifier getId();

	/**
	 * @return Translation key for a short name that succinctly describes the error. Ex. "Too Hot"
	 */
	String getDescriptionTranslationKey();

	/**
	 * @return Translation key for a detailed message on how to fix the error. Ex. "Move the bees to a cooler climate."
	 */
	String getHelpTranslationKey();

	/**
	 * @return Location of an icon sprite registered to the Forestry texture manager at {@link ITextureManager}.
	 */
	Identifier getSprite();
}
