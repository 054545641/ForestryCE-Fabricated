package forestry.core.platform.fluids;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

public class DrainOnlyFluidHandlerWrapper extends DelegatingResourceHandler<FluidResource> {
	public DrainOnlyFluidHandlerWrapper(ResourceHandler<FluidResource> handler) {
		super(handler);
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, size());
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return 0;
	}

	@Override
	public int insert(FluidResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return 0;
	}
}
