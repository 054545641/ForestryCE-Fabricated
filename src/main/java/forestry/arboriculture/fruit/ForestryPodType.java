package forestry.arboriculture.fruit;

import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.core.IBlockSubtype;
import forestry.core.platform.util.SpeciesUtil;

import net.minecraft.resources.Identifier;

import java.util.Locale;
import forestry.api.arboriculture.ForestryFruits;

public enum ForestryPodType implements IBlockSubtype {
	COCOA(ForestryFruits.COCOA),
	//TODO: change all of these to be 'bunches'. Could also be used for Bananas?
	DATES(ForestryFruits.DATES),
	PAPAYA(ForestryFruits.PAPAYA),
	COCONUT(ForestryFruits.COCONUT);

	private final Identifier fruitId;

	ForestryPodType(Identifier fruitId) {
		this.fruitId = fruitId;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public IFruit getFruit() {
		return SpeciesUtil.TREE_TYPE.get().getFruit(this.fruitId);
	}
}
