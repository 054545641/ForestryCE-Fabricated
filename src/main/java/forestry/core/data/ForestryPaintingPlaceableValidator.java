package forestry.core.data;

import com.google.gson.JsonParser;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/** Checks the actual resource packs, including jar resources, rather than the datagen classloader. */
public final class ForestryPaintingPlaceableValidator implements DataProvider {
	private final ResourceManager resources;
	public ForestryPaintingPlaceableValidator(ResourceManager resources) { this.resources = resources; }

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		Set<String> variants = new TreeSet<>();
		this.resources.listResources("painting_variant", id -> id.getNamespace().equals("forestry") && id.getPath().endsWith(".json"))
			.keySet().forEach(id -> variants.add(id.withPath(id.getPath().substring("painting_variant/".length(), id.getPath().length() - 5)).toString()));
		if (variants.isEmpty()) throw new IllegalStateException("No Forestry painting variants in datagen resource packs");
		Set<String> tagged = new TreeSet<>();
		for (var resource : this.resources.getResourceStack(Identifier.withDefaultNamespace("tags/painting_variant/placeable.json"))) {
			try (var reader = resource.openAsReader()) {
				var json = JsonParser.parseReader(reader).getAsJsonObject();
				if (json.has("replace") && json.get("replace").getAsBoolean()) tagged.clear();
				json.getAsJsonArray("values").forEach(value -> {
					String id = value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("id").getAsString();
					if (id.startsWith("forestry:")) tagged.add(id);
				});
			} catch (IOException e) { throw new UncheckedIOException(e); }
		}
		Set<String> missing = new TreeSet<>(variants); missing.removeAll(tagged);
		Set<String> orphaned = new TreeSet<>(tagged); orphaned.removeAll(variants);
		if (!missing.isEmpty() || !orphaned.isEmpty()) throw new IllegalStateException("Painting/placeable tag drift: missing=" + missing + ", orphaned=" + orphaned);
		return CompletableFuture.completedFuture(null);
	}

	@Override public String getName() { return "Forestry Painting placeable tag validator"; }
}
