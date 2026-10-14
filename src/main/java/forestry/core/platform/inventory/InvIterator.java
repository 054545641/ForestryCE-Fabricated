package forestry.core.platform.inventory;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class InvIterator implements Iterator<IInvSlot> {
	private final ResourceHandler<ItemResource> inv;
	private int slot = 0;

	public InvIterator(ResourceHandler<ItemResource> inv) {
		this.inv = inv;
	}

	@Override
	public boolean hasNext() {
		return this.slot < this.inv.size();
	}

	@Override
	public IInvSlot next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return new InvSlot(this.inv, this.slot++);
	}
}
