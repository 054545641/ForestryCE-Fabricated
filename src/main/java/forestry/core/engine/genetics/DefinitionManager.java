package forestry.core.engine.genetics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class DefinitionManager<T> extends SimpleJsonResourceReloadListener<JsonElement> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
		dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(), json -> new Dynamic<>(JsonOps.INSTANCE, json));
	private final String folder;
	private final Supplier<Codec<T>> codec;
	private final Consumer<Map<Identifier, T>> rebuild;
	private Map<Identifier, T> definitions = Map.of();

	protected DefinitionManager(String folder, Codec<T> codec, Consumer<Map<Identifier, T>> rebuild) {
		this(folder, () -> codec, rebuild);
	}

	protected DefinitionManager(String folder, Supplier<Codec<T>> codec, Consumer<Map<Identifier, T>> rebuild) {
		// parsing in apply keeps cross-definition references behind the reload dependency barrier
		super(JSON_CODEC, FileToIdConverter.json(folder));
		this.folder = folder;
		this.codec = codec;
		this.rebuild = rebuild;
	}

	public Map<Identifier, T> getDefinitions() {
		return this.definitions;
	}

	public void setDefinitions(Map<Identifier, T> definitions) {
		this.definitions = snapshot(definitions);
	}

	private static <T> Map<Identifier, T> snapshot(Map<Identifier, T> definitions) {
		Map<Identifier, T> ordered = new LinkedHashMap<>();
		definitions.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
		return Collections.unmodifiableMap(ordered);
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
		var ops = makeConditionalOps();
		Codec<T> codec = this.codec.get();
		Map<Identifier, T> parsed = new LinkedHashMap<>();
		object.forEach((id, json) -> codec.parse(ops, json)
			.ifError(error -> LOGGER.error("Skipping {} {}: {}", this.folder, id, error.message()))
			.result().ifPresent(definition -> parsed.put(id, definition)));

		Map<Identifier, T> next = snapshot(parsed);
		this.rebuild.accept(next);
		this.definitions = next;
	}
}
