package forestry.core.content.sorting.gui;

import forestry.core.platform.gui.GuiForestry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public interface ISelectableProvider<S> {
	Collection<S> getEntries();

	void onSelect(S selectable);

	void draw(GuiForestry<?> gui, S selectable, GuiGraphicsExtractor graphics, int y, int x);

	Component getName(S selectable);
}
