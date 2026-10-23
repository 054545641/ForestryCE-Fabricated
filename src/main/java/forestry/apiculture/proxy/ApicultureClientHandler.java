package forestry.apiculture.proxy;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import forestry.core.platform.models.ClientManager;
import forestry.apiculture.features.ApicultureItems;
import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.apiculture.features.ApicultureCrates;
import forestry.apiculture.features.ApicultureMenuTypes;
import forestry.apiculture.alveary.AlvearyScreen;
import forestry.apiculture.alveary.AlvearyHygroregulatorScreen;
import forestry.apiculture.alveary.AlvearySieveScreen;
import forestry.apiculture.alveary.AlvearySwarmerScreen;
import forestry.apiculture.bees.BeeHousingIcon;
import forestry.apiculture.models.ModelBee;
import forestry.apiculture.particles.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.IEventBus;

public class ApicultureClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ApicultureClientHandler::clientPlayerTick);
		modBus.addListener(ApicultureClientHandler::registerBlockColors);
		modBus.addListener(ApicultureClientHandler::registerMenus);
		modBus.addListener(ApicultureClientHandler::registerParticleFactory);
		modBus.addListener(ApicultureClientHandler::registerModelLoaders);
	}


	private static void clientPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
		var player = event.getEntity();
		if (player.level().isClientSide() && player.getMainHandItem().getItem() instanceof forestry.apiculture.apiarist.ItemSmoker smoker) {
			smoker.clientIdleTick(player);
		}
	}

	private static void registerMenus(RegisterMenuScreensEvent event) {
		event.register(ApicultureMenuTypes.ALVEARY.menuType(), AlvearyScreen::new);
		event.register(ApicultureMenuTypes.ALVEARY_HYGROREGULATOR.menuType(), AlvearyHygroregulatorScreen::new);
		event.register(ApicultureMenuTypes.ALVEARY_SIEVE.menuType(), AlvearySieveScreen::new);
		event.register(ApicultureMenuTypes.ALVEARY_SWARMER.menuType(), AlvearySwarmerScreen::new);
		event.register(ApicultureMenuTypes.BEE_HOUSING.menuType(), BeeHousingScreen::new);
	}

	private static void registerParticleFactory(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(ApicultureParticles.BEE_EXPLORER_PARTICLE.get(), sprites -> (data, level, x, y, z, xSpeed, ySpeed, zSpeed, random) ->
			new BeeExploreParticle(level, x, y, z, data.destination(), data.color(), sprites.get(random)));
		event.registerSpriteSet(ApicultureParticles.BEE_ROUND_TRIP_PARTICLE.get(), sprites -> (data, level, x, y, z, xSpeed, ySpeed, zSpeed, random) ->
			new BeeRoundTripParticle(level, x, y, z, data.destination(), data.color(), sprites.get(random)));
		event.registerSpriteSet(ApicultureParticles.BEE_TARGET_ENTITY_PARTICLE.get(), sprites -> (data, level, x, y, z, xSpeed, ySpeed, zSpeed, random) -> {
			Entity entity = level.getEntity(data.entity());
			return entity == null ? null : new BeeTargetEntityParticle(level, x, y, z, entity, data.color(), sprites.get(random));
		});
		event.registerSpriteSet(ApicultureParticles.SNOW_PARTICLE.get(), sprites -> (data, level, x, y, z, xSpeed, ySpeed, zSpeed, random) ->
			new SnowParticle(level, x, y, z, sprites.get(random)));
	}

	private static void registerModelLoaders(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
		event.register(ForestryConstants.forestry("bee_ge"), ModelBee.CODEC);
	}

	private static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
		event.register(ClientManager.FORESTRY_BLOCK_COLOR, ApicultureBlocks.COMB_BLOCK.blockArray());
	}


}
