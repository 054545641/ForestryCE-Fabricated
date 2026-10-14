package forestry.mail.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import forestry.core.platform.item.ITintedItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Reads the stamp's two colours through the item definition tint pipeline. */
public record StampTintSource(int tintIndex) implements ItemTintSource {
	public static final MapCodec<StampTintSource> CODEC = Codec.intRange(0, 1).fieldOf("tint_index")
		.xmap(StampTintSource::new, StampTintSource::tintIndex);

	@Override
	public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
		return stack.getItem() instanceof ITintedItem item
			? 0xff000000 | item.getColorFromItemStack(stack, this.tintIndex) : -1;
	}

	@Override
	public MapCodec<StampTintSource> type() {
		return CODEC;
	}
}
