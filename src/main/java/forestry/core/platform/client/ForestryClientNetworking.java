package forestry.core.platform.client;

import forestry.core.platform.network.PacketRegistry;
import forestry.core.platform.util.NetworkUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import forestry.core.engine.genetics.root.BreedingTrackerManager;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.data.ForestryDataMapRuntime;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ForestryClientNetworking {
    private static final java.util.concurrent.atomic.AtomicReference<net.minecraft.network.Connection> ACTIVE = new java.util.concurrent.atomic.AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicBoolean CLEANUP_PENDING = new java.util.concurrent.atomic.AtomicBoolean();
    private static net.minecraft.client.multiplayer.ClientLevel previousLevel;
    private ForestryClientNetworking() {}
    public static void initialize() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            if (previousLevel != null && previousLevel != level) forestry.core.platform.multiblock.MultiblockEventHandler.clear(previousLevel);
            previousLevel = level;
        });
        NetworkUtil.setClientSender(ClientPlayNetworking::send);
        ClientPlayConnectionEvents.INIT.register((connection, client) -> {
            // Native PLAY initialization runs on the client thread. Reconfiguration retains the transport.
            var transport = connection.getConnection();
            if (ACTIVE.getAndSet(transport) != transport) {
                CLEANUP_PENDING.set(false);
                clearConnectionState();
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((connection, client) -> {
            // This callback can run on Netty. Minecraft may discard queued executor tasks when closing a world.
            // Keep the cleanup request until a client tick observes it, and never clear a newer connection.
            if (ACTIVE.compareAndSet(connection.getConnection(), null)) CLEANUP_PENDING.set(true);
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var transport = ACTIVE.get();
            if (transport != null && !transport.isConnected() && ACTIVE.compareAndSet(transport, null)) CLEANUP_PENDING.set(true);
            if (ACTIVE.get() == null && CLEANUP_PENDING.getAndSet(false)) {
                clearConnectionState();
                ((forestry.core.engine.climate.ForestryClimateManager) forestry.api.IForestryApi.INSTANCE.getClimateManager()).clearClient();
            }
        });
        for (var registration : PacketRegistry.INSTANCE.clientHandlers()) install(registration);
    }
    public static String connectionDiagnostics() { var active = ACTIVE.get(); return "tracked=" + System.identityHashCode(active) + ", connected=" + (active != null && active.isConnected()) + ", pending=" + CLEANUP_PENDING.get(); }
    private static void clearConnectionState() {
        ForestryDataMapRuntime.clearClient();
        forestry.core.platform.recipes.RecipeManagers.replace(net.minecraft.world.item.crafting.RecipeMap.EMPTY);
        ForestryConfig.clearServerSnapshot();
        BreedingTrackerManager.INSTANCE.clearClientCache();
        forestry.core.platform.data.GeneticDataLoader.clearClient();
    }
    private static <P extends CustomPacketPayload> void install(PacketRegistry.ClientRegistration<P> registration) {
        ClientPlayNetworking.registerGlobalReceiver(registration.type(), (payload, context) -> {
            // The context player belongs to the connection that received this packet. Never apply a late old
            // connection packet to a player from the next server, even if it was queued before disconnect.
            if (ACTIVE.get() == context.player().connection.getConnection() && context.client().player == context.player() && context.client().getConnection() == context.player().connection)
                registration.handler().accept(payload, context.player());
        });
    }
}
