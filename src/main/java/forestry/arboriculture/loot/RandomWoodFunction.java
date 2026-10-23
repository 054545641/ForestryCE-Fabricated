package forestry.arboriculture.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.IForestryApi;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.wood.ForestryWoodType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Replaces the stack with a random Forestry wood type's block of the given kind, keeping the count.
 * Used by the arborist's planks and logs trades.
 */
public class RandomWoodFunction extends LootItemConditionalFunction {
	public static final MapCodec<RandomWoodFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.and(StringRepresentable.fromEnum(WoodBlockKind::values).fieldOf("kind").forGetter(function -> function.kind))
		.apply(instance, RandomWoodFunction::new));

	private final WoodBlockKind kind;

	private RandomWoodFunction(List<LootItemCondition> conditions, WoodBlockKind kind) {
		super(conditions);
		this.kind = kind;
	}

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return ArboricultureLootFunctions.RANDOM_WOOD.get();
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		ForestryWoodType woodType = ForestryWoodType.getRandom(context.getRandom());
		ItemStack result = IForestryApi.INSTANCE.getTreeManager().getStack(woodType, this.kind, false);
		result.setCount(stack.getCount());
		return result;
	}
}
