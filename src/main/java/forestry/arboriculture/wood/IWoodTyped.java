package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;

public interface IWoodTyped {
	WoodBlockKind getBlockKind();

	boolean isFireproof();

	IWoodType getWoodType();
}
