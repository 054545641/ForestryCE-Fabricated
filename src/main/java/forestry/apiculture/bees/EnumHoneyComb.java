package forestry.apiculture.bees;

import forestry.api.core.IBlockSubtype;
import forestry.api.core.IItemSubtype;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum EnumHoneyComb implements StringRepresentable, IItemSubtype, IBlockSubtype {
	HONEY(TextColor.fromRgb(0xe8d56a), TextColor.fromRgb(0xffa12b)),
	COCOA(TextColor.fromRgb(0x674016), TextColor.fromRgb(0xffb62b)),
	SIMMERING(TextColor.fromRgb(0x981919), TextColor.fromRgb(0xFE8738)),
	STRINGY(TextColor.fromRgb(0xc8be67), TextColor.fromRgb(0xbda93e)),
	FROZEN(TextColor.fromRgb(0xf9ffff), TextColor.fromRgb(0xa0ffff)),
	DRIPPING(TextColor.fromRgb(0xdc7613), TextColor.fromRgb(0xffff00)),
	SILKY(TextColor.fromRgb(0x508907), TextColor.fromRgb(0xddff00)),
	PARCHED(TextColor.fromRgb(0xdcbe13), TextColor.fromRgb(0xffff00)),
	MYSTERIOUS(TextColor.fromRgb(0x161616), TextColor.fromRgb(0xe099ff)),
	POWDERY(TextColor.fromRgb(0x676767), TextColor.fromRgb(0xffffff)),
	WHEATEN(TextColor.fromRgb(0xfeff8f), TextColor.fromRgb(0xffffff)),
	MOSSY(TextColor.fromRgb(0x2a3313), TextColor.fromRgb(0x7e9939)),
	MELLOW(TextColor.fromRgb(0x886000), TextColor.fromRgb(0xfff960)),
	KAOLIN(TextColor.fromRgb(0x5e6c8d), TextColor.fromRgb(0xafb9d6)),
	VINTAGE(TextColor.fromRgb(0xDEB887), TextColor.fromRgb(0xCD853F)),
	SPONGY(TextColor.fromRgb(0x9D8F39), TextColor.fromRgb(0xe1e351)),
	SCULKEN(TextColor.fromRgb(0x111B21), TextColor.fromRgb(0x05625d)),
	//LUMINOUS(TextColor.fromRgb(0x495E27), TextColor.fromRgb(0xF7CE46));
	;
	//""(TextColor.fromRgb(0xd7bee5), TextColor.fromRgb(0xfd58ab)); // kindof pinkish

	public static final EnumHoneyComb[] VALUES = values();

	public final String name;
	public final int primaryColor;
	public final int secondaryColor;

	EnumHoneyComb(TextColor primary, TextColor secondary) {
		this.name = toString().toLowerCase(Locale.ENGLISH);
		this.primaryColor = primary.getValue();
		this.secondaryColor = secondary.getValue();
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
