package forestry.api.apiculture.genetics;

import forestry.api.ForestryConstants;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum BeeLifeStage implements ILifeStage {
	DRONE(ForestryConstants.forestry("drone_bee")),
	PRINCESS(ForestryConstants.forestry("princess_bee")),
	QUEEN(ForestryConstants.forestry("queen_bee")),
	LARVAE(ForestryConstants.forestry("larvae_bee"));

	private final String name;
	// resolved on demand, not held: apiculture registers these items and builds them from
	// these same constants, so holding the item would be a class-init cycle
	private final Identifier itemId;

	BeeLifeStage(Identifier itemId) {
		this.name = name().toLowerCase(Locale.ENGLISH);
		this.itemId = itemId;
	}

	/**
	 * @return The registry id of this life stage's item form. Apiculture registers the item
	 * under this id, so the two cannot drift
	 */
	public Identifier itemId() {
		return this.itemId;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	@Override
	public Item getItemForm() {
		return BuiltInRegistries.ITEM.getValue(this.itemId);
	}
}
