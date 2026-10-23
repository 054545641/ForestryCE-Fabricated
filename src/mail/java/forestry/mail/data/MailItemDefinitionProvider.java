package forestry.mail.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** The item definitions that select the mail jar's existing baked models. */
public final class MailItemDefinitionProvider implements DataProvider {
	private final PackOutput.PathProvider paths;
	private final Map<Identifier, Boolean> items;

	public MailItemDefinitionProvider(PackOutput output, Map<Identifier, Boolean> items) {
		this.paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
		this.items = Map.copyOf(items);
	}

	public static JsonObject definition(Identifier item, boolean stamp) {
		JsonObject model = new JsonObject();
		model.addProperty("type", "minecraft:model");
		model.addProperty("model", item.withPrefix("item/").toString());
		if (stamp) {
			JsonArray tints = new JsonArray();
			for (int layer = 0; layer < 2; layer++) {
				JsonObject tint = new JsonObject();
				tint.addProperty("type", "forestry:mail_stamp");
				tint.addProperty("tint_index", layer);
				tints.add(tint);
			}
			model.add("tints", tints);
		}
		JsonObject definition = new JsonObject();
		definition.add("model", model);
		return definition;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return CompletableFuture.allOf(this.items.entrySet().stream().map(entry ->
			DataProvider.saveStable(cache, definition(entry.getKey(), entry.getValue()), this.paths.json(entry.getKey()))
		).toArray(CompletableFuture[]::new));
	}

	@Override
	public String getName() {
		return "Forestry mail item definitions";
	}
}
