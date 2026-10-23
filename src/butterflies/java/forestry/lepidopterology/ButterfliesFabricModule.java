package forestry.lepidopterology;

import forestry.Forestry;
import forestry.api.modules.*;
import forestry.core.platform.registration.FeatureRegistry;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Module lifecycle is active; genome-backed items/entities require F2 and rendering requires F3. */
public final class ButterfliesFabricModule implements IForestryModule {
    @Override public Identifier getId() { return ForestryModuleIds.LEPIDOPTEROLOGY; }
    @Override public String getOwnerModId() { return "forestrybutterflies"; }
    @Override public List<Identifier> getModuleDependencies() { return List.of(ForestryModuleIds.CORE); }
    @Override public void register() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) ->
            dispatcher.register(net.minecraft.commands.Commands.literal("forestry").then(forestry.lepidopterology.commands.CommandButterfly.register())));
        var registry = forestry.core.platform.registration.ModFeatureRegistry.get(getId());
        forestry.lepidopterology.features.LepidopterologyItems.BUTTERFLY_GE.id();
        forestry.lepidopterology.features.LepidopterologyBlocks.COCOON.id();
        forestry.lepidopterology.features.LepidopterologyTiles.COCOON.getRegistry();
        forestry.lepidopterology.features.LepidopterologyRecipes.MATING_SERIALIZER.getId();
        var entity = forestry.lepidopterology.features.LepidopterologyEntities.BUTTERFLY;
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(entity.entityType(), entity.createAttributes());
        forestry.core.platform.data.GeneticDataLoader.registerFolder("butterfly_species", forestry.lepidopterology.butterflies.genetics.ButterflySpeciesDefinition::codec, forestry.lepidopterology.butterflies.genetics.LepidopterologyReloadHandler::rebuildButterflySpecies);
        forestry.core.platform.data.GeneticDataLoader.afterApply(forestry.lepidopterology.butterflies.genetics.LepidopterologyReloadHandler::refreshLoadedEntities);
        registry.creativeTab("lepidopterology", tab -> tab.icon(() -> forestry.lepidopterology.features.LepidopterologyItems.BUTTERFLY_GE.stack())
            .displayItems((parameters, output) -> forestry.core.platform.util.SpeciesUtil.addTypeToCreativeTab(output, forestry.api.core.genetics.ForestrySpeciesTypes.BUTTERFLY)));
    }
}
