package forestry.core.platform.registration;

import forestry.api.core.IMenuTypeProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface IMenuTypeFeature<C extends AbstractContainerMenu> extends IMenuTypeProvider<C>, IModFeature {
}
