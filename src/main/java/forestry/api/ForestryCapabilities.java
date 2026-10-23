package forestry.api;

import forestry.api.apiculture.IBeeProtection;
import forestry.api.core.ISpectacleVision;
import forestry.api.core.genetics.filter.IFilterLogic;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;

import static forestry.api.ForestryConstants.forestry;

/**
 * All capabilities added by base Forestry.
 */
public class ForestryCapabilities {
	/**
	 * Items with this capability can protect the wearer from harmful bee effects.
	 */
	public static final ItemApiLookup<IBeeProtection, Void> BEE_PROTECTION = ItemApiLookup.get(forestry("bee_protection"), IBeeProtection.class, Void.class);

	/**
	 * Grants the wearer the ability to see wild bee hives and pollinated leaves more easily.
	 */
	public static final ItemApiLookup<ISpectacleVision, Void> SPECTACLE_VISION = ItemApiLookup.get(forestry("spectacle_vision"), ISpectacleVision.class, Void.class);

	/**
	 * Genetic filters expose their configurable sorting logic through this block capability.
	 */
	public static final BlockApiLookup<IFilterLogic, Void> FILTER_LOGIC = BlockApiLookup.get(forestry("filter_logic"), IFilterLogic.class, Void.class);
}
