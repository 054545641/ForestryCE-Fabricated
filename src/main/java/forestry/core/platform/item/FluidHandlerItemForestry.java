package forestry.core.platform.item;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.features.CoreDataComponents;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
/** Disposable, whole-bucket containers. The item and fluid share the caller's transaction. */
public final class FluidHandlerItemForestry extends SingleVariantItemStorage<FluidVariant> {
    private final FluidContainerType type;
    private final ContainerItemContext context;
    public FluidHandlerItemForestry(ContainerItemContext context, FluidContainerType type) { super(context); this.type = type; this.context = context; }
    private static FluidVolume contents(ItemVariant item) { return item.getComponents().getOrDefault(CoreDataComponents.FLUID_CONTENT.get(), FluidVolume.EMPTY); }
    @Override protected FluidVariant getBlankResource() { return FluidVariant.blank(); }
    @Override protected FluidVariant getResource(ItemVariant item) { return contents(item).variant(); }
    @Override protected long getAmount(ItemVariant item) { return contents(item).amount(); }
    @Override protected long getCapacity(FluidVariant fluid) { return FluidConstants.BUCKET; }
    @Override protected boolean canInsert(FluidVariant fluid) { return type != FluidContainerType.WAX_CAPSULE || FluidVariantAttributes.getTemperature(fluid) < 310.15; }
    @Override public long insert(FluidVariant fluid, long amount, TransactionContext tx) {
        StoragePreconditions.notBlankNotNegative(fluid, amount);
        return amount < FluidConstants.BUCKET || getAmount() != 0 ? 0 : super.insert(fluid, FluidConstants.BUCKET, tx);
    }
    @Override public long extract(FluidVariant fluid, long amount, TransactionContext tx) {
        StoragePreconditions.notBlankNotNegative(fluid, amount);
        long available = getAmount();
        if (available == 0 || amount < available || !getResource().equals(fluid)) return 0;
        return context.extract(context.getItemVariant(), 1, tx) == 1 ? available : 0;
    }
    @Override protected ItemVariant getUpdatedVariant(ItemVariant item, FluidVariant fluid, long amount) {
        if (amount == 0) throw new IllegalStateException("Disposable containers must be consumed through their context");
        var stack = item.toStack(); stack.set(CoreDataComponents.FLUID_CONTENT.get(), new FluidVolume(fluid, amount)); return ItemVariant.of(stack);
    }
}
