package forestry.api.core.circuits;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface ICircuit {
	String getId();

	String getTranslationKey();

	default Component getDisplayName() {
		return Component.translatable(getTranslationKey());
	}

	boolean isCircuitable(Object tile);

	void onInsertion(int slot, Object tile);

	void onLoad(int slot, Object tile);

	void onRemoval(int slot, Object tile);

	void onTick(int slot, Object tile);

	void addTooltip(List<Component> list);
}
