package forestry.core.platform.block;

import forestry.api.core.IBlockSubtype;

public interface IBlockType extends IBlockSubtype {
	IMachineProperties<?> getMachineProperties();
}
