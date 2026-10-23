package forestry.core.platform.util;

import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/** Client effect dispatch; common genetics classes never link client renderer types. */
public final class EffectParticles {
    public enum Kind { IGNITION, POTION, SNOW }
    public record Request(Kind kind, Level level, double x, double y, double z, int color) {}
    private static Consumer<Request> renderer = request -> {};
    private EffectParticles() {}
    public static void install(Consumer<Request> renderer) { EffectParticles.renderer = java.util.Objects.requireNonNull(renderer); }
    public static void spawn(Kind kind, Level level, double x, double y, double z, int color) {
        if (level.isClientSide()) renderer.accept(new Request(kind, level, x, y, z, color));
    }
}
