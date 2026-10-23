package forestry.core.features;

import forestry.api.modules.IForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.resources.*;
import forestry.core.content.tools.ForestersManualItem;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.registration.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** F1's complete, usable subset of the original CoreItems/CoreBlocks declarations. */
public final class CoreFabricModule implements IForestryModule {
    public static final List<String> MATERIALS = List.of("apatite", "raw_tin", "amber", "silicon", "tin_ingot", "bronze_ingot",
        "tin_nugget", "carton", "sturdy_casing", "hardened_casing", "impregnated_casing", "flexible_casing",
        "bronze_gear", "copper_gear", "tin_gear", "iron_gear", "solar_cell", "ash", "ash_brick", "honey_drop", "honeydew",
        "wax_brick", "refractory_wax_brick");

    @Override public Identifier getId() { return ForestryModuleIds.CORE; }
    @Override public String getOwnerModId() { return "forestry"; }

    @Override
    public void register() {
        FeatureRegistry registry = ModFeatureRegistry.get(getId());
        // Construct the registries before any component codec refers to their IDs.
        forestry.api.ForestryRegistries.PRODUCT_TYPE.size();
        CoreDataComponents.ANALYZED.getId();
        CoreProductTypes.ITEM.getId();
        CoreMutationConditionTypes.TEMPERATURE.getId();
        CoreFlowerTypeSerializers.TAG.getId();
        forestry.core.platform.data.FabricDomainBootstrap.declareTypes();
        forestry.core.platform.fluids.MilkFluid.declare();
        FabricMachines.register();
        CoreUtilityBlocks.register();
        forestry.core.content.backpacks.BackpacksFabric.register();
        FabricStorageBlocks.register();
        var fluidsRegistry = ModFeatureRegistry.create(ForestryModuleIds.FLUIDS, "forestry");
        for (var container : FluidsItems.CONTAINERS.getFeatures()) net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.registerForItems(
            (stack, context) -> container.item().createFluidHandler(context), container.item());
        forestry.apiculture.ApicultureFabric.register();
        forestry.arboriculture.ArboricultureFabric.initializeRegistration();
        forestry.core.platform.worldgen.ForestryFabricWorldgen.declare();
        forestry.core.platform.loot.CoreLootFunctions.ORGANISM.getId();
        forestry.arboriculture.loot.ArboricultureLootFunctions.COUNT.getId();
        List<IItemFeature<?>> materials = new ArrayList<>();
        materials.addAll(CoreUtilityItems.all());
        materials.addAll(FluidsItems.CONTAINERS.getFeatures());
        materials.add(registry.item(ForestersManualItem::new, "foresters_manual"));
        MATERIALS.forEach(id -> materials.add(registry.item(ItemForestry::new, id)));
        for (var fruit : forestry.core.platform.item.FruitItemType.values()) materials.add(registry.item(properties ->
            new forestry.core.platform.item.ItemForestryFood(fruit.heal, fruit.saturation, fruit.useTime, properties), fruit.getSerializedName()));
        for (String id : List.of("peat", "bituminous_peat", "mouldy_wheat", "decaying_wheat", "iodine_capsule", "dissipation_charge", "royal_jelly"))
            materials.add(registry.item(ItemForestry::new, id));
        for (String id : List.of("compost", "fertilizer", "mulch")) materials.add(registry.item(ItemFertilizer::new, id));
        var spectacles = registry.item(forestry.core.content.tools.ItemSpectacles::new, "spectacles");
        materials.add(spectacles);
        forestry.api.ForestryCapabilities.SPECTACLE_VISION.registerForItems((stack, context) -> forestry.core.content.tools.ItemSpectacles.SPECTACLE_VISION, spectacles.item());
        for (var type : List.of(net.minecraft.world.item.equipment.ArmorType.HELMET, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE,
                net.minecraft.world.item.equipment.ArmorType.LEGGINGS, net.minecraft.world.item.equipment.ArmorType.BOOTS)) {
            String suffix = switch (type) { case HELMET -> "hat"; case CHESTPLATE -> "shirt"; case LEGGINGS -> "pants"; case BOOTS -> "shoes"; default -> throw new IllegalStateException(); };
            var armor = registry.item(properties -> new forestry.apiculture.apiarist.ItemArmorApiarist(type, properties), "apiarists_" + suffix);
            materials.add(armor);
            forestry.api.ForestryCapabilities.BEE_PROTECTION.registerForItems((stack, context) -> forestry.apiculture.apiarist.ItemArmorApiarist.BeeProtection.INSTANCE, armor.item());
        }
        for (var fluid : forestry.core.platform.fluids.ForestryFluids.values()) materials.add(fluid.getBucketFeature());
        for (var tube : EnumElectronTube.values()) materials.add(registry.item(properties -> new ItemElectronTube(tube, properties), tube.registryPath()));
        for (var board : forestry.core.engine.circuits.EnumCircuitBoardType.values()) materials.add(registry.item(properties -> new forestry.core.engine.circuits.ItemCircuitBoard(board, properties), board.getSerializedName() + "_circuit_board"));
        materials.add(registry.item(forestry.core.engine.genetics.ItemResearchNote::new, "research_note"));
        for (var stage : forestry.api.apiculture.genetics.BeeLifeStage.values())
            registry.item(properties -> new forestry.apiculture.bees.ForestryBeeItem(stage, properties), stage.itemId().getPath());
        for (var comb : forestry.apiculture.bees.EnumHoneyComb.values())
            materials.add(registry.item(properties -> new forestry.apiculture.bees.ItemHoneyComb(comb, properties), comb.getSerializedName() + "_comb"));
        for (var pollen : forestry.apiculture.bees.EnumPollenCluster.values())
            materials.add(registry.item(properties -> new forestry.apiculture.bees.PollenClusterItem(pollen, properties), pollen == forestry.apiculture.bees.EnumPollenCluster.NORMAL ? "pollen_cluster" : "crystalline_pollen_cluster"));
        for (EnumCraftingMaterial type : EnumCraftingMaterial.values()) {
            materials.add(registry.item(properties -> new ItemCraftingMaterial(type, properties), type.getSerializedName()));
        }
        List<IItemFeature<?>> blocks = new ArrayList<>();
        blocks.addAll(FabricDecorativeBlocks.register(registry));
        blocks.add(CoreTerrain.HUMUS);
        blocks.add(CoreTerrain.BOG_EARTH);
        CoreTerrain.PEAT.id();
        for (EnumResourceType type : EnumResourceType.values()) {
            blocks.add(registry.block(properties -> new BlockResourceStorage(properties, type), Properties::of,
                ItemBlockForestry::new, type.getSerializedName() + "_block"));
        }
        var apatite = registry.block(properties -> new DropExperienceBlock(UniformInt.of(0, 4), properties),
            () -> Properties.ofFullCopy(Blocks.COAL_ORE), ItemBlockForestry::new, "apatite_ore");
        blocks.add(apatite);
        blocks.add(registry.block(properties -> new DropExperienceBlock(UniformInt.of(0, 4), properties),
            () -> Properties.ofFullCopy(apatite.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3).sound(SoundType.DEEPSLATE),
            ItemBlockForestry::new, "deepslate_apatite_ore"));
        var tin = registry.block(Block::new, () -> Properties.ofFullCopy(Blocks.COPPER_ORE), ItemBlockForestry::new, "tin_ore");
        blocks.add(tin);
        blocks.add(registry.block(Block::new,
            () -> Properties.ofFullCopy(tin.block()).mapColor(MapColor.DEEPSLATE).strength(4.5f, 3).sound(SoundType.DEEPSLATE),
            ItemBlockForestry::new, "deepslate_tin_ore"));
        blocks.add(registry.block(Block::new, () -> Properties.ofFullCopy(Blocks.RAW_COPPER_BLOCK), ItemBlockForestry::new, "raw_tin_block"));
        for (String id : List.of("ash_bricks", "wax_bricks", "refractory_wax_bricks")) {
            MapColor color = id.equals("ash_bricks") ? MapColor.COLOR_LIGHT_GRAY : id.equals("wax_bricks") ? MapColor.COLOR_YELLOW : MapColor.COLOR_RED;
            SoundType sound = id.equals("ash_bricks") ? SoundType.MUD_BRICKS : SoundType.MUD;
            stoneFamily(registry, blocks, id, color, sound);
            blocks.add(registry.block(Block::new, stoneProperties(Blocks.MUD_BRICKS, color, sound), ItemBlockForestry::new, "chiseled_" + id));
        }
        for (String id : List.of("waxstone", "refractory_waxstone", "honeystone")) {
            MapColor color = id.equals("honeystone") ? MapColor.COLOR_ORANGE : MapColor.SAND;
            for (String family : List.of(id, "cobbled_" + id, id + "_bricks", "polished_" + id)) {
                stoneFamily(registry, blocks, family, color, SoundType.CANDLE);
            }
            blocks.add(registry.block(Block::new, stoneProperties(Blocks.MUD_BRICKS, color, SoundType.CANDLE), ItemBlockForestry::new, "chiseled_" + id));
        }
        blocks.add(registry.block(Block::new, stoneProperties(Blocks.MUD_BRICKS, MapColor.SAND, SoundType.MUD_BRICKS), ItemBlockForestry::new, "ashen_wax_block"));
        blocks.add(registry.block(Block::new, stoneProperties(Blocks.MUD_BRICKS, MapColor.COLOR_BROWN, SoundType.MUD_BRICKS), ItemBlockForestry::new, "crispy_honey_block"));
        registry.creativeTab("forestry", tab -> tab.icon(() -> materials.get(1).stack()).displayItems((parameters, output) -> materials.forEach(item -> output.accept(item.stack()))));
        registry.creativeTab("building_blocks", tab -> tab.icon(() -> blocks.getFirst().stack()).displayItems((parameters, output) -> blocks.forEach(item -> output.accept(item.stack()))));
    }

    private static void stoneFamily(FeatureRegistry registry, List<IItemFeature<?>> blocks, String id, MapColor color, SoundType sound) {
        String shape = id.endsWith("bricks") ? id.substring(0, id.length() - 1) : id;
        blocks.add(registry.block(Block::new, stoneProperties(Blocks.MUD_BRICKS, color, sound), ItemBlockForestry::new, id));
        blocks.add(registry.block(properties -> new StairBlock(Blocks.MUD_BRICKS.defaultBlockState(), properties),
            stoneProperties(Blocks.MUD_BRICK_STAIRS, color, sound), ItemBlockForestry::new, shape + "_stairs"));
        blocks.add(registry.block(SlabBlock::new, stoneProperties(Blocks.MUD_BRICK_SLAB, color, sound), ItemBlockForestry::new, shape + "_slab"));
        blocks.add(registry.block(WallBlock::new, stoneProperties(Blocks.MUD_BRICK_WALL, color, sound), ItemBlockForestry::new, shape + "_wall"));
    }

    private static Supplier<Properties> stoneProperties(Block copy, MapColor color, SoundType sound) {
        return () -> Properties.ofFullCopy(copy).mapColor(color).sound(sound);
    }
}
