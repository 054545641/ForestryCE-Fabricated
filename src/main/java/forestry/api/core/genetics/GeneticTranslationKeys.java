package forestry.api.core.genetics;

import net.minecraft.resources.Identifier;

/**
 * Builds the translation keys used for genetic objects: species of a species type, and alleles of a
 * chromosome.
 */
public final class GeneticTranslationKeys {
	private GeneticTranslationKeys() {
	}

	/**
	 * Creates a translation key for an OBJECT of a TYPE (ex. a SPECIES of a SPECIES TYPE, or an ALLELE of a CHROMOSOME)
	 * The format is as follows:
	 * <ul>
	 *     <li>If {@code typeId} and {@code objectId} share the same namespace, the format is: <br> {@code TYPE.NAMESPACE.TYPEPATH.OBJECTPATH}</li>
	 *     <li>If {@code typeId} and {@code objectId} have different namespaces, the format is: <br> {@code TYPE.TYPENAMESPACE.TYPEPATH.OBJECTNAMESPACE.OBJECTPATH}</li>
	 * </ul>
	 * For example, the Austere bee species from Forestry has the translation key: <br> {@code species.forestry.bee.austere} <br>
	 * and a bee species contributed by an add-on under its own namespace has the translation key: <br> {@code species.forestry.bee.myaddon.mybee}
	 *
	 * @param type     The first part of the translation key that describes the type of object this is. Can be empty.
	 * @param typeId   The ID of the type. An example would be the ID of the species type.
	 * @param objectId The ID of the object. An example would be the ID of the species.
	 * @return The translation key.
	 */
	public static String createTranslationKey(String type, Identifier typeId, Identifier objectId) {
		String typeNamespace = typeId.getNamespace();
		StringBuilder translationKey = new StringBuilder(type);
		if (!type.isEmpty()) {
			translationKey.append('.');
		}
		translationKey.append(typeNamespace);
		translationKey.append('.');
		translationKey.append(typeId.getPath());
		translationKey.append('.');

		String speciesNamespace = objectId.getNamespace();
		if (speciesNamespace.equals(typeNamespace)) {
			// for species from the same mod as species type, use the following format:
			// species.forestry.bee.austere
			translationKey.append(objectId.getPath());
		} else {
			// if species type is from another mod, use this format instead:
			// species.forestry.bee.myaddon.mybee
			translationKey.append(speciesNamespace);
			translationKey.append('.');
			translationKey.append(objectId.getPath());
		}

		return translationKey.toString();
	}
}
