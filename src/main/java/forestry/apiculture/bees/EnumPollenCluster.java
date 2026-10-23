package forestry.apiculture.bees;

import forestry.core.platform.item.TwoTintItem;
import net.minecraft.network.chat.TextColor;

import java.util.Locale;

public enum EnumPollenCluster implements TwoTintItem.ITwoTintItemSubtype {
	NORMAL(TextColor.fromRgb(0xa28a25), TextColor.fromRgb(0xa28a25)),
	CRYSTALLINE(TextColor.fromRgb(0xffffff), TextColor.fromRgb(0xc5feff));

	private final String name;
	private final int primaryColor;
	private final int secondaryColor;

	EnumPollenCluster(TextColor primary, TextColor secondary) {
		this.name = toString().toLowerCase(Locale.ENGLISH);
		this.primaryColor = primary.getValue();
		this.secondaryColor = secondary.getValue();
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	@Override
	public int primaryColor() {
		return this.primaryColor;
	}

	@Override
	public int secondaryColor() {
		return this.secondaryColor;
	}
}
