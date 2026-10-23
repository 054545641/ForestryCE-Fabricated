package forestry.core.data.models;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.features.ApicultureCrates;
import forestry.apiculture.features.ApicultureItems;
import forestry.core.data.builder.FilledCrateModelBuilder;
import forestry.core.platform.util.ModUtil;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.backpacks.features.CrateItems;
import forestry.core.content.backpacks.items.BackpackItem;
import forestry.core.content.backpacks.items.ItemCrated;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;


public class ForestryItemModelProvider extends ItemAssetsProvider {
	public ForestryItemModelProvider(PackOutput output) {
		super(output);
	}

	@Override
	protected void registerModels() {
		for (FeatureItem<ItemCrated> featureCrated : CrateItems.getCrates()) {
			Item containedItem = featureCrated.get().getContained().getItem();
			String id = featureCrated.getName();

			if (ApicultureItems.BEE_COMBS.itemEqual(containedItem)) {
				filledCrateModelLayered(id, modLoc("item/bee_combs.0"), modLoc("item/bee_combs.1"));
			} else if (ApicultureItems.POLLEN_CLUSTER.itemEqual(containedItem)) {
				filledCrateModelLayered(id, modLoc("item/pollen.0"), modLoc("item/pollen.1"));
			} else {
				Identifier contained = ModUtil.getRegistryName(containedItem);
				Identifier contentsTexture;

				if (containedItem instanceof BlockItem && containedItem.getDescriptionId().startsWith("block.")) {
					contentsTexture = Identifier.fromNamespaceAndPath(contained.getNamespace(), "block/" + contained.getPath());
				} else {
					contentsTexture = Identifier.fromNamespaceAndPath(contained.getNamespace(), "item/" + contained.getPath());
				}

				filledCrateModel(id, contentsTexture);

			}
		}

		// 2d items
		basicItem(ApicultureItems.FRAME_CREATIVE.get());

		// manual overrides
		filledCrateModel(CrateItems.CRATED_CACTUS.getName(), mcLoc("block/cactus_side"));
		filledCrateModel(CrateItems.CRATED_MYCELIUM.getName(), mcLoc("block/mycelium_side"));
		filledCrateModel(CrateItems.CRATED_GRASS_BLOCK.getName(), mcLoc("block/grass_block_top"));
		filledCrateModel(ApicultureCrates.CRATED_PROPOLIS.getName(), modLoc("item/propolis.0"));

		// Buckets are client item definitions using neoforge:fluid_container.

		// Backpacks
		for (DeferredHolder<Item, ? extends Item> object : ModFeatureRegistry.get(ForestryModuleIds.STORAGE).getRegistry(Registries.ITEM).getEntries()) {
			if (object.get() instanceof BackpackItem) {
				String path = object.getId().getPath();
				// Deviation from 1.20.1: the ids here are woven_<type>_backpack, not <type>_bag_woven, so
				// the old endsWith test never matched and every woven bag took the plain model
				boolean woven = path.startsWith("woven");

				withExistingParent(path, woven ? modLoc("item/backpack/woven_neutral") : modLoc("item/backpack/normal_neutral"));
			}
		}
	}

	private static String path(Item block) {
		return ModUtil.getRegistryName(block).getPath();
	}

	private void filledCrateModel(String id, Identifier texture) {
		new FilledCrateModelBuilder().layer1(texture).toJson(getBuilder(id).json);
	}

	private void filledCrateModelLayered(String id, Identifier layer1, Identifier layer2) {
		new FilledCrateModelBuilder().layer1(layer1).layer2(layer2).toJson(getBuilder(id).json);
	}
}
