package forestry.api.mail;

import net.minecraft.network.chat.Component;

public interface IPostalState {
	/**
	 * Normal states are OK, error states are not OK
	 */
	boolean isOk();

	/**
	 * Localized description of the postal state
	 */
	Component getDescription();
}
