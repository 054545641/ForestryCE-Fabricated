package forestry.apiculture.bees;

import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.client.ForestrySprites;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.filter.FilterData;
import forestry.api.core.genetics.filter.IFilterRuleType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public enum ApicultureFilterRuleType implements IFilterRuleType {
	BEE(ForestrySprites.ANALYZER_BEE) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return true;
		}
	},
	DRONE(ForestrySprites.ANALYZER_DRONE) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == BeeLifeStage.DRONE;
		}
	},
	PRINCESS(ForestrySprites.ANALYZER_PRINCESS) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == BeeLifeStage.PRINCESS;
		}
	},
	QUEEN(ForestrySprites.ANALYZER_QUEEN) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == BeeLifeStage.QUEEN;
		}
	};

	private final String id;
	private final Identifier sprite;

	ApicultureFilterRuleType(Identifier sprite) {
		this.id = "forestry.apiculture." + name().toLowerCase(Locale.ENGLISH);
		this.sprite = sprite;
	}

	@Override
	public Identifier getSprite() {
		return this.sprite;
	}

	@Override
	public Identifier getSpeciesTypeId() {
		return ForestrySpeciesTypes.BEE;
	}

	@Override
	public String getId() {
		return this.id;
	}
}
