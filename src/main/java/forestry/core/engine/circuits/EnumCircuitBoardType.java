package forestry.core.engine.circuits;

import forestry.api.core.IItemSubtype;

import java.util.Locale;

public enum EnumCircuitBoardType implements IItemSubtype {
	BASIC(1),
	ENHANCED(2),
	REFINED(3),
	INTRICATE(4);

	private final int sockets;
	private final String name;

	EnumCircuitBoardType(int sockets) {
		this.name = toString().toLowerCase(Locale.ENGLISH);
		this.sockets = sockets;
	}

	public int getSockets() {
		return this.sockets;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
