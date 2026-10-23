package forestry.core.platform.fluids;

import forestry.api.core.fluids.FluidVolume;
import forestry.core.features.CoreDataComponents;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;

/** The real held/container item is exchanged inside the caller's transaction. */
public final class PipetteFluidStorage extends SingleVariantItemStorage<FluidVariant> {
    public PipetteFluidStorage(ContainerItemContext context) { super(context); }
    private static FluidVolume contents(ItemVariant item) { return item.getComponents().getOrDefault(CoreDataComponents.FLUID_CONTENT.get(), FluidVolume.EMPTY); }
    @Override protected FluidVariant getBlankResource() { return FluidVariant.blank(); }
    @Override protected FluidVariant getResource(ItemVariant item) { return contents(item).variant(); }
    @Override protected long getAmount(ItemVariant item) { return contents(item).amount(); }
    @Override protected long getCapacity(FluidVariant fluid) { return FluidConstants.BUCKET; }
    @Override protected ItemVariant getUpdatedVariant(ItemVariant item, FluidVariant fluid, long amount) {
        var stack = item.toStack();
        if (amount == 0) stack.remove(CoreDataComponents.FLUID_CONTENT.get());
        else stack.set(CoreDataComponents.FLUID_CONTENT.get(), new FluidVolume(fluid, amount));
        return ItemVariant.of(stack);
    }
}
