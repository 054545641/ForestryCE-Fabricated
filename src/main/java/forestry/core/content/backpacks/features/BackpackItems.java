package forestry.core.content.backpacks.features;


import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.core.backpacks.EnumBackpackType;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.backpacks.BackpackDefinitions;

@FeatureProvider
public class BackpackItems {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.STORAGE);

	public static final FeatureItem<?> APIARIST_BACKPACK = REGISTRY.item(properties -> new forestry.core.content.backpacks.items.NaturalistBackpackItem(ForestrySpeciesTypes.BEE, BackpackDefinitions.APIARIST, properties.stacksTo(1).component(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY)), "apiarists_backpack");
	public static final FeatureItem<?> ARBORIST_BACKPACK = REGISTRY.item(properties -> new forestry.core.content.backpacks.items.NaturalistBackpackItem(ForestrySpeciesTypes.TREE, BackpackDefinitions.ARBORIST, properties.stacksTo(1).component(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY)), "arborists_backpack");
	public static final FeatureItem<?> LEPIDOPTERIST_BACKPACK = REGISTRY.item(properties -> new forestry.core.content.backpacks.items.NaturalistBackpackItem(ForestrySpeciesTypes.BUTTERFLY, BackpackDefinitions.LEPIDOPTERIST, properties.stacksTo(1).component(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY)), "lepidopterists_backpack");

	public static final FeatureItem<?> MINER_BACKPACK = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.MINER, EnumBackpackType.NORMAL, properties), "miner_backpack");
	public static final FeatureItem<?> MINER_BACKPACK_T_2 = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.MINER, EnumBackpackType.WOVEN, properties), "woven_miner_backpack");
	public static final FeatureItem<?> DIGGER_BACKPACK = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.DIGGER, EnumBackpackType.NORMAL, properties), "digger_backpack");
	public static final FeatureItem<?> DIGGER_BACKPACK_T_2 = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.DIGGER, EnumBackpackType.WOVEN, properties), "woven_digger_backpack");
	public static final FeatureItem<?> FORESTER_BACKPACK = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.FORESTER, EnumBackpackType.NORMAL, properties), "forester_backpack");
	public static final FeatureItem<?> FORESTER_BACKPACK_T_2 = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.FORESTER, EnumBackpackType.WOVEN, properties), "woven_forester_backpack");
	public static final FeatureItem<?> HUNTER_BACKPACK = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.HUNTER, EnumBackpackType.NORMAL, properties), "hunter_backpack");
	public static final FeatureItem<?> HUNTER_BACKPACK_T_2 = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.HUNTER, EnumBackpackType.WOVEN, properties), "woven_hunter_backpack");
	public static final FeatureItem<?> ADVENTURER_BACKPACK = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.ADVENTURER, EnumBackpackType.NORMAL, properties), "adventurer_backpack");
	public static final FeatureItem<?> ADVENTURER_BACKPACK_T_2 = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.ADVENTURER, EnumBackpackType.WOVEN, properties), "woven_adventurer_backpack");
	public static final FeatureItem<?> BUILDER_BACKPACK = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.BUILDER, EnumBackpackType.NORMAL, properties), "builder_backpack");
	public static final FeatureItem<?> BUILDER_BACKPACK_T_2 = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.BUILDER, EnumBackpackType.WOVEN, properties), "woven_builder_backpack");
	// Deviation from 1.20.1: brewer_bag/brewer_bag_woven renamed to match the ids the other nine backpacks use here
	public static final FeatureItem<?> BREWER_BACKPACK = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.BREWER, EnumBackpackType.NORMAL, properties), "brewer_backpack");
	public static final FeatureItem<?> BREWER_BACKPACK_T_2 = REGISTRY.item(properties -> BackpackDefinitions.BACKPACK_INTERFACE.createBackpack(BackpackDefinitions.BREWER, EnumBackpackType.WOVEN, properties), "woven_brewer_backpack");
}
