package forestry.api.core.machines;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Forestry's ingredient/count recipe value, with the existing nested JSON shape. */
public record CountedIngredient(Ingredient ingredient, int count) {
    public CountedIngredient {
        java.util.Objects.requireNonNull(ingredient);
        if (count <= 0) throw new IllegalArgumentException("Non-positive ingredient count");
    }
    public boolean test(ItemStack stack) { return ingredient.test(stack) && stack.getCount() >= count; }
    public static final Codec<CountedIngredient> NESTED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
        ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(CountedIngredient::count)
    ).apply(instance, CountedIngredient::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, CountedIngredient::ingredient, ByteBufCodecs.VAR_INT, CountedIngredient::count, CountedIngredient::new);
}
