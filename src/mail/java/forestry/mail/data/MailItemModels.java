package forestry.mail.data;

import forestry.core.data.models.ModelAssets;

import forestry.core.data.ForestryItemModels;
import forestry.core.platform.registration.FeatureItem;
import forestry.mail.features.MailItems;
import forestry.mail.letters.LetterItem;

/**
 * Generates the item models for the mail jar.
 */
public class MailItemModels {
	public static void addModels(ModelAssets models) {
		models.generic2d(MailItems.CATALOGUE);

		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.BIG, LetterItem.State.EMPTIED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.BIG, LetterItem.State.FRESH));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.BIG, LetterItem.State.OPENED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.BIG, LetterItem.State.STAMPED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.EMPTIED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.FRESH));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.OPENED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.STAMPED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.SMALL, LetterItem.State.EMPTIED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.SMALL, LetterItem.State.FRESH));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.SMALL, LetterItem.State.OPENED));
		models.generic2d(MailItems.LETTERS.get(LetterItem.Size.SMALL, LetterItem.State.STAMPED));

		for (FeatureItem<?> stamp : MailItems.STAMPS.getFeatures()) {
			ForestryItemModels.layered(models, stamp, "item/stamps.0", "item/stamps.1");
		}
	}
}
