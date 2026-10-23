package forestry.core.platform.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Client connection state must not overwrite the integrated server's authoritative domain data. */
public final class LogicalData<T> {
    private static volatile MinecraftServer server;
    private static final AtomicReference<Map<LogicalData<?>, Object>> SERVER = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<LogicalData<?>, Object>> CLIENT = new AtomicReference<>(Map.of());
    private static final ThreadLocal<Staging> STAGING = new ThreadLocal<>();
    private static final ThreadLocal<Map<LogicalData<?>, Object>> SERVER_READ = new ThreadLocal<>();
    private record Staging(boolean serverSide, Map<LogicalData<?>, Object> base, Map<LogicalData<?>, Object> changes) {}
    public record Update(boolean serverSide, Map<LogicalData<?>, Object> changes) {
        public Update complete(Runnable builder) {
            return prepare(serverSide, () -> { STAGING.get().changes.putAll(changes); builder.run(); });
        }
        public void publish() {
            if (serverSide != isServer()) throw new IllegalStateException("Cannot publish domain data on the other logical side");
            var target = serverSide ? SERVER : CLIENT;
            target.updateAndGet(previous -> { var next = new HashMap<>(previous); next.putAll(changes); return Map.copyOf(next); });
        }
    }
    /** Builds every dependent registry against one unpublished candidate; exceptions discard all changes. */
    public static Update prepare(boolean serverSide, Runnable builder) {
        if (STAGING.get() != null) throw new IllegalStateException("Nested domain publication");
        var staging = new Staging(serverSide, (serverSide ? SERVER : CLIENT).get(), new HashMap<>());
        STAGING.set(staging);
        try { builder.run(); return new Update(serverSide, Map.copyOf(staging.changes)); }
        finally { STAGING.remove(); }
    }
    public static boolean isPreparing() { return STAGING.get() != null; }
    public static <R> R withServerSnapshot(Supplier<R> action) {
        if (SERVER_READ.get() != null) return action.get();
        SERVER_READ.set(SERVER.get());
        try { return action.get(); }
        finally { SERVER_READ.remove(); }
    }
    private volatile T serverValue;
    private volatile T clientValue;
    public LogicalData(T initial) { initializeBoth(initial); }
    public static void initializeLifecycle() {
        ServerLifecycleEvents.SERVER_STARTING.register(current -> server = current);
        ServerLifecycleEvents.SERVER_STOPPED.register(current -> { if (server == current) server = null; });
    }
    public static boolean isServer() {
        var staging = STAGING.get();
        if (staging != null) return staging.serverSide;
        if (SERVER_READ.get() != null) return true;
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER || (server != null && server.isSameThread());
    }
    public static MinecraftServer server() { return server; }
    @SuppressWarnings("unchecked") public T get() {
        var staging = STAGING.get();
        if (staging != null) return (T) staging.changes.getOrDefault(this, staging.base.getOrDefault(this, staging.serverSide ? serverValue : clientValue));
        var snapshot = SERVER_READ.get();
        if (snapshot != null) return (T) snapshot.getOrDefault(this, serverValue);
        boolean side = isServer();
        return (T) (side ? SERVER : CLIENT).get().getOrDefault(this, side ? serverValue : clientValue);
    }
    public void set(T value) {
        Objects.requireNonNull(value);
        var staging = STAGING.get();
        if (staging != null) { staging.changes.put(this, value); return; }
        (isServer() ? SERVER : CLIENT).updateAndGet(previous -> {
            var next = new HashMap<>(previous); next.put(this, value); return Map.copyOf(next);
        });
    }
    public void initializeBoth(T value) { serverValue = clientValue = Objects.requireNonNull(value); }
}
