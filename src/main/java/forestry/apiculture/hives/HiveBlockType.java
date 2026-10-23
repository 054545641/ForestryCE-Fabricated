package forestry.apiculture.hives;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.core.IBlockSubtype;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public enum HiveBlockType implements IBlockSubtype {
	FOREST(ForestryBeeSpecies.FOREST),
	MEADOWS(ForestryBeeSpecies.MEADOWS),
	MODEST(ForestryBeeSpecies.MODEST),
	TROPICAL(ForestryBeeSpecies.TROPICAL),
	ENDER(ForestryBeeSpecies.ENDED),
	WINTRY(ForestryBeeSpecies.WINTRY),
	MARSHY(ForestryBeeSpecies.MARSHY),
	SAVANNA(ForestryBeeSpecies.SAVANNA),
	LUSH(ForestryBeeSpecies.LUSH),
	AQUATIC(ForestryBeeSpecies.AQUATIC),
	NETHER(ForestryBeeSpecies.EMBITTERED),
	SWARM(ForestryConstants.forestry("none"));

	private final Identifier speciesUid;

	HiveBlockType(Identifier speciesUid) {
		this.speciesUid = speciesUid;
	}

	public Identifier getSpeciesId() {
		return this.speciesUid;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}
