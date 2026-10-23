package forestry.api.core.climate;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Provides information about the biome an object is based in.
 */
public interface IBiomeProvider {
	/**
	 * @return The biome the object that implements this interface is located in.
	 */
	Holder<Biome> getBiome();
}
