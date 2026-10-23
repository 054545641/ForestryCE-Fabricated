package forestry.core.platform.transfer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.util.*;

/** World work may open transfers, which Fabric forbids inside transaction close callbacks. */
public final class WorldWorkQueue {
    private static final Map<ServerLevel, ArrayDeque<Runnable>> PENDING = new IdentityHashMap<>();
    private WorldWorkQueue() {}
    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            var pending = PENDING.remove(level);
            if (pending != null) while (!pending.isEmpty()) pending.removeFirst().run();
        });
        // Paid progress is in the block entity. Discarding a queued action preserves that credit for reload.
        ServerLevelEvents.UNLOAD.register((server, level) -> PENDING.remove(level));
    }
    public static void enqueue(Level level, Runnable action) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread())
            throw new IllegalStateException("World work must be scheduled on the logical server thread");
        PENDING.computeIfAbsent(serverLevel, ignored -> new ArrayDeque<>()).addLast(action);
    }
}
