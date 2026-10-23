package forestry.core.platform.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Schedules a side effect only if its nested scope and the outer transaction both commit. */
public final class CommitAction extends CommitParticipant<Boolean> {
    private final Runnable action;
    private boolean pending;
    public CommitAction(Runnable action) { this.action = java.util.Objects.requireNonNull(action); }
    @Override public void updateSnapshots(TransactionContext transaction) { super.updateSnapshots(transaction); pending = true; }
    @Override protected Boolean createSnapshot() { return pending; }
    @Override protected void readSnapshot(Boolean value) { pending = value; }
    @Override protected void onCommitted(Boolean previous) { boolean run = pending; pending = false; if (run) action.run(); }
}
