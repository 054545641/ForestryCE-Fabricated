package forestry.core.platform.item;

import forestry.api.core.IItemSubtype;

import java.util.Locale;

public enum FluidContainerType implements IItemSubtype {
	CAN,
	WAX_CAPSULE,
	REFRACTORY_CAPSULE;

	private final String name;

	FluidContainerType() {
		this.name = name().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
