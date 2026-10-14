package forestry.apiculture.tab;

import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.block.NaturalistChestBlockType;
import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.platform.tab.ForestryCreativeTabs;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.platform.registration.FeatureCreativeTab;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.backpacks.features.BackpackItems;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.apiculture.hives.HiveBlockType;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.apiculture.features.ApicultureItems;
import forestry.apiculture.apiary.CreativeHiveFrameItem;
import forestry.core.platform.util.NBTUtilForestry;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The apiculture creative tab. Ordering keys are built from tab ids rather than tab objects so
 * this module does not depend on the others' holder classes.
 */
@FeatureProvider
public class ApicultureCreativeTab {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureCreativeTab APICULTURE = REGISTRY.creativeTab("apiculture", tab -> {
		tab.icon(() -> SpeciesUtil.BEE_TYPE.get().createStack(ForestryBeeSpecies.FOREST, BeeLifeStage.QUEEN));
		tab.displayItems(ApicultureCreativeTab::addApicultureItems);
		tab.withTabsBefore(ForestryCreativeTabs.tabKey("building_blocks"));
		tab.withTabsAfter(ForestryCreativeTabs.tabKey("arboriculture"));
	});

	static void addApicultureItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		// Genetics
		ForestryCreativeTabs.addGeneticBasics(items);
		items.accept(BackpackItems.APIARIST_BACKPACK);
		items.accept(CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.APIARIST_CHEST));

		// Gear
		items.accept(CoreItems.SCOOP);
		items.accept(CoreItems.PROVEN_SCOOP);
		items.accept(CoreItems.SPECTACLES);
		items.accept(ApicultureItems.APIARIST_HELMET);
		items.accept(ApicultureItems.APIARIST_CHEST);
		items.accept(ApicultureItems.APIARIST_LEGS);
		items.accept(ApicultureItems.APIARIST_BOOTS);

		// Hives
		ApicultureBlocks.BASE.getItems().forEach(items::accept);
		for (HiveBlockType type : HiveBlockType.values()) {
			if (type != HiveBlockType.SWARM) {
				items.accept(ApicultureBlocks.HIVE.get(type));
			}
		}

		items.accept(ApicultureBlocks.WAX_BLOCK);
		items.accept(ApicultureBlocks.REFRACTORY_WAX_BLOCK);

		// Alveary
		ApicultureBlocks.ALVEARY.getItems().forEach(items::accept);

		// Frames
		items.accept(ApicultureItems.FRAME_UNTREATED);
		items.accept(ApicultureItems.FRAME_IMPREGNATED);
		items.accept(ApicultureItems.FRAME_PROVEN);
		ItemStack creativeFrameMaxMutation = ApicultureItems.FRAME_CREATIVE.stack();
		CompoundTag forceMutationsTag = new CompoundTag();
		forceMutationsTag.put(CreativeHiveFrameItem.NBT_FORCE_MUTATIONS, ByteTag.valueOf((byte) 1));
		NBTUtilForestry.setItemStackTag(creativeFrameMaxMutation, forceMutationsTag);
		items.accept(ApicultureItems.FRAME_CREATIVE);
		items.accept(creativeFrameMaxMutation);

		// Food
		items.accept(ApicultureItems.HONEYED_SLICE);
		items.accept(ApicultureItems.AMBROSIA);

		// Misc items
		ApicultureItems.BEE_COMBS.getItems().forEach(items::accept);
		ApicultureBlocks.COMB_BLOCK.getItems().forEach(items::accept);
		ApicultureItems.PROPOLIS.getItems().forEach(items::accept);
		ApicultureItems.POLLEN_CLUSTER.getItems().forEach(items::accept);
		items.accept(ApicultureItems.ROYAL_JELLY);
		items.accept(ApicultureItems.AMBER_DRONE);

		SpeciesUtil.addTypeToCreativeTab(items, ForestrySpeciesTypes.BEE);
	}

	/**
	 * Contributes this module's entries to the base Forestry tab, which lives in another module.
	 * The event appends rather than splices, so these land at the end of that tab.
	 */
	public static void addToForestryTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == ForestryCreativeTabs.tabKey("forestry")) {
			event.accept(ApicultureItems.SMOKER);
			event.accept(ApicultureItems.AMBER_DRONE);
		}
	}
}
