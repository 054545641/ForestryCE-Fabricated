package forestry.core.platform.compat;

import java.util.ServiceLoader;
import net.minecraft.world.entity.player.Player;

public interface NaturalistEyeAccess {
    boolean hasNaturalistEye(Player player);

    static boolean hasEye(Player player) {
        return Providers.ALL.stream().anyMatch(provider -> provider.hasNaturalistEye(player));
    }

    final class Providers {
        private static final java.util.List<NaturalistEyeAccess> ALL = ServiceLoader.load(NaturalistEyeAccess.class)
                .stream().map(ServiceLoader.Provider::get).toList();

        private Providers() {
        }
    }
}
