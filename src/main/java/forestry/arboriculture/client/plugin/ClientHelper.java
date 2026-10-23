package forestry.arboriculture.client.plugin;

import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.client.arboriculture.ILeafTint;
import forestry.api.client.plugin.IClientHelper;
import forestry.arboriculture.client.BiomeLeafTint;
import forestry.arboriculture.client.FixedLeafTint;
import forestry.arboriculture.models.LeafSprite;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.block.BlockAndTintGetter;

import javax.annotation.Nullable;
import java.awt.*;

public class ClientHelper implements IClientHelper {
	@Override
	public ILeafTint createNoneTint() {
		return FixedLeafTint.NONE;
	}

	@Override
	public ILeafTint createFixedTint(TextColor color) {
		return new FixedLeafTint(color);
	}

	@Override
	public ILeafTint createBiomeTint() {
		return BiomeLeafTint.DEFAULT;
	}

	@Override
	public ILeafTint createBiomeTint(Int2IntFunction mapper) {
		return new BiomeLeafTint() {
			@Override
			public int get(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
				return mapper.applyAsInt(super.get(level, pos));
			}
		};
	}

	@Override
	public ILeafSprite createLeafSprite(Identifier id) {
		return LeafSprite.create(id);
	}
}
