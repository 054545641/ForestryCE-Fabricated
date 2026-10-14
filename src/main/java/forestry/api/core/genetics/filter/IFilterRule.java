package forestry.api.core.genetics.filter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IFilterRule {
	boolean isValid(ItemStack stack, FilterData data);

	/**
	 * If a species type with this uid is registered, the filter will only get stack with individuals from this root.
	 */
	@Nullable
	default Identifier getSpeciesTypeId() {
		return null;
	}
}
