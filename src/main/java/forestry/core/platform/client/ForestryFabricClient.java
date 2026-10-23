package forestry.core.platform.client;

import forestry.Forestry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.color.item.ItemTintSources;
import forestry.api.ForestryConstants;
import forestry.core.platform.models.ForestryItemTintSource;

public final class ForestryFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ForestryClientInitialization.initialize();
        forestry.arboriculture.client.SignEditorTexture.register(java.util.Arrays.stream(forestry.arboriculture.wood.ForestryWoodType.VALUES).map(type -> type.getSerializedName()).toList());
        var particles = net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance();
        particles.register(forestry.apiculture.particles.ApicultureParticles.BEE_EXPLORER_PARTICLE.get(), sprites -> (data, level, posX, posY, posZ, xSpeed, ySpeed, zSpeed, random) ->
            new forestry.apiculture.particles.BeeExploreParticle(level, posX, posY, posZ, data.destination(), data.color(), sprites.get(random)));
        particles.register(forestry.apiculture.particles.ApicultureParticles.BEE_ROUND_TRIP_PARTICLE.get(), sprites -> (data, level, posX, posY, posZ, xSpeed, ySpeed, zSpeed, random) ->
            new forestry.apiculture.particles.BeeRoundTripParticle(level, posX, posY, posZ, data.destination(), data.color(), sprites.get(random)));
        particles.register(forestry.apiculture.particles.ApicultureParticles.BEE_TARGET_ENTITY_PARTICLE.get(), sprites -> (data, level, posX, posY, posZ, xSpeed, ySpeed, zSpeed, random) -> {
            var entity = level.getEntity(data.entity());
            return entity == null ? null : new forestry.apiculture.particles.BeeTargetEntityParticle(level, posX, posY, posZ, entity, data.color(), sprites.get(random));
        });
        particles.register(forestry.apiculture.particles.ApicultureParticles.SNOW_PARTICLE.get(), sprites -> (data, level, posX, posY, posZ, xSpeed, ySpeed, zSpeed, random) ->
            new forestry.apiculture.particles.SnowParticle(level, posX, posY, posZ, sprites.get(random)));
        forestry.core.platform.client.render.NaturalistEyeRenderer.initialize();
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(forestry.arboriculture.client.BoatRenderGeometry.BOAT, net.minecraft.client.model.object.boat.BoatModel::createBoatModel);
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(forestry.arboriculture.client.BoatRenderGeometry.CHEST_BOAT, net.minecraft.client.model.object.boat.BoatModel::createChestBoatModel);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(forestry.arboriculture.features.ArboricultureEntities.BOAT.entityType(), context -> new forestry.arboriculture.client.ForestryBoatRenderer(context, false));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(forestry.arboriculture.features.ArboricultureEntities.CHEST_BOAT.entityType(), context -> new forestry.arboriculture.client.ForestryBoatRenderer(context, true));
        ForestryClientNetworking.initialize();
        forestry.core.platform.data.GeneticDataLoader.afterApply(() -> {
            if (!forestry.core.platform.data.LogicalData.isServer()) net.minecraft.world.item.CreativeModeTabs.CACHED_PARAMETERS = null;
        });
        forestry.arboriculture.client.TreeBlockModel.initialize();
        forestry.arboriculture.client.FabricLeafModels.initialize();
        ForestryBlockColors.initialize();
        net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(forestry.core.platform.particles.CoreParticles.REFRACTORY_WAX.get(), RefractoryWaxParticle::new);
        forestry.core.platform.client.render.FabricPlatformModels.initialize();
        net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) ->
            forestry.core.platform.recipes.RecipeManagers.replace(net.minecraft.world.item.crafting.RecipeMap.create(recipes.recipes())));
        forestry.core.engine.circuits.CircuitBoard.setExpandedTooltip(() -> net.minecraft.client.Minecraft.getInstance().hasShiftDown());
        forestry.core.engine.genetics.ItemGE.setTooltipDetails(() -> net.minecraft.client.Minecraft.getInstance().hasShiftDown());
        forestry.core.platform.util.EffectParticles.install(request -> {
            switch (request.kind()) {
                case IGNITION -> forestry.core.platform.client.ParticleRender.addEntityIgnitionFX((net.minecraft.client.multiplayer.ClientLevel) request.level(), request.x(), request.y(), request.z());
                case POTION -> forestry.core.platform.client.ParticleRender.addEntityPotionFX(request.level(), request.x(), request.y(), request.z(), request.color());
                case SNOW -> forestry.apiculture.client.BeeParticleRender.addEntitySnowFX(request.level(), request.x(), request.y(), request.z());
            }
        });
        forestry.core.engine.genetics.root.BreedingTrackerManager.installClientHandler(new forestry.core.platform.client.ClientBreedingHandler());
        // Fabric API exposes this vanilla registry through its transitive access widener.
        ItemTintSources.ID_MAPPER.put(ForestryConstants.forestry("item"), ForestryItemTintSource.CODEC);
        net.minecraft.client.renderer.item.ItemModels.ID_MAPPER.put(ForestryConstants.forestry("bee_ge"), forestry.apiculture.models.ModelBee.CODEC);
        net.minecraft.client.renderer.item.ItemModels.ID_MAPPER.put(ForestryConstants.forestry("sapling_ge"), forestry.arboriculture.client.TreeItemModel.CODEC);
        for (var fluid : forestry.core.platform.fluids.ForestryFluids.values()) {
            var textures = fluid.getFeature().properties().resources;
            var still = new net.minecraft.client.resources.model.sprite.Material(textures[0], true);
            var flowing = new net.minecraft.client.resources.model.sprite.Material(
                fluid == forestry.core.platform.fluids.ForestryFluids.WAX || fluid == forestry.core.platform.fluids.ForestryFluids.EXPERIENCE ? textures[1] : textures[0], true);
            net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(fluid.getFluid(), fluid.getFlowing(),
                new net.minecraft.client.renderer.block.FluidModel.Unbaked(still, flowing, null, state -> 0xffffffff));
        }
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private int readyTicks;
            @Override public void onEndTick(net.minecraft.client.Minecraft client) {
                if (client.gui.screen() instanceof TitleScreen && client.gui.overlay() == null && ++readyTicks == 20) {
                    Forestry.LOGGER.info("FORESTRY_F2_CLIENT_READY");
                    if (Boolean.getBoolean("forestry.clientAutoExit")) client.stop();
                }
            }
        });
    }
}
