package forestry.apiculture.features;

import forestry.apiculture.bees.EnumHoneyComb;
import forestry.apiculture.bees.EnumPollenCluster;
import forestry.apiculture.bees.EnumPropolis;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureItemGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.content.backpacks.features.CrateItems;
import forestry.core.content.backpacks.items.ItemCrated;

/**
 * Crates for apiculture products. Registered here rather than in {@link CrateItems} so the base
 * artifact does not name a bee item. The crates still register through base's registry, so their
 * ids and generated models are unchanged.
 */
@FeatureProvider
public class ApicultureCrates {
	public static final FeatureItem<ItemCrated> CRATED_POLLEN_CLUSTER_NORMAL = CrateItems.registerCrate("pollen_cluster", "crated_pollen_cluster_normal");
	public static final FeatureItem<ItemCrated> CRATED_POLLEN_CLUSTER_CRYSTALLINE = CrateItems.registerCrate("crystalline_pollen_cluster", "crated_pollen_cluster_crystalline");
	public static final FeatureItem<ItemCrated> CRATED_PROPOLIS = CrateItems.registerCrate("propolis", "crated_propolis");
	public static final FeatureItem<ItemCrated> CRATED_ROYAL_JELLY = CrateItems.registerCrate("royal_jelly", "crated_royal_jelly");

	public static final FeatureItemGroup<ItemCrated, EnumHoneyComb> CRATED_BEE_COMBS = CrateItems.registry()
		.itemGroup((comb, properties) -> new ItemCrated(() -> new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry(comb.getSerializedName() + "_comb"))), properties), EnumHoneyComb.VALUES)
		.identifier(comb -> "crated_" + comb.getSerializedName() + "_comb")
		.create();

	static {
		CrateItems.addCrates(CRATED_BEE_COMBS.getFeatures());
	}
}
