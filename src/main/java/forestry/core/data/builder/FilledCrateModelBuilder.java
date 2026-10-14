package forestry.core.data.builder;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.generators.template.CustomLoaderBuilder;

import javax.annotation.Nullable;

public class FilledCrateModelBuilder extends CustomLoaderBuilder {
	private static final Identifier ID = Identifier.parse("forestry:filled_crate");

	@Nullable
	private Identifier layer1;
	@Nullable
	private Identifier layer2;

	public FilledCrateModelBuilder() {
		super(ID, false);
	}

	@Override
	protected CustomLoaderBuilder copyInternal() {
		FilledCrateModelBuilder copy = new FilledCrateModelBuilder();
		copy.layer1 = this.layer1;
		copy.layer2 = this.layer2;
		return copy;
	}

	public FilledCrateModelBuilder layer1(Identifier texture) {
		this.layer1 = texture;
		return this;
	}

	public FilledCrateModelBuilder layer2(Identifier texture) {
		this.layer2 = texture;
		return this;
	}

	// doing it this way skips the NPEs from regular textures, helpful for debugging
	@Override
	public JsonObject toJson(JsonObject json) {
		json = super.toJson(json);
		JsonObject textures = new JsonObject();
		textures.addProperty("layer1", this.layer1.toString());
		if (this.layer2 != null) {
			textures.addProperty("layer2", this.layer2.toString());
		}
		json.add("textures", textures);
		return json;
	}
}
