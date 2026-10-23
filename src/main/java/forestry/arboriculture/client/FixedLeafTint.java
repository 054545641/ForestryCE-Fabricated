package forestry.arboriculture.client;

import forestry.api.client.arboriculture.ILeafTint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextColor;
import net.minecraft.client.renderer.block.BlockAndTintGetter;

import javax.annotation.Nullable;
import java.awt.*;

public record FixedLeafTint(int color) implements ILeafTint {
	// TODO use for Azalea and Cherry trees
	public static final FixedLeafTint NONE = new FixedLeafTint(0xffffff);

	public FixedLeafTint(TextColor color) {
		this(color.getValue());
	}

	@Override
	public int get(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
		return this.color;
	}
}
