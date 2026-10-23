package forestry.core.platform.client;

import forestry.core.engine.genetics.root.ServerBreedingHandler;

import com.mojang.authlib.GameProfile;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientBreedingHandler extends ServerBreedingHandler {
	private final Map<ISpeciesType<?, ?>, IBreedingTracker> trackerByUID = new LinkedHashMap<>();

	@Override
	public void clearClientCache() {
		this.trackerByUID.clear();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IBreedingTracker> T getTracker(ISpeciesType<?, ?> type, LevelAccessor level, @Nullable GameProfile profile) {
		if (level instanceof ServerLevel) {
			return super.getTracker(type, level, profile);
		}
		T tracker = (T) this.trackerByUID.computeIfAbsent(type, (key) -> type.createBreedingTracker());
		type.initializeBreedingTracker(tracker, Minecraft.getInstance().level, profile);
		return tracker;
	}
}
