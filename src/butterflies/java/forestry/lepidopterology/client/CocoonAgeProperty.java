package forestry.lepidopterology.client;

import com.mojang.serialization.MapCodec;
import forestry.lepidopterology.butterflies.ItemButterflyGE;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record CocoonAgeProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<CocoonAgeProperty> CODEC = MapCodec.unit(new CocoonAgeProperty());
    @Override public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) { return ItemButterflyGE.getAge(stack); }
    @Override public MapCodec<CocoonAgeProperty> type() { return CODEC; }
}
