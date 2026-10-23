package forestry.core.engine.genetics.root;

import com.mojang.authlib.GameProfile;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.IBreedingTrackerManager;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

public enum BreedingTrackerManager implements IBreedingTrackerManager {
	INSTANCE;

	private static SidedHandler BREEDING_HANDLER = new ServerBreedingHandler();
    public static void installClientHandler(SidedHandler handler) { BREEDING_HANDLER = java.util.Objects.requireNonNull(handler); }

	@Override
	public <T extends IBreedingTracker> T getTracker(ISpeciesType<?, ?> type, LevelAccessor level, @Nullable GameProfile profile) {
		return BREEDING_HANDLER.getTracker(type, level, profile);
	}

	public void clearClientCache() {
		BREEDING_HANDLER.clearClientCache();
	}

	public interface SidedHandler {
		default void clearClientCache() {
		}

		<T extends IBreedingTracker> T getTracker(ISpeciesType<?, ?> type, LevelAccessor level, @Nullable GameProfile profile);
	}
}
