package forestry.core.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/** Fabric owns rollback; this retains the original state solely for one final change notification. */
public abstract class CommitParticipant<T> extends SnapshotParticipant<T> {
    private TransactionContext observedRoot;

    @Override public void updateSnapshots(TransactionContext transaction) {
        TransactionContext root = transaction.getOpenTransaction(0);
        if (observedRoot != root) {
            T original = createSnapshot();
            observedRoot = root;
            root.addOuterCloseCallback(result -> {
                observedRoot = null;
                if (result.wasCommitted()) onCommitted(original);
            });
        }
        super.updateSnapshots(transaction);
    }

    protected abstract void onCommitted(T original);
}
