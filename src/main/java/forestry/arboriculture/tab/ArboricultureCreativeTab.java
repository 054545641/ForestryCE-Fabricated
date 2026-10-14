package forestry.arboriculture.tab;

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
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.WoodAccess;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.arboriculture.features.CharcoalBlocks;

/**
 * The arboriculture creative tab. Ordering keys are built from tab ids rather than tab objects so
 * this module does not depend on the others' holder classes.
 */
@FeatureProvider
public class ArboricultureCreativeTab {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	public static final FeatureCreativeTab ARBORICULTURE = REGISTRY.creativeTab("arboriculture", tab -> {
		tab.icon(() -> SpeciesUtil.TREE_TYPE.get().createStack(ForestryTreeSpecies.OAK, TreeLifeStage.SAPLING));
		tab.withTabsBefore(ForestryCreativeTabs.tabKey("apiculture"));
		tab.withTabsAfter(ForestryCreativeTabs.tabKey("lepidopterology"));
		tab.displayItems(ArboricultureCreativeTab::addArboricultureItems);
	});

	static void addArboricultureItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		// Genetics
		ForestryCreativeTabs.addGeneticBasics(items);
		items.accept(BackpackItems.ARBORIST_BACKPACK);
		items.accept(CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.ARBORIST_CHEST));

		// Gear
		items.accept(CoreItems.SPECTACLES);
		items.accept(ArboricultureItems.GRAFTER);
		items.accept(ArboricultureItems.PROVEN_GRAFTER);

		// Fruits
		CoreItems.FRUITS.getItems().forEach(items::accept);

		// Blocks
		items.accept(CharcoalBlocks.LOG_PILE);
		items.accept(CharcoalBlocks.DECORATIVE_LOG_PILE);
		items.accept(CharcoalBlocks.ASH);
		items.accept(CoreItems.ASH);
		WoodAccess access = WoodAccess.INSTANCE;
		for (IWoodType type : access.getRegisteredWoodTypes()) {
			addAllWoodBlocks(items, access, type, false);
		}
		for (IWoodType type : access.getRegisteredWoodTypes()) {
			addAllWoodBlocks(items, access, type, true);
		}

		for (ForestryWoodType type : ForestryWoodType.VALUES) {
			items.accept(ArboricultureItems.BOAT.item(type));
			items.accept(ArboricultureItems.CHEST_BOAT.item(type));
			items.accept(ArboricultureBlocks.SIGN.get(type));
			items.accept(ArboricultureBlocks.HANGING_SIGN.get(type));
		}

		// Specimens
		SpeciesUtil.addTypeToCreativeTab(items, ForestrySpeciesTypes.TREE);
		items.accept(ArboricultureItems.AMBER_SAPLING_FOSSIL);
		ArboricultureBlocks.LEAVES_DECORATIVE.getItems().forEach(items::accept);
		ArboricultureBlocks.LEAVES_DEFAULT.getItems().forEach(items::accept);
		ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.getItems().forEach(items::accept);
	}

	/**
	 * Adds every block shape of one wood type to a tab. Public so the building blocks tab can list
	 * the same wood, which it does alongside this tab rather than instead of it, as on 1.20.1.
	 *
	 * @param items     The tab's output
	 * @param access    The wood access to read the blocks from
	 * @param type      The wood type to list
	 * @param fireproof Whether to list the fireproof variants
	 */
	public static void addAllWoodBlocks(CreativeModeTab.Output items, WoodAccess access, IWoodType type, boolean fireproof) {
		items.accept(access.getStack(type, WoodBlockKind.LOG, fireproof));
		items.accept(access.getStack(type, WoodBlockKind.WOOD, fireproof));
		items.accept(access.getStack(type, WoodBlockKind.STRIPPED_LOG, fireproof));
		items.accept(access.getStack(type, WoodBlockKind.STRIPPED_WOOD, fireproof));
		items.accept(access.getStack(type, WoodBlockKind.PLANKS, fireproof));
		items.accept(access.getStack(type, WoodBlockKind.STAIRS, fireproof));
		items.accept(access.getStack(type, WoodBlockKind.SLAB, fireproof));
		items.accept(access.getStack(type, WoodBlockKind.FENCE, fireproof));
		items.accept(access.getStack(type, WoodBlockKind.FENCE_GATE, fireproof));
		// These shapes are intrinsically non-burning; WoodAccess returns the same item for both flags.
		if (!fireproof) {
			items.accept(access.getStack(type, WoodBlockKind.DOOR, false));
			items.accept(access.getStack(type, WoodBlockKind.TRAPDOOR, false));
			items.accept(access.getStack(type, WoodBlockKind.PRESSURE_PLATE, false));
			items.accept(access.getStack(type, WoodBlockKind.BUTTON, false));
		}
	}

	/**
	 * Contributes this module's entries to the base Forestry tab, which lives in another module.
	 * The event appends rather than splices, so these land at the end of that tab.
	 */
	public static void addToForestryTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == ForestryCreativeTabs.tabKey("forestry")) {
			event.accept(ArboricultureItems.GRAFTER);
			event.accept(ArboricultureItems.PROVEN_GRAFTER);
			event.accept(ArboricultureItems.AMBER_SAPLING_FOSSIL);
			event.accept(CharcoalBlocks.CHARCOAL);
			// Deviation from 1.20.1: that tree listed the ash block in its building blocks tab,
			// which this tree does not have, so it appends to the base Forestry tab instead
			event.accept(CharcoalBlocks.ASH);
		}
	}
}
