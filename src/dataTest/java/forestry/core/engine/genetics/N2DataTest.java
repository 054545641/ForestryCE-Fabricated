package forestry.core.engine.genetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import forestry.core.platform.network.IStreamable;
import forestry.core.platform.util.LazyMapCodec;
import forestry.core.platform.util.LazyStreamCodec;
import forestry.core.platform.util.NBTUtilForestry;
import forestry.core.platform.util.SavedDataMigration;
import forestry.core.platform.util.ValueIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class N2DataTest {
	private static int checks;

	public static void main(String[] args) throws Exception {
		reloadDefinitions();
		valueIO();
		streamData();
		legacyFiles();
		lazyCodecs();
		System.out.println("N2 data contracts: " + checks + " checks passed");
	}

	private static void reloadDefinitions() throws Exception {
		AtomicInteger resolutions = new AtomicInteger();
		AtomicBoolean fail = new AtomicBoolean();
		var published = new ArrayList<Map<Identifier, Integer>>();
		DefinitionManager<Integer> manager = new DefinitionManager<>("test", () -> {
			resolutions.incrementAndGet();
			return Codec.INT.flatXmap(value -> value < 0 ? DataResult.error(() -> "invalid partial", 99) : DataResult.success(value), DataResult::success);
		}, values -> {
			if (fail.get()) {
				throw new IllegalStateException("rebuild failed");
			}
			published.add(values);
		}) {};
		check(0, resolutions.get(), "codec is lazy until apply");
		Identifier a = Identifier.parse("forestry:a");
		Identifier z = Identifier.parse("forestry:z");
		Map<Identifier, JsonElement> json = new LinkedHashMap<>();
		json.put(z, new JsonPrimitive(2));
		json.put(Identifier.parse("forestry:invalid"), new JsonPrimitive("wrong type"));
		json.put(Identifier.parse("forestry:partial"), new JsonPrimitive(-1));
		json.put(a, new JsonPrimitive(1));
		manager.apply(json, null, null);
		check(Map.of(a, 1, z, 2), manager.getDefinitions(), "reject invalid and partial definitions");
		check(List.of(a, z), List.copyOf(manager.getDefinitions().keySet()), "deterministic order");
		check(manager.getDefinitions(), published.getFirst(), "published rebuild matches snapshot");
		expect(UnsupportedOperationException.class, () -> manager.getDefinitions().put(a, 3));
		fail.set(true);
		expect(IllegalStateException.class, () -> manager.apply(Map.of(a, new JsonPrimitive(4)), null, null));
		check(Map.of(a, 1, z, 2), manager.getDefinitions(), "failed rebuild keeps previous definitions");
		fail.set(false);
		manager.apply(Map.of(a, new JsonPrimitive(5)), null, null);
		check(Map.of(a, 5), manager.getDefinitions(), "reload removes deleted definitions");
		Map<Identifier, Integer> sync = new LinkedHashMap<>(Map.of(z, 8));
		manager.setDefinitions(sync);
		sync.clear();
		check(Map.of(z, 8), manager.getDefinitions(), "network snapshot is detached");
		manager.apply(Map.of(), null, null);
		check(Map.of(), manager.getDefinitions(), "empty reload clears definitions");
	}

	private static void valueIO() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Health", 42);
		tag.putByteArray("dataBytes", new byte[]{1, 2, 3});
		CompoundTag child = new CompoundTag();
		child.putString("Species", "forestry:forest");
		tag.put("Genome", child);
		ProblemReporter.Collector problems = new ProblemReporter.Collector();
		var output = TagValueOutput.createWithoutContext(problems);
		output.putString("id", "forestry:test");
		ValueIOUtil.mergeInto(output, tag);
		CompoundTag saved = output.buildResult();
		check("forestry:test", saved.getStringOr("id", ""), "base block entity fields survive merge");
		var input = TagValueInput.create(problems, RegistryAccess.EMPTY, saved);
		CompoundTag restored = ValueIOUtil.toTag(input);
		check(saved, restored, "flat NBT round trip");
		child.putString("Species", "changed");
		check("forestry:forest", restored.getCompoundOrEmpty("Genome").getStringOr("Species", ""), "output owns nested data");
		restored.getCompoundOrEmpty("Genome").putString("Species", "also_changed");
		check("forestry:forest", saved.getCompoundOrEmpty("Genome").getStringOr("Species", ""), "input conversion owns nested data");
		check(true, problems.isEmpty(), "no ValueIO problems");
		ValueIOUtil.readTag(saved, RegistryAccess.EMPTY, value -> {
			check(42, value.getIntOr("Health", 0), "legacy load receives ValueInput");
			check(RegistryAccess.EMPTY, value.lookup(), "legacy load retains registry context");
		});
		GameProfile profile = new GameProfile(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"), "Keeper");
		check(profile, NBTUtilForestry.readGameProfile(NBTUtilForestry.writeGameProfile(new CompoundTag(), profile)), "legacy profile round trip");
		NBTUtilForestry.putBlockPos(tag, "Pos", new BlockPos(-4, 80, 200));
		check(new BlockPos(-4, 80, 200), NBTUtilForestry.getBlockPos(tag, "Pos").orElseThrow(), "legacy position round trip");
		tag.putIntArray("Pos", new int[]{1, 2});
		check(true, NBTUtilForestry.getBlockPos(tag, "Pos").isEmpty(), "malformed position rejected");
	}

	private static void streamData() throws Exception {
		class Stream implements IStreamable {
			RegistryFriendlyByteBuf last;
			int value = 123;
			boolean fail;
			public void writeData(RegistryFriendlyByteBuf data) {
				this.last = data;
				if (this.fail) throw new IllegalStateException("encode");
				data.writeInt(this.value);
			}
			public void readData(RegistryFriendlyByteBuf data) {
				this.last = data;
				if (this.fail) throw new IllegalStateException("decode");
				this.value = data.readInt();
			}
		}
		Stream stream = new Stream();
		CompoundTag tag = NBTUtilForestry.writeStreamableToNbt(stream, new CompoundTag(), RegistryAccess.EMPTY);
		check(0, stream.last.refCnt(), "encode releases buffer");
		stream.value = 0;
		NBTUtilForestry.readStreamableFromNbt(stream, tag, RegistryAccess.EMPTY);
		check(123, stream.value, "binary NBT round trip");
		check(0, stream.last.refCnt(), "decode releases buffer");
		stream.fail = true;
		expect(IllegalStateException.class, () -> NBTUtilForestry.writeStreamableToNbt(stream, new CompoundTag(), RegistryAccess.EMPTY));
		check(0, stream.last.refCnt(), "failed encode releases buffer");
		expect(IllegalStateException.class, () -> NBTUtilForestry.readStreamableFromNbt(stream, tag, RegistryAccess.EMPTY));
		check(0, stream.last.refCnt(), "failed decode releases buffer");
	}

	private static void legacyFiles() throws Exception {
		Path directory = Files.createTempDirectory(Path.of("."), "legacy-tracker-");
		CompoundTag record = new CompoundTag();
		record.putInt("QueensTotal", 17);
		CompoundTag envelope = new CompoundTag();
		envelope.put("data", record);
		NbtIo.writeCompressed(envelope, directory.resolve("ApiaristTracker.common.dat"));
		check(record, SavedDataMigration.readLegacy(directory, "ApiaristTracker.common").orElseThrow(), "compressed legacy tracker");
		try (var output = new DataOutputStream(Files.newOutputStream(directory.resolve("plain.dat")))) {
			NbtIo.write(envelope, output);
		}
		check(record, SavedDataMigration.readLegacy(directory, "plain").orElseThrow(), "uncompressed legacy tracker");
		check(true, SavedDataMigration.readLegacy(directory, "absent").isEmpty(), "new tracker has no legacy record");
		expect(IOException.class, () -> SavedDataMigration.readLegacy(directory, "../outside"));
		Files.write(directory.resolve("broken.dat"), new byte[]{1, 2, 3});
		expect(IOException.class, () -> SavedDataMigration.readLegacy(directory, "broken"));
		NbtIo.writeCompressed(new CompoundTag(), directory.resolve("missing-data.dat"));
		expect(IOException.class, () -> SavedDataMigration.readLegacy(directory, "missing-data"));
		check(true, Files.exists(directory.resolve("ApiaristTracker.common.dat")), "legacy file remains intact");
	}

	private static void lazyCodecs() {
		AtomicInteger mapLoads = new AtomicInteger();
		var codec = new LazyMapCodec<>(() -> {
			mapLoads.incrementAndGet();
			return Codec.INT.fieldOf("value");
		}).codec();
		check(0, mapLoads.get(), "map codec initialization deferred");
		var json = codec.encodeStart(JsonOps.INSTANCE, 17).getOrThrow();
		check(17, codec.parse(JsonOps.INSTANCE, json).getOrThrow(), "lazy map codec round trip");
		check(1, mapLoads.get(), "map codec initialized once");
		AtomicInteger streamLoads = new AtomicInteger();
		var stream = new LazyStreamCodec<ByteBuf, Integer>(() -> {
			streamLoads.incrementAndGet();
			return StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
		});
		check(0, streamLoads.get(), "stream codec initialization deferred");
		ByteBuf buffer = Unpooled.buffer();
		try {
			stream.encode(buffer, 99);
			check(99, stream.decode(buffer), "lazy stream codec round trip");
			check(1, streamLoads.get(), "stream codec initialized once");
		} finally {
			buffer.release();
		}
	}

	private static void check(Object expected, Object actual, String scenario) {
		if (!java.util.Objects.equals(expected, actual)) throw new AssertionError(scenario + ": " + actual);
		checks++;
	}

	private static void expect(Class<? extends Exception> type, CheckedAction action) throws Exception {
		try {
			action.run();
		} catch (Exception exception) {
			if (!type.isInstance(exception)) throw exception;
			checks++;
			return;
		}
		throw new AssertionError("Expected " + type.getSimpleName());
	}

	private interface CheckedAction {
		void run() throws Exception;
	}
}
