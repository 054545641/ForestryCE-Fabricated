package forestry.api.client.arboriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;

import javax.annotation.Nullable;

/**
 * Responsible for tinting leaf colors according to their environment.
 * The default implementation for vanilla species is using the biome's foliage color, while Forestry species tint
 * based on the escritoire color of the species.
 *
 * @see forestry.api.client.plugin.IClientHelper For methods to create new leaf tints with common behavior.
 */
public interface ILeafTint {
	/**
	 * A default fallback tint.
	 */
	ILeafTint DEFAULT = (level, pos) -> FoliageColor.FOLIAGE_DEFAULT;

	int get(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos);
}
