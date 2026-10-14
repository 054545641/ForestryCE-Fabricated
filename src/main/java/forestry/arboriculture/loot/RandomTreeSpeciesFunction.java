package forestry.arboriculture.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the stack with a random registered tree species at the given life stage, restricted to species
 * whose complexity does not exceed {@code max_complexity}. Used by the arborist's sapling and pollen trades.
 */
public class RandomTreeSpeciesFunction extends LootItemConditionalFunction {
	public static final MapCodec<RandomTreeSpeciesFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.and(instance.group(
			StringRepresentable.fromEnum(TreeLifeStage::values).fieldOf("stage").forGetter(function -> function.stage),
			Codec.INT.fieldOf("max_complexity").forGetter(function -> function.maxComplexity)
		))
		.apply(instance, RandomTreeSpeciesFunction::new));

	private final TreeLifeStage stage;
	private final int maxComplexity;

	private RandomTreeSpeciesFunction(List<LootItemCondition> conditions, TreeLifeStage stage, int maxComplexity) {
		super(conditions);
		this.stage = stage;
		this.maxComplexity = maxComplexity;
	}

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return ArboricultureLootFunctions.RANDOM_TREE_SPECIES.get();
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		List<ITreeSpecies> potentialSpecies = new ArrayList<>();
		for (ITreeSpecies species : SpeciesUtil.getAllTreeSpecies()) {
			if (species.getComplexity() <= this.maxComplexity) {
				potentialSpecies.add(species);
			}
		}
		if (potentialSpecies.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ITreeSpecies chosenSpecies = potentialSpecies.get(context.getRandom().nextInt(potentialSpecies.size()));
		ItemStack result = chosenSpecies.createStack(this.stage);
		result.setCount(stack.getCount());
		return result;
	}
}
