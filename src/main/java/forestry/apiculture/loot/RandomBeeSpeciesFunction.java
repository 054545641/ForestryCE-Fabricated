package forestry.apiculture.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.IForestryApi;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Replaces the stack with a bee of a random species from the given list, keeping the life stage of the
 * template item. Used by the beekeeper's drone trade, which sells a random mundane hive bee.
 */
public class RandomBeeSpeciesFunction extends LootItemConditionalFunction {
	public static final MapCodec<RandomBeeSpeciesFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.and(Identifier.CODEC.listOf().fieldOf("species").forGetter(function -> function.speciesIds))
		.apply(instance, RandomBeeSpeciesFunction::new));

	private final List<Identifier> speciesIds;

	private RandomBeeSpeciesFunction(List<LootItemCondition> conditions, List<Identifier> speciesIds) {
		super(conditions);
		this.speciesIds = speciesIds;
	}

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return ApicultureLootFunctions.RANDOM_BEE_SPECIES.get();
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		if (this.speciesIds.isEmpty()) {
			return stack;
		}
		ISpeciesType<?, ?> speciesType = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BEE);
		ILifeStage stage = speciesType.getLifeStage(stack);
		if (stage == null) {
			return stack;
		}
		Identifier speciesId = this.speciesIds.get(context.getRandom().nextInt(this.speciesIds.size()));
		ItemStack result = SpeciesUtil.getBeeSpecies(speciesId).createStack(stage);
		result.setCount(stack.getCount());
		return result;
	}
}
