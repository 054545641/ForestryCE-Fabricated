package forestry.core.platform.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Replaces the stack's item with a random item from the given tag, keeping the count and components.
 * Used by villager trades, ex. the beekeeper selling a random comb from {@code forestry:village_combs}.
 */
public class RandomItemFromTagFunction extends LootItemConditionalFunction {
	public static final MapCodec<RandomItemFromTagFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
		.and(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(function -> function.tag))
		.apply(instance, RandomItemFromTagFunction::new));

	private final TagKey<Item> tag;

	private RandomItemFromTagFunction(List<LootItemCondition> conditions, TagKey<Item> tag) {
		super(conditions);
		this.tag = tag;
	}

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return CoreLootFunctions.RANDOM_ITEM_FROM_TAG.get();
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		return context.getLevel().registryAccess().lookupOrThrow(Registries.ITEM)
			.get(this.tag)
			.flatMap(set -> set.getRandomElement(context.getRandom()))
			.map(holder -> stack.transmuteCopy(holder.value()))
			.orElse(stack);
	}
}
