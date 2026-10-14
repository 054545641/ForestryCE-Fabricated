package forestry.arboriculture.trees;

import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.client.ForestrySprites;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.filter.FilterData;
import forestry.api.core.genetics.filter.IFilterRuleType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public enum ArboricultureFilterRuleType implements IFilterRuleType {
	TREE(ForestrySprites.ANALYZER_TREE) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return true;
		}
	},
	SAPLING(ForestrySprites.ANALYZER_SAPLING) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == TreeLifeStage.SAPLING;
		}
	},
	POLLEN(ForestrySprites.ANALYZER_POLLEN) {
		@Override
		public boolean isValid(ItemStack stack, FilterData data) {
			return data.stage() == TreeLifeStage.POLLEN;
		}
	};

	private final String id;
	private final Identifier sprite;

	ArboricultureFilterRuleType(Identifier sprite) {
		this.sprite = sprite;
		this.id = "forestry.arboriculture." + name().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public Identifier getSprite() {
		return this.sprite;
	}

	@Override
	public Identifier getSpeciesTypeId() {
		return ForestrySpeciesTypes.TREE;
	}

	@Override
	public String getId() {
		return this.id;
	}
}
