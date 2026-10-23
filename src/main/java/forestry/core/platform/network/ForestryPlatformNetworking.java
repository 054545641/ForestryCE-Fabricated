package forestry.core.platform.network;

import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.data.*;
import forestry.core.platform.network.packets.*;
import forestry.core.platform.util.NetworkUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ForestryPlatformNetworking {
    private ForestryPlatformNetworking() {}
    public static void initialize() {
        NetworkUtil.initialize();
        PacketRegistry.INSTANCE.clientbound(PacketRefractoryWax.TYPE, PacketRefractoryWax::encode, PacketRefractoryWax::decode, PacketRefractoryWax::handle);
        PacketRegistry.INSTANCE.clientbound(PacketItemStackDisplay.TYPE, PacketItemStackDisplay::encode, PacketItemStackDisplay::decode, PacketItemStackDisplay::handle);
        PacketRegistry.INSTANCE.serverbound(PacketGuiSelectRequest.TYPE, PacketGuiSelectRequest::encode, PacketGuiSelectRequest::decode, PacketGuiSelectRequest::handle);
        PacketRegistry.INSTANCE.clientbound(PacketActiveUpdate.TYPE, PacketActiveUpdate::encode, PacketActiveUpdate::decode, PacketActiveUpdate::handle);
        PacketRegistry.INSTANCE.clientbound(PacketGuiEnergy.TYPE, PacketGuiEnergy::encode, PacketGuiEnergy::decode, PacketGuiEnergy::handle);
        PacketRegistry.INSTANCE.clientbound(PacketGuiStream.TYPE, PacketGuiStream::encode, PacketGuiStream::decode, PacketGuiStream::handle);
        PacketRegistry.INSTANCE.clientbound(PacketErrorUpdate.TYPE, PacketErrorUpdate::encode, PacketErrorUpdate::decode, PacketErrorUpdate::handle);
        PacketRegistry.INSTANCE.clientbound(PacketTankLevelUpdate.TYPE, PacketTankLevelUpdate::encode, PacketTankLevelUpdate::decode, PacketTankLevelUpdate::handle);
        PacketRegistry.INSTANCE.clientbound(PacketSocketUpdate.TYPE, PacketSocketUpdate::encode, PacketSocketUpdate::decode, PacketSocketUpdate::handle);
        PacketRegistry.INSTANCE.serverbound(PacketPipetteClick.TYPE, PacketPipetteClick::encode, PacketPipetteClick::decode, PacketPipetteClick::handle);
        PacketRegistry.INSTANCE.serverbound(PacketChipsetClick.TYPE, PacketChipsetClick::encode, PacketChipsetClick::decode, PacketChipsetClick::handle);
        PacketRegistry.INSTANCE.serverbound(PacketSolderingIronClick.TYPE, PacketSolderingIronClick::encode, PacketSolderingIronClick::decode, PacketSolderingIronClick::handle);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(forestry.core.platform.block.TileStreamUpdateTracker::syncVisualUpdates);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> forestry.core.platform.block.TileStreamUpdateTracker.clear());
        PacketRegistry.INSTANCE.clientbound(PacketPlatformSnapshot.TYPE, PacketPlatformSnapshot::encode, PacketPlatformSnapshot::decode, PacketPlatformSnapshot::handle);
        PacketRegistry.INSTANCE.clientbound(PacketGenomeTrackerSync.TYPE, PacketGenomeTrackerSync::encode, PacketGenomeTrackerSync::decode, PacketGenomeTrackerSync::handle);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> synchronize(server, handler.player));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) for (var player : server.getPlayerList().getPlayers()) synchronize(server, player);
        });
    }
    public static void synchronize(MinecraftServer server, ServerPlayer player) {
        var snapshot = ForestryDataMapRuntime.snapshot(server);
        var genetics = ((net.fabricmc.fabric.api.resource.v1.DataResourceStore) server).getOrThrow(GeneticDataLoader.KEY);
        NetworkUtil.sendToPlayer(new PacketPlatformSnapshot(snapshot.revision(), DataMapSync.encode(snapshot, server.registryAccess()), ForestryConfig.serverSnapshot(), genetics.wire()), player);
    }
}
