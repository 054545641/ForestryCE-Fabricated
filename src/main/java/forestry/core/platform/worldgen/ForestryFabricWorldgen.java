package forestry.core.platform.worldgen;

import com.mojang.datafixers.util.Either;
import forestry.api.ForestryConstants;
import forestry.apiculture.features.ApicultureFeatures;
import forestry.apiculture.worldgen.ApiaristPoolElement;
import forestry.apiculture.worldgen.TemplatePoolInsertion;
import forestry.arboriculture.features.ArboricultureFeatures;
import forestry.core.platform.config.ForestryConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class ForestryFabricWorldgen {
    public static void declare() {
        forestry.apiculture.apiarist.villagers.ApicultureVillagers.PROF_BEEKEEPER.getId();
        forestry.arboriculture.villagers.ArboricultureVillagers.ARBORIST.getId();
        ApicultureFeatures.HIVE.getId();
        ApicultureFeatures.APIARIST_POOL_ELEMENT_TYPE.getId();
        ArboricultureFeatures.TREE_DECORATOR.getId();
        ArboricultureFeatures.CUSTOM_TREE.getId();
        forestry.apiculture.loot.ApicultureLootFunctions.RANDOM_BEE_SPECIES.getId();
        Registry.register(BuiltInRegistries.FEATURE, ForestryConstants.forestry("ore_apatite"), new ConfiguredOre(() -> ForestryConfig.SERVER.spawnApatiteOre.get()));
        Registry.register(BuiltInRegistries.FEATURE, ForestryConstants.forestry("ore_tin"), new ConfiguredOre(() -> ForestryConfig.SERVER.spawnTinOre.get()));
    }

    public static void initialize() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof net.minecraft.world.entity.npc.villager.Villager villager
                    && villager.getVillagerData().profession().is(forestry.apiculture.apiarist.villagers.ApicultureVillagers.PROF_BEEKEEPER.getKey())
                    && villager.getGoalSelector().getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof forestry.apiculture.apiarist.ApiaristAI))
                villager.getGoalSelector().addGoal(6, new forestry.apiculture.apiarist.ApiaristAI(villager, 0.6));
        });
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, placed("ore_apatite"));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, placed("ore_tin"));
        BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Decoration.VEGETAL_DECORATION, ApicultureFeatures.PLACED_HIVE);
        BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Decoration.VEGETAL_DECORATION, ArboricultureFeatures.PLACED_TREE);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server ->
            forestry.apiculture.worldgen.VillagerJigsaw.init(server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL),
                server.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST)));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ForestryConstants.forestry(name));
    }

    private static final class ConfiguredOre extends OreFeature {
        private final BooleanSupplier enabled;

        private ConfiguredOre(BooleanSupplier enabled) {
            super(OreConfiguration.CODEC);
            this.enabled = enabled;
        }

        @Override
        public boolean place(FeaturePlaceContext<OreConfiguration> context) {
            return forestry.core.platform.data.LogicalData.withServerSnapshot(() -> this.enabled.getAsBoolean() && super.place(context));
        }
    }
}
