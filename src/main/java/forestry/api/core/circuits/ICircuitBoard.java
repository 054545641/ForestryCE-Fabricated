package forestry.api.core.circuits;

import forestry.api.core.INbtWritable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.List;

public interface ICircuitBoard extends INbtWritable {
	void addTooltip(List<Component> list);

	void onInsertion(Object tile);

	void onLoad(Object tile);

	void onRemoval(Object tile);

	void onTick(Object tile);

	ICircuit[] getCircuits();

	/**
	 * Specifies where a circuit can be used.
	 */
	@Nullable
	Identifier getSocketType();
}
