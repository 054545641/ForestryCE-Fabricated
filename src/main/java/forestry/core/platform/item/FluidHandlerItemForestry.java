package forestry.core.platform.item;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;

public class FluidHandlerItemForestry extends ItemAccessFluidHandler {
	private final FluidContainerType containerType;

	public FluidHandlerItemForestry(ItemAccess access, DataComponentType<SimpleFluidContent> component, FluidContainerType containerType) {
		super(access, component, FluidType.BUCKET_VOLUME);
		this.containerType = containerType;
	}

	@Override
	public boolean isValid(int index, FluidResource resource) {
		return super.isValid(index, resource) && (this.containerType != FluidContainerType.WAX_CAPSULE
			|| resource.getFluidType().getTemperature(resource.toStack(FluidType.BUCKET_VOLUME)) < 310.15);
	}

	@Override
	@Nullable
	protected ItemResource update(ItemResource container, int index, FluidResource resource, int amount) {
		if (amount == 0) {
			// ItemAccess consumes the capsule in the same transaction as its contents.
			return null;
		}
		if (amount != FluidType.BUCKET_VOLUME) {
			return ItemResource.EMPTY;
		}
		return super.update(container, index, resource, amount);
	}
}
