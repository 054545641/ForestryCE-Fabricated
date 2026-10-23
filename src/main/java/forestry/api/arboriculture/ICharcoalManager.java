package forestry.api.arboriculture;

import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Provides functions that are related to the forestry charcoal pile.
 *
 * <p>Wall registration is performed through
 * {@link forestry.api.plugin.IArboricultureRegistration#registerCharcoalPitWall} during plugin
 * setup. Runtime queries (lookup-by-state and the full wall list) live here.
 */
@Deprecated
public interface ICharcoalManager {
	@Nullable
	ICharcoalPileWall getWall(BlockState state);

	/**
	 * @return A collection with all registered charcoal pile walls.
	 */
	List<ICharcoalPileWall> getWalls();
}
