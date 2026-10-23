package forestry.apiculture.bees;

import forestry.core.platform.item.TwoTintItem;

//TODO - create common superclass for items/blocks defined by an enum.
//Will help with automatic creation of stuff too.
public class PropolisItem extends TwoTintItem {
	public PropolisItem(EnumPropolis type, Properties properties) {
		super(type, properties);
	}
}
