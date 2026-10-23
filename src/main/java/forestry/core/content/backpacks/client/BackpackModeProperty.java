package forestry.core.content.backpacks.client;

import com.mojang.serialization.MapCodec;
import forestry.core.content.backpacks.items.BackpackItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record BackpackModeProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<BackpackModeProperty> CODEC = MapCodec.unit(new BackpackModeProperty());
    @Override public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return BackpackItem.getMode(stack).ordinal();
    }
    @Override public MapCodec<BackpackModeProperty> type() { return CODEC; }
}
