package forestry.core.platform.data;

import forestry.api.ForestryDataMaps;
import forestry.api.core.data.DataMapSnapshot;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.DataResourceStore;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import java.util.Objects;

public final class ForestryDataMapRuntime {
    private static volatile MinecraftServer server;
    private static volatile DataMapSnapshot client = DataMapSnapshot.EMPTY;
    private ForestryDataMapRuntime() {}

    public static void initialize() {
        DataResourceLoader.get().registerReloadListener(DataMapLoader.ID, new DataMapLoader());
        ServerLifecycleEvents.SERVER_STARTING.register(current -> server = current);
        ServerLifecycleEvents.SERVER_STOPPED.register(current -> { if (server == current) server = null; });
        ForestryDataMaps.bindLookup(() -> {
            var current = server;
            if (current != null && (current.isSameThread() || FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)) return snapshot(current);
            return client;
        });
    }
    /** Keep this immutable snapshot when querying off the logical game thread. */
    public static DataMapSnapshot snapshot(MinecraftServer server) { return ((DataResourceStore) server).getOrThrow(DataMapLoader.KEY); }
    public static DataMapSnapshot clientSnapshot() { return client; }
    public static void replaceClient(DataMapSnapshot snapshot) { client = Objects.requireNonNull(snapshot); }
    public static void clearClient() { client = DataMapSnapshot.EMPTY; }
}
