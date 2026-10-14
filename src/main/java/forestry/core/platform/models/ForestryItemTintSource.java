package forestry.core.platform.models;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import forestry.core.platform.item.ITintedItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ForestryItemTintSource(int tintIndex) implements ItemTintSource {
    public static final MapCodec<ForestryItemTintSource> CODEC = Codec.intRange(0, 255).fieldOf("tint_index")
        .xmap(ForestryItemTintSource::new, ForestryItemTintSource::tintIndex);
    @Override public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return stack.getItem() instanceof ITintedItem item ? 0xff000000 | item.getColorFromItemStack(stack, this.tintIndex) : -1;
    }
    @Override public MapCodec<ForestryItemTintSource> type() { return CODEC; }
}
