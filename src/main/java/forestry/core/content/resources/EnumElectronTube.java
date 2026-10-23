package forestry.core.content.resources;

import forestry.api.core.IItemSubtype;

import java.util.Locale;

public enum EnumElectronTube implements IItemSubtype {
	COPPER,
	TIN,
	BRONZE,
	IRON,
	GOLD,
	DIAMOND,
	OBSIDIAN,
	BLAZE,
	EMERALD,
	APATITE,
	LAPIS,
	ENDER,
	AMBER,
	SILICON;

	private final String uid;

	EnumElectronTube() {
		this.uid = name().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public String getSerializedName() {
		return this.uid;
	}
    public String registryPath() {
        return switch (this) {
            case GOLD -> "golden_electron_tube";
            case DIAMOND -> "diamantine_electron_tube";
            case APATITE -> "apatine_electron_tube";
            case BLAZE -> "blazing_electron_tube";
            default -> this.uid + "_electron_tube";
        };
    }

}
