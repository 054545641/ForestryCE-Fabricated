package forestry.api.arboriculture;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum WoodBlockKind implements StringRepresentable {
	LOG, STRIPPED_LOG, STRIPPED_WOOD, WOOD, PLANKS, SLAB, FENCE, FENCE_GATE, STAIRS, DOOR, TRAPDOOR, SIGN, WALL_SIGN, HANGING_SIGN, WALL_HANGING_SIGN, BUTTON, PRESSURE_PLATE;

	public String getSerializedName() {
		return super.toString().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public String toString() {
		return getSerializedName();
	}
}
