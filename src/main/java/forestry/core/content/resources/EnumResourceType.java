package forestry.core.content.resources;

import forestry.api.core.IBlockSubtype;

import java.util.Locale;

public enum EnumResourceType implements IBlockSubtype {
	APATITE,
	TIN,
	BRONZE,
	AMBER,
	SILICON;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}
