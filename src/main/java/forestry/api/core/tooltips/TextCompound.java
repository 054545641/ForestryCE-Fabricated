package forestry.api.core.tooltips;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nullable;

/**
 * Helper class to allow simple appending of siblings to a text collection.
 */
public class TextCompound implements ITextInstance<TextCompound, TextCompound, TextCollection> {
	private final TextCollection parent;
	@Nullable
	private MutableComponent root;

	public TextCompound(TextCollection parent) {
		this.parent = parent;
	}

	@Nullable
	@Override
	public Component lastComponent() {
		return this.root;
	}

	@Override
	public TextCompound add(Component line) {
		if (this.root == null) {
			if (!(line instanceof MutableComponent)) {
				return this;
			}
            this.root = (MutableComponent) line;
			return this;
		}
        this.root.append(line);
		return this;
	}

	@Override
	public TextCompound singleLine() {
		return this;
	}

	@Override
	public TextCompound cast() {
		return this;
	}

	@Override
	public TextCollection create() {
		if (this.root != null) {
            this.parent.add(this.root);
		}
		return this.parent;
	}

	@Override
	public boolean isEmpty() {
		return this.root == null;
	}
}
