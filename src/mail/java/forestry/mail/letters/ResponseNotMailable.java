package forestry.mail.letters;

import forestry.api.mail.IPostalState;
import net.minecraft.network.chat.Component;

public class ResponseNotMailable implements IPostalState {
	private final IPostalState state;

	public ResponseNotMailable(IPostalState state) {
		this.state = state;
	}

	@Override
	public boolean isOk() {
		return false;
	}

	@Override
	public Component getDescription() {
		return Component.translatable("for.chat.mail.response.not.mailable.format", this.state.getDescription());
	}
}
