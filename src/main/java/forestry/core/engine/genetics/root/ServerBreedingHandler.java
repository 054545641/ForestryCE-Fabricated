package forestry.core.engine.genetics.root;

import com.mojang.authlib.GameProfile;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.platform.util.SavedDataMigration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import java.util.Objects;

public class ServerBreedingHandler implements BreedingTrackerManager.SidedHandler {
    // SavedDataStorage keys its cache by the complete SavedDataType (including its codec and factory).
    // Recreating those lambdas for each query would create parallel trackers for the same on-disk ID.
    private final java.util.Map<Identifier, SavedDataType<SavedData>> dataTypes = new java.util.concurrent.ConcurrentHashMap<>();
	@Override
	@SuppressWarnings("unchecked")
	public <T extends IBreedingTracker> T getTracker(ISpeciesType<?, ?> type, LevelAccessor level, @Nullable GameProfile profile) {
		ServerLevel overworld = Objects.requireNonNull(level.getServer()).overworld();
		Identifier id = type.id().withPath(path -> "breeding_tracker/" + path + "/" + (profile == null ? "common" : profile.id()));
		SavedDataType<SavedData> dataType = this.dataTypes.computeIfAbsent(id, key -> new SavedDataType<>(key,
			() -> (SavedData) type.createBreedingTracker(),
			CompoundTag.CODEC.xmap(tag -> (SavedData) type.createBreedingTracker(tag), data -> {
				CompoundTag tag = new CompoundTag();
				((IBreedingTracker) data).save(tag);
				return tag;
			}), null));
		SavedData data = SavedDataMigration.getOrCreate(overworld, dataType, type.getBreedingTrackerFile(profile),
			tag -> (SavedData) type.createBreedingTracker(tag));
		T tracker = (T) data;
		type.initializeBreedingTracker(tracker, overworld, profile);
		return tracker;
	}
}
