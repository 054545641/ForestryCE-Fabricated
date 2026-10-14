package forestry.lepidopterology.butterflies;

import forestry.api.client.ForestrySprites;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.filter.FilterData;
import forestry.api.core.genetics.filter.IFilterRuleType;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public enum LepidopterologyFilterRuleType implements IFilterRuleType {
	FLUTTER(ForestrySprites.ANALYZER_FLUTTER) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return true;
		}
	},
	BUTTERFLY(ForestrySprites.ANALYZER_BUTTERFLY) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == ButterflyLifeStage.BUTTERFLY;
		}
	},
	SERUM(ForestrySprites.ANALYZER_SERUM) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == ButterflyLifeStage.SERUM;
		}
	},
	CATERPILLAR(ForestrySprites.ANALYZER_CATERPILLAR) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == ButterflyLifeStage.CATERPILLAR;
		}
	},
	COCOON(ForestrySprites.ANALYZER_COCOON) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == ButterflyLifeStage.COCOON;
		}
	};

	private final String id;
	private final Identifier sprite;

	LepidopterologyFilterRuleType(Identifier sprite) {
		this.sprite = sprite;
		this.id = "forestry.lepidopterology." + name().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public Identifier getSprite() {
		return this.sprite;
	}

	@Override
	public Identifier getSpeciesTypeId() {
		return ForestrySpeciesTypes.BUTTERFLY;
	}

	@Override
	public String getId() {
		return this.id;
	}
}
