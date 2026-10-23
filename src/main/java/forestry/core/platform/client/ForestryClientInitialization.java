package forestry.core.platform.client;

import forestry.api.IForestryApi;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.genetics.IAnalyzerPlugin;
import forestry.api.core.genetics.ISpeciesType;
import forestry.apiimpl.client.ButterflyClientManager;
import forestry.apiimpl.client.ForestryClientApiImpl;
import forestry.apiimpl.client.genetics.GeneticClientManager;
import forestry.apiimpl.client.plugin.ClientRegistration;
import forestry.apiculture.client.BeeClientManager;
import forestry.apiculture.client.plugin.ApicultureClientRegistration;
import forestry.arboriculture.client.TreeClientManager;
import forestry.arboriculture.client.plugin.ArboricultureClientRegistration;
import forestry.core.platform.render.ColourProperties;
import forestry.core.platform.render.ForestrySpriteUploader;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

public final class ForestryClientInitialization {
    public static void initialize() {
        var api = (ForestryClientApiImpl) IForestryClientApi.INSTANCE;
        api.initializeTextureManager();
        ForestrySpriteUploader.registerAtlas();
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(forestry.api.ForestryConstants.forestry("colours"), ColourProperties.INSTANCE);
        var registration = new ClientRegistration();
        new ApicultureClientRegistration().accept(registration);
        new ArboricultureClientRegistration().accept(registration);
        for (Consumer<forestry.api.client.plugin.IClientRegistration> extension : FabricLoader.getInstance().getEntrypoints("forestry:client", Consumer.class)) {
            extension.accept(registration);
        }
        var defaults = new IdentityHashMap<forestry.api.core.genetics.ILifeStage, net.minecraft.resources.Identifier>();
        for (var stage : forestry.api.apiculture.genetics.BeeLifeStage.values()) defaults.put(stage, registration.getDefaultBeeModel(stage));
        api.setBeeManager(new BeeClientManager(defaults, new IdentityHashMap<>(registration.getBeeModels())));
        api.setTreeManager(new TreeClientManager(registration.getLeafSprites(), registration.getTints(), registration.getSaplingModels()));
        api.setButterflyManager(new ButterflyClientManager(registration.getButterflyTextures()));
        var analyzers = new IdentityHashMap<ISpeciesType<?, ?>, IAnalyzerPlugin<?, ?>>();
        for (var type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
            var plugin = registration.getAnalyzerPlugins().get(type.id());
            if (plugin != null) analyzers.put(type, plugin);
        }
        api.setGeneticsManager(new GeneticClientManager(analyzers));
        ForestryScreens.initialize();
    }
}
