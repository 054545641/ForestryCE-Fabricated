package forestry.core.platform.recipes;

import com.mojang.serialization.MapCodec;
import forestry.api.ForestryConstants;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import java.util.stream.Stream;

public record ExactComponentIngredient(ItemStackTemplate stack) implements CustomIngredient {
    public static final MapCodec<ExactComponentIngredient> CODEC = ItemStackTemplate.CODEC.fieldOf("stack")
        .xmap(ExactComponentIngredient::new, ExactComponentIngredient::stack);
    public static final StreamCodec<RegistryFriendlyByteBuf, ExactComponentIngredient> STREAM_CODEC =
        ItemStackTemplate.STREAM_CODEC.map(ExactComponentIngredient::new, ExactComponentIngredient::stack);
    public static final CustomIngredientSerializer<ExactComponentIngredient> TYPE = new CustomIngredientSerializer<>() {
        @Override public Identifier getIdentifier() { return ForestryConstants.forestry("exact_components"); }
        @Override public MapCodec<ExactComponentIngredient> getCodec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ExactComponentIngredient> getStreamCodec() { return STREAM_CODEC; }
    };

    public static void initialize() { CustomIngredientSerializer.register(TYPE); }
    @Override public boolean test(ItemStack input) { return ItemStack.isSameItemSameComponents(input, this.stack.create()); }
    @Override public Stream<Holder<Item>> items() { return Stream.of(this.stack.item()); }
    @Override public boolean requiresTesting() { return true; }
    @Override public CustomIngredientSerializer<?> getSerializer() { return TYPE; }
    @Override public SlotDisplay display() { return new SlotDisplay.ItemStackSlotDisplay(this.stack); }
}
