package forestry.lepidopterology.data;

import net.minecraft.data.PackOutput;

import forestry.core.data.models.ItemAssetsProvider;

import forestry.api.ForestryConstants;
import forestry.lepidopterology.features.LepidopterologyItems;

/**
 * Generates the item models for the butterflies jar.
 */
public class LepidopterologyItemModelProvider extends ItemAssetsProvider {
	public LepidopterologyItemModelProvider(PackOutput output) {
		super(output);
	}

	@Override
	protected void registerModels() {
		withExistingParent(LepidopterologyItems.CATERPILLAR_GE.getName(), mcLoc("item/generated"))
			.texture("layer0", ForestryConstants.forestry("item/caterpillar.body2"))
			.texture("layer1", ForestryConstants.forestry("item/caterpillar.body"));
		withExistingParent(LepidopterologyItems.SERUM_GE.getName(), mcLoc("item/generated"))
			.texture("layer0", ForestryConstants.forestry("item/liquids/jar.bottle"))
			.texture("layer1", ForestryConstants.forestry("item/liquids/jar.contents"));
	}
}
