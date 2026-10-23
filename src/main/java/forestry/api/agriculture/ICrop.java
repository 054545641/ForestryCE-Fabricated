package forestry.api.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public interface ICrop {
	/**
	 * Harvests this crop. Performs the necessary manipulations to set the crop into a "harvested" state.
	 *
	 * @return Products harvested. Null if this crop cannot be harvested
	 */
	@Nullable
	List<ItemStack> harvest();

	BlockPos getPosition();
}
