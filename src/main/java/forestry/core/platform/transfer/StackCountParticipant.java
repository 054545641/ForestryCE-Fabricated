package forestry.core.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

/** Journals the external stack consumed by a pickup/stow operation along with its destination. */
public final class StackCountParticipant extends SnapshotParticipant<Integer> {
    private final ItemStack stack;
    public StackCountParticipant(ItemStack stack) { this.stack = stack; }
    @Override protected Integer createSnapshot() { return stack.getCount(); }
    @Override protected void readSnapshot(Integer count) { stack.setCount(count); }
}
