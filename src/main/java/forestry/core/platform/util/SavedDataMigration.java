package forestry.core.platform.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SavedDataMigration {
    private static final java.util.Map<ServerLevel, java.util.Map<Identifier, SavedDataType<?>>> TYPES = new java.util.IdentityHashMap<>();
    static {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> TYPES.keySet().removeIf(level -> level.getServer() == server));
    }
	private SavedDataMigration() {
	}

	public static <T extends SavedData> T getOrCreate(ServerLevel level, SavedDataType<T> type, String legacyName, Function<CompoundTag, T> readLegacy) {
        type = canonicalType(level, type);
		Path root = level.getServer().getWorldPath(LevelResource.ROOT);
		Path directory = DimensionType.getStorageFolder(level.dimension(), root).resolve("data");
		Path oldDirectory = level.dimension().equals(Level.OVERWORLD) ? root.resolve("data")
			: level.dimension().equals(Level.NETHER) ? root.resolve("DIM-1/data")
			: level.dimension().equals(Level.END) ? root.resolve("DIM1/data") : directory;
		return getOrCreate(level.getDataStorage(), directory, type, type.constructor(), readLegacy,
			legacyName, directory, oldDirectory);
	}
    @SuppressWarnings("unchecked") private static <T extends SavedData> SavedDataType<T> canonicalType(ServerLevel level, SavedDataType<T> type) {
        return (SavedDataType<T>) TYPES.computeIfAbsent(level, ignored -> new java.util.HashMap<>()).computeIfAbsent(type.id(), ignored -> type);
    }

	public static <T extends SavedData> T getOrCreate(SavedDataStorage storage, Path directory, SavedDataType<T> type,
		Supplier<T> create, Function<CompoundTag, T> decodeLegacy, String legacyName, Path... legacyDirectories) {
		T existing = storage.get(type);
		if (existing != null) {
			return existing;
		}
		// storage.get also returns null on decode failure
		if (Files.exists(type.id().withSuffix(".dat").resolveAgainst(directory))) {
			throw new IllegalStateException("Could not decode saved data " + type.id());
		}
		try {
			for (Path legacyDirectory : legacyDirectories) {
				Optional<CompoundTag> tag = readLegacy(legacyDirectory, legacyName);
				if (tag.isPresent()) {
					T migrated = decodeLegacy.apply(tag.get());
					storage.set(type, migrated);
					return migrated;
				}
			}
		} catch (IOException exception) {
			throw new UncheckedIOException("Could not migrate saved data " + type.id(), exception);
		}
		T fresh = create.get();
		storage.set(type, fresh);
		return fresh;
	}

	public static Optional<CompoundTag> readLegacy(Path directory, String filename) throws IOException {
		Path root = directory.toAbsolutePath().normalize();
		Path file = root.resolve(filename + ".dat").normalize();
		if (!root.equals(file.getParent())) {
			throw new IOException("Invalid legacy saved data filename: " + filename);
		}
		if (!Files.exists(file)) {
			return Optional.empty();
		}
		try (var input = new BufferedInputStream(Files.newInputStream(file))) {
			input.mark(2);
			boolean compressed = input.read() == 0x1f && input.read() == 0x8b;
			input.reset();
			CompoundTag tag = compressed ? NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()) : NbtIo.read(new DataInputStream(input));
			return Optional.of(tag.getCompound("data").orElseThrow(() -> new IOException("Missing data compound in " + file)));
		}
	}
}
