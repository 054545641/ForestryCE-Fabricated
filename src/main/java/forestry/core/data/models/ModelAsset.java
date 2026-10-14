package forestry.core.data.models;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

/** A named model JSON under assets/<namespace>/models, independent of item definitions. */
public class ModelAsset {
	private final Identifier id;
	public final JsonObject json = new JsonObject();
	public ModelAsset(Identifier id) { this.id = id; }
	public ModelAsset(String id) { this(Identifier.parse(id)); }
	public Identifier getLocation() { return this.id; }
	public ModelAsset parent(ModelAsset parent) { return parent(parent.getLocation()); }
	public ModelAsset parent(Identifier parent) { this.json.addProperty("parent", parent.toString()); return this; }
	public ModelAsset texture(String name, Identifier texture) {
		if (!this.json.has("textures")) this.json.add("textures", new JsonObject());
		this.json.getAsJsonObject("textures").addProperty(name, texture.toString());
		return this;
	}
	public ModelAsset renderType(String type) { this.json.addProperty("render_type", type); return this; }
}
