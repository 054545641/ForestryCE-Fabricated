package forestry.arboriculture.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.arboriculture.charcoal.AshBlock;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Set;

public class CountBlockFunction extends LootItemConditionalFunction {
	public static final MapCodec<CountBlockFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.apply(instance, CountBlockFunction::new));

	protected CountBlockFunction(List<LootItemCondition> conditions) {
		super(conditions);
	}

	public static LootItemConditionalFunction.Builder<?> builder() {
		return simpleBuilder(CountBlockFunction::new);
	}

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return ArboricultureLootFunctions.COUNT.get();
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
		if (state == null || !state.hasProperty(AshBlock.AMOUNT)) {
			return stack;
		}
		int amount = state.getValue(AshBlock.AMOUNT);
		stack.setCount(amount);
		return stack;
	}

	@Override
	public Set<ContextKey<?>> getReferencedContextParams() {
		return ImmutableSet.of(LootContextParams.BLOCK_STATE);
	}
}
