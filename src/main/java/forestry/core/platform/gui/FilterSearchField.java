package forestry.core.platform.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Text entry owns inventory/hotbar keys while focused; escape and tab retain normal screen behavior. */
public final class FilterSearchField extends EditBox {
	public FilterSearchField(Font font, int x, int y, int width, int height) {
		super(font, x, y, width, height, Component.empty());
		setMaxLength(50);
		setBordered(false);
		setTextColor(0xFFFFFFFF);
	}
	@Override public boolean keyPressed(KeyEvent event) {
		if (super.keyPressed(event)) return true;
		return isFocused() && isVisible() && event.key() != GLFW.GLFW_KEY_ESCAPE && event.key() != GLFW.GLFW_KEY_TAB;
	}
}
