package forestry.mail.letters;

import forestry.api.mail.ILetter;
import forestry.mail.features.MailDataComponents;
import forestry.mail.MailFabricModule;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import forestry.mail.letters.Letter;
import forestry.mail.letters.LetterProperties;
import forestry.mail.letters.MailAddress;

public class LetterUtils {
	public static ItemStack createLetterStack(ILetter letter) {
		ItemStack letterStack = LetterProperties.createStampedLetterStack(letter);
		setLetterData(letterStack, letter);

		return letterStack;
	}

	@Nullable
	public static ILetter getLetter(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}

		if (!LetterUtils.isLetter(stack)) {
			return null;
		}

		return getLetterData(stack);
	}

	public static boolean isLetter(ItemStack stack) {
		return MailFabricModule.LETTERS.itemEqual(stack);
	}

	@Nullable
	public static Letter getLetterData(ItemStack stack) {
		Letter letter = stack.get(MailDataComponents.LETTER_DATA.get());
		return letter == null ? null : letter.copy();
	}

	public static void setLetterData(ItemStack stack, ILetter letter) {
		stack.set(MailDataComponents.LETTER_DATA.get(), letter instanceof Letter forestryLetter ? forestryLetter.copy() : fromLetter(letter));
	}

	private static Letter fromLetter(ILetter letter) {
		if (letter instanceof Letter forestryLetter) {
			return forestryLetter.copy();
		}

		Letter copy = new Letter(MailAddress.copyOf(letter.getSender()), letter.getRecipient() == null ? null : MailAddress.copyOf(letter.getRecipient()));
		copy.setProcessed(letter.isProcessed());
		copy.setText(letter.getText());
		copy.addAttachments(letter.getAttachments());
		for (ItemStack stamp : letter.getPostage()) {
			if (!stamp.isEmpty()) {
				copy.addStamps(stamp.copy());
			}
		}
		return copy;
	}
}
