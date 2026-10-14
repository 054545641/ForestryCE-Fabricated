package forestry.api.lepidopterology.genetics;

import forestry.api.ForestryConstants;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum ButterflyLifeStage implements ILifeStage {
	BUTTERFLY(ForestryConstants.forestry("butterfly")),
	SERUM(ForestryConstants.forestry("butterfly_serum")),
	CATERPILLAR(ForestryConstants.forestry("caterpillar")),
	COCOON(ForestryConstants.forestry("cocoon"));

	private final String name;
	// resolved on demand, not held: lepidopterology registers these items and builds them from
	// these same constants, so holding the item would be a class-init cycle
	private final Identifier itemId;

	ButterflyLifeStage(Identifier itemId) {
		this.name = name().toLowerCase(Locale.ENGLISH);
		this.itemId = itemId;
	}

	/**
	 * @return The registry id of this life stage's item form. Lepidopterology registers the item
	 * under this id, so the two cannot drift
	 */
	public Identifier itemId() {
		return this.itemId;
	}

	public String getSerializedName() {
		return this.name;
	}

	@Override
	public Item getItemForm() {
		return BuiltInRegistries.ITEM.getValue(this.itemId);
	}
}
