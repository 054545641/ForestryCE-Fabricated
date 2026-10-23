package forestry.api.mail;

import net.minecraft.network.chat.Component;

public enum EnumTradeStationState implements IPostalState {
	OK("for.chat.mail.ok"),
	INSUFFICIENT_OFFER("for.chat.mail.insufficient.offer"),
	INSUFFICIENT_TRADE_GOOD("for.chat.mail.insufficient.trade.good"),
	INSUFFICIENT_BUFFER("for.chat.mail.insufficient.buffer"),
	INSUFFICIENT_PAPER("for.chat.mail.insufficient.paper"),
	INSUFFICIENT_STAMPS("for.chat.mail.insufficient.stamps");

	private final String unlocalizedDescription;

	EnumTradeStationState(String unlocalizedDescription) {
		this.unlocalizedDescription = unlocalizedDescription;
	}

	@Override
	public boolean isOk() {
		return this == OK;
	}

	@Override
	public Component getDescription() {
		return Component.translatable(this.unlocalizedDescription);
	}
}
