package forestry.environment;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = EnvironmentProbe.ID, value = Dist.CLIENT)
public final class EnvironmentClientProbe {
    private static int readyTicks;
    private static java.util.concurrent.CompletableFuture<Void> reload;

    private static void verifyAssets(Minecraft client) {
        if (Boolean.getBoolean("forestry.environment.mailAssets")) MailClientAssetProbe.verify(client);
        if (Boolean.getBoolean("forestry.environment.n5Models")) { N5ModelProbe.verify(client); N5MachineProbe.verify(client); N5WoodClientProbe.verify(client); N5GuiParticleProbe.verify(client); }
    }

    @SubscribeEvent
    public static void registerAtlas(net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent event) {
        if (Boolean.getBoolean("forestry.environment.mailAssets")) forestry.core.platform.render.ForestrySpriteUploader.registerAtlas(event);
    }

    @SubscribeEvent
    public static void registerTints(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("forestry", "mail_stamp"), forestry.mail.client.StampTintSource.CODEC);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (reload != null && reload.isDone()) {
            reload.join();
            reload = null;
            verifyAssets(client);
            LogUtils.getLogger().info("Client asset probes: resource reload verified");
            if (Boolean.getBoolean("forestry.environment.autoExit")) client.stop();
        }
        if (client.gui.screen() instanceof TitleScreen && client.gui.overlay() == null && ++readyTicks == 20) {
            LogUtils.getLogger().info("Forestry environment probe: title screen ready after resource loading");
            if (Boolean.getBoolean("forestry.environment.mailAssets") || Boolean.getBoolean("forestry.environment.n5Models")) {
                verifyAssets(client);
                reload = client.reloadResourcePacks();
            } else if (Boolean.getBoolean("forestry.environment.autoExit")) {
                client.stop();
            }
        }
    }
}

