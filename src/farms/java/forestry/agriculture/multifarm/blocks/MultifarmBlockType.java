package forestry.agriculture.multifarm.blocks;

import forestry.api.core.IBlockSubtype;

import java.util.Locale;

public enum MultifarmBlockType implements IBlockSubtype {
	PLAIN,
	GEARBOX,
	HATCH,
	VALVE,
	CONTROL;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}
