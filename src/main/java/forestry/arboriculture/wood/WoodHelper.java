package forestry.arboriculture.wood;

import forestry.api.IForestryApi;
import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.core.platform.util.Translator;
import net.minecraft.network.chat.Component;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.IWoodTyped;
import forestry.arboriculture.wood.VanillaWoodType;

public class WoodHelper {
	public static Component getDisplayName(IWoodTyped wood, IWoodType woodType) {
		return getDisplayName(wood.getBlockKind(), wood.isFireproof(), woodType);
	}

	public static Component getDisplayName(WoodBlockKind kind, boolean fireproof, IWoodType woodType) {
		Component displayName;

		if (woodType instanceof ForestryWoodType) {
			String customUnlocalizedName = "block.forestry." + kind + "." + woodType;
			if (Translator.canTranslateToLocal(customUnlocalizedName)) {
				displayName = Component.translatable(customUnlocalizedName);
			} else {
				displayName = Component.translatable("for." + kind + ".grammar", Component.translatable("for.trees.woodType." + woodType));
			}
		} else if (woodType instanceof VanillaWoodType) {
			displayName = IForestryApi.INSTANCE.getTreeManager().getStack(woodType, kind, false).getHoverName();
		} else {
			throw new IllegalArgumentException("Unknown wood type: " + woodType);
		}

		if (fireproof) {
			displayName = Component.translatable("block.forestry.fireproof", displayName);
		}

		return displayName;
	}
}
