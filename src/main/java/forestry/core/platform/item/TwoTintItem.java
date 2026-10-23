package forestry.core.platform.item;

import forestry.api.core.IItemSubtype;
import net.minecraft.world.item.ItemStack;

/**
 * Base class for items with an overlay color and multiple layer models.
 *
 * @see forestry.mail.letters.ItemStamp
 */
public class TwoTintItem extends ItemForestry implements ITintedItem {
	// Variant of subtype that has primary/secondary color fields
	public interface ITwoTintItemSubtype extends IItemSubtype {
		int primaryColor();

		int secondaryColor();
	}

	protected final ITwoTintItemSubtype type;

	public TwoTintItem(ITwoTintItemSubtype type, Properties properties) {
		super(properties);

		this.type = type;
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		if (tintIndex == 0 || this.type.secondaryColor() == 0) {
			return this.type.primaryColor();
		} else {
			return this.type.secondaryColor();
		}
	}
}
