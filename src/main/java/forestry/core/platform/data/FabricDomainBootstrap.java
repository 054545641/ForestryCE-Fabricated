package forestry.core.platform.data;

import forestry.api.*;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.plugin.IForestryPlugin;
import forestry.apiimpl.*;
import forestry.apiimpl.plugin.GeneticRegistration;
import forestry.apiculture.plugin.ApicultureGenetics;
import net.minecraft.core.Registry;
import java.util.*;

/** Installs the original domain implementations after their item forms have been registered. */
public final class FabricDomainBootstrap {
    private FabricDomainBootstrap() {}
    public static void declareTypes() {
        Objects.requireNonNull(forestry.core.features.GeneticsRecipeTypes.BEE_MUTATION);
        forestry.apiculture.features.ApicultureBeeEffectTypes.APPLY_POTION.getId();
        forestry.apiculture.features.ApicultureProductTypes.FIREWORK.getId();
        forestry.apiculture.features.ApicultureBeeJubilanceTypes.REQUIRES_RESOURCE.getId();
        forestry.apiculture.features.ApicultureEffects.MATATA.getId();
        forestry.apiculture.particles.ApicultureParticles.SNOW_PARTICLE.getId();
        Objects.requireNonNull(forestry.core.platform.advancements.ForestryAdvancementTriggers.DISCOVER_SPECIES);
    }
    public static void initialize() {
        List<IForestryPlugin> plugins = new ArrayList<>();
        plugins.add(new ApicultureGenetics());
        plugins.add(new forestry.arboriculture.plugin.ArboricultureGenetics());
        ServiceLoader.load(IForestryPlugin.class).stream().map(ServiceLoader.Provider::get).filter(IForestryPlugin::shouldLoad)
            .sorted(Comparator.comparing(IForestryPlugin::id)).forEach(plugins::add);
        var registration = new GeneticRegistration();
        registration.registerFilterRuleTypes(forestry.core.content.sorting.DefaultFilterRuleType.values());
        plugins.forEach(plugin -> plugin.registerGenetics(registration));
        var types = registration.buildSpeciesTypes();
        var api = (ForestryApiImpl) IForestryApi.INSTANCE;
        var errorRegistration = new forestry.apiimpl.plugin.ErrorRegistration();
        for (var error : forestry.api.core.ForestryError.values()) errorRegistration.registerError(error);
        plugins.forEach(plugin -> plugin.registerErrors(errorRegistration));
        var errors = errorRegistration.getErrors();
        errors.sort(Comparator.comparing(forestry.api.core.IError::getId));
        if (errors.size() > Short.MAX_VALUE) throw new IllegalStateException("Too many Forestry error types");
        var errorIds = new it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap<forestry.api.core.IError>();
        var numericErrors = new it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap<forestry.api.core.IError>();
        var namedErrors = com.google.common.collect.ImmutableMap.<net.minecraft.resources.Identifier, forestry.api.core.IError>builder();
        for (int i = 0; i < errors.size(); i++) {
            var error = errors.get(i); errorIds.put((short) i, error); numericErrors.put(error, (short) i); namedErrors.put(error.getId(), error);
        }
        api.setErrorManager(new forestry.core.platform.errors.ErrorManager(errorIds, numericErrors, namedErrors.buildOrThrow()));
        forestry.core.engine.circuits.FabricCircuits.initialize(plugins);
        api.setGeneticManager(new GeneticManager(registration.buildTaxa(), types));
        api.setFilterManager(new forestry.core.content.sorting.FilterManager(registration.getFilterRuleTypes()));
        ((forestry.core.engine.genetics.ForestryFlowerTypeManager) api.getFlowerTypeManager()).setCodeFlowerTypes(registration.getFlowerTypes());
        for (var type : types.values()) {
            Registry.register(ForestryRegistries.SPECIES_TYPE, type.id(), type);
            initializeSpecies(type, plugins);
        }
        var pollen = com.google.common.collect.ImmutableMap.<net.minecraft.resources.Identifier, forestry.api.core.genetics.pollen.IPollenType<?>>builder();
        plugins.forEach(plugin -> plugin.registerPollen(type -> pollen.put(type.id(), type)));
        api.setPollenManager(new forestry.core.engine.genetics.PollenManager(pollen.buildOrThrow()));
        forestry.api.apiculture.BeeManager.armorApiaristHelper = new forestry.apiculture.apiarist.ArmorApiaristHelper();
        var hives = ((forestry.apiculture.bees.genetics.BeeSpeciesType) types.get(forestry.api.core.genetics.ForestrySpeciesTypes.BEE)).hiveRegistration();
        api.setHiveManager(new forestry.apiculture.hives.HiveManager(hives.buildHives(), hives.commonVillageHives(), hives.rareVillageHives()));
        net.fabricmc.fabric.api.resource.v1.DataResourceLoader.get().registerReloadListener(GeneticDataLoader.ID, new GeneticDataLoader());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(FabricDomainBootstrap::applyServer);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> { if (success) applyServer(server); });
    }
    private static void applyServer(net.minecraft.server.MinecraftServer server) {
        GeneticDataLoader.apply(((net.fabricmc.fabric.api.resource.v1.DataResourceStore) server).getOrThrow(GeneticDataLoader.KEY));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void initializeSpecies(ISpeciesType type, List<IForestryPlugin> plugins) {
        type.onSpeciesRegistered(type.handleSpeciesRegistration(plugins));
    }
}
