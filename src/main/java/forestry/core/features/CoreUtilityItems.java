package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.resources.ItemBeesWax;
import forestry.core.content.tools.ItemScoop;
import forestry.core.content.resources.ItemRefractoryWax;
import forestry.core.engine.circuits.EnumCircuitBoardType;
import forestry.core.engine.circuits.ItemCircuitBoard;
import forestry.core.engine.genetics.ItemResearchNote;
import forestry.core.platform.item.*;
import forestry.core.content.tools.*;
import forestry.core.content.analyzer.*;
import forestry.core.content.resources.*;
import forestry.core.content.resources.EnumCraftingMaterial;
import forestry.core.content.resources.EnumElectronTube;
import forestry.core.content.tools.ToolTier;
import forestry.core.platform.registration.*;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;

public final class CoreUtilityItems {
 private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
	public static final FeatureItem<PortableAnalyzerItem> PORTABLE_ANALYZER = REGISTRY.item(PortableAnalyzerItem::new, () -> new Item.Properties()
		.stacksTo(1)
		.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
		.component(CoreDataComponents.ALYZER_CHARGES.get(), 0), "portable_analyzer");
	public static final FeatureItem<ItemWrench> WRENCH = REGISTRY.item(ItemWrench::new, "wrench");
	public static final FeatureItem<ItemPipette> PIPETTE = REGISTRY.item(ItemPipette::new, "pipette");
	public static final FeatureItem<ItemForestry> BROKEN_SURVIVALISTS_PICKAXE = REGISTRY.item(ItemForestry::new, "broken_survivalists_pickaxe");
	public static final FeatureItem<ItemForestry> BROKEN_SURVIVALISTS_SHOVEL = REGISTRY.item(ItemForestry::new, "broken_survivalists_shovel");
	public static final FeatureItem<ItemForestry> BROKEN_SURVIVALISTS_AXE = REGISTRY.item(ItemForestry::new, "broken_survivalists_axe");
	public static final FeatureItem<ItemForestry> BROKEN_SURVIVALISTS_SWORD = REGISTRY.item(ItemForestry::new, "broken_survivalists_sword");
	public static final FeatureItem<ItemForestry> BROKEN_SURVIVALISTS_HOE = REGISTRY.item(ItemForestry::new, "broken_survivalists_hoe");
	public static final FeatureItem<HasRemnants.Pickaxe> SURVIVALISTS_PICKAXE = REGISTRY.item(properties -> new HasRemnants.Pickaxe(ToolTier.SURVIVALIST, 1, -2.8f, properties, BROKEN_SURVIVALISTS_PICKAXE::stack), "survivalists_pickaxe");
	public static final FeatureItem<HasRemnants.Shovel> SURVIVALISTS_SHOVEL = REGISTRY.item(properties -> new HasRemnants.Shovel(ToolTier.SURVIVALIST, 1.5f, -3.0f, properties, BROKEN_SURVIVALISTS_SHOVEL::stack), "survivalists_shovel");
	public static final FeatureItem<HasRemnants.Axe> SURVIVALISTS_AXE = REGISTRY.item(properties -> new HasRemnants.Axe(ToolTier.SURVIVALIST, 5.5f, -3.2f, properties, BROKEN_SURVIVALISTS_AXE::stack), "survivalists_axe");
	public static final FeatureItem<HasRemnants.Sword> SURVIVALISTS_SWORD = REGISTRY.item(properties -> new HasRemnants.Sword(ToolTier.SURVIVALIST, 3, -2.4f, properties, BROKEN_SURVIVALISTS_SWORD::stack), "survivalists_sword");
	public static final FeatureItem<HasRemnants.Hoe> SURVIVALISTS_HOE = REGISTRY.item(properties -> new HasRemnants.Hoe(ToolTier.SURVIVALIST, -2, -1.0f, properties, BROKEN_SURVIVALISTS_HOE::stack), "survivalists_hoe");
	public static final FeatureItem<SolderingIronItem> SOLDERING_IRON = REGISTRY.item(SolderingIronItem::new, () -> new Item.Properties()
		.durability(5)
		.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY), "soldering_iron");
	public static final FeatureItem<ItemScoop> SCOOP = REGISTRY.item(properties -> new ItemScoop(10, properties), "scoop");
	public static final FeatureItem<ItemScoop> PROVEN_SCOOP = REGISTRY.item(properties -> new ItemScoop(160, properties), "proven_scoop");
	public static final FeatureItem<ItemBeesWax> BEESWAX = REGISTRY.item(ItemBeesWax::new, "beeswax");
	public static final FeatureItem<ItemRefractoryWax> REFRACTORY_WAX = REGISTRY.item(ItemRefractoryWax::new, "refractory_wax");
 public static final FeatureItem<ItemAssemblyKit> SHOVEL_KIT = REGISTRY.item(properties -> new ItemAssemblyKit(SURVIVALISTS_SHOVEL::stack, properties), "shovel_kit");
 public static final FeatureItem<ItemAssemblyKit> PICKAXE_KIT = REGISTRY.item(properties -> new ItemAssemblyKit(SURVIVALISTS_PICKAXE::stack, properties), "pickaxe_kit");
 public static final FeatureItem<ItemAssemblyKit> AXE_KIT = REGISTRY.item(properties -> new ItemAssemblyKit(SURVIVALISTS_AXE::stack, properties), "axe_kit");
 public static final FeatureItem<ItemAssemblyKit> SWORD_KIT = REGISTRY.item(properties -> new ItemAssemblyKit(SURVIVALISTS_SWORD::stack, properties), "sword_kit");
 public static final FeatureItem<ItemAssemblyKit> HOE_KIT = REGISTRY.item(properties -> new ItemAssemblyKit(SURVIVALISTS_HOE::stack, properties), "hoe_kit");
 static {
  ModFeatureRegistry.get(ForestryModuleIds.CORE).addRegistryListener(net.minecraft.core.registries.Registries.ITEM, () ->
    net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.registerForItems((stack, context) -> PIPETTE.item().createFluidHandler(context), PIPETTE.item()));
 }
 public static java.util.List<IItemFeature<?>> all() { return java.util.List.of(SHOVEL_KIT, PICKAXE_KIT, AXE_KIT, SWORD_KIT, HOE_KIT, PORTABLE_ANALYZER, WRENCH, PIPETTE, SOLDERING_IRON, SCOOP, PROVEN_SCOOP, BEESWAX, REFRACTORY_WAX, BROKEN_SURVIVALISTS_PICKAXE, BROKEN_SURVIVALISTS_SHOVEL, BROKEN_SURVIVALISTS_AXE, BROKEN_SURVIVALISTS_SWORD, BROKEN_SURVIVALISTS_HOE, SURVIVALISTS_PICKAXE, SURVIVALISTS_SHOVEL, SURVIVALISTS_AXE, SURVIVALISTS_SWORD, SURVIVALISTS_HOE); }
}
