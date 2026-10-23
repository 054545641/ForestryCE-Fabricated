package forestry.core.data.models;

import com.google.gson.JsonObject;

/** Rotations of a model in a blockstate variant. */
public record ModelVariant(ModelAsset model, int x, int y, boolean uvLock) {
	public JsonObject json() {
		JsonObject result = new JsonObject();
		result.addProperty("model", this.model.getLocation().toString());
		if (this.x != 0) result.addProperty("x", this.x);
		if (this.y != 0) result.addProperty("y", this.y);
		if (this.uvLock) result.addProperty("uvlock", true);
		return result;
	}
	public static Builder builder() { return new Builder(); }
	public static ModelVariant[] allYRotations(ModelAsset model, int x, boolean uvLock) {
		return new ModelVariant[]{new ModelVariant(model, x, 0, uvLock), new ModelVariant(model, x, 90, uvLock), new ModelVariant(model, x, 180, uvLock), new ModelVariant(model, x, 270, uvLock)};
	}
	public static class Builder {
		private ModelAsset model;
		private int x, y;
		public Builder modelFile(ModelAsset model) { this.model = model; return this; }
		public Builder rotationX(int x) { this.x = x; return this; }
		public Builder rotationY(int y) { this.y = y; return this; }
		public ModelVariant[] build() { return new ModelVariant[]{new ModelVariant(java.util.Objects.requireNonNull(this.model), this.x, this.y, false)}; }
	}
}
