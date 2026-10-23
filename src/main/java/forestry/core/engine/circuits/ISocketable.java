package forestry.core.engine.circuits;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public interface ISocketable {
	int getSocketCount();

	ItemStack getSocket(int slot);

	void setSocket(int slot, ItemStack stack);

	Identifier getSocketType();
}
