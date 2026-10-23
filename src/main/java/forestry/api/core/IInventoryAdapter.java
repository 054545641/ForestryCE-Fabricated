package forestry.api.core;

import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.storage.ValueOutput;

public interface IInventoryAdapter extends WorldlyContainer, ITransactionalContainer, IFilterSlotDelegate, INbtWritable, INbtReadable {
	/**
	 * Writes inventory data using the output's registry context.
	 *
	 * @param output The destination for inventory data
	 */
	void write(ValueOutput output);
}
