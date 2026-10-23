package forestry.core.platform.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.IForestryApi;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Loot function to add genetic information, an organism, to the item stack.
 */
public class OrganismFunction extends LootItemConditionalFunction {
	public static final MapCodec<OrganismFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.and(instance.group(
			Identifier.CODEC.fieldOf("type_id").forGetter(function -> function.typeId),
			Identifier.CODEC.fieldOf("species_id").forGetter(function -> function.speciesId)
		))
		.apply(instance, OrganismFunction::new));

	private final Identifier typeId;
	private final Identifier speciesId;

	private OrganismFunction(List<LootItemCondition> conditions, Identifier typeId, Identifier speciesId) {
		super(conditions);
		this.typeId = typeId;
		this.speciesId = speciesId;
	}

	public static LootItemConditionalFunction.Builder<?> fromDefinition(ISpeciesType<?, ?> type, ISpecies<?> species) {
		return fromId(type.id(), species.id());
	}

	public static LootItemConditionalFunction.Builder<?> fromId(Identifier typeId, Identifier speciesId) {
		return simpleBuilder(conditions -> new OrganismFunction(conditions, typeId, speciesId));
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext lootContext) {
		ISpeciesType<?, ?> speciesType = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(this.typeId);
		ILifeStage stage = speciesType.getLifeStage(stack);

		if (stage != null) {
			ISpecies<?> species = speciesType.getSpecies(this.speciesId);
			return species.createStack(stage);
		}

		return stack;
	}

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return CoreLootFunctions.ORGANISM.get();
	}
}
