package forestry.api.agriculture;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public interface IFarmingManager {
	/**
	 * Used to check whether the farms jar is installed. Base ships a no-op farming manager, so this
	 * returns {@code false} rather than the getter returning null or throwing. Every other api manager
	 * comes from base and is always real.
	 *
	 * @return Whether a real implementation is installed
	 * @since 2.10.0
	 */
	default boolean isLoaded() {
		return true;
	}

	default List<IFarmable> getFarmables(Identifier farmTypeId) {
		IFarmType farmType = getFarmType(farmTypeId);
		return farmType == null ? List.of() : farmType.getFarmables();
	}

	/**
	 * @return The value of the fertilizer when used in a farm.
	 */
	int getFertilizeValue(ItemStack stack);

	@Nullable
	IFarmType getFarmType(Identifier id);
}
