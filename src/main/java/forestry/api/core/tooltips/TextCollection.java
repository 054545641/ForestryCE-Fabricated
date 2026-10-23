package forestry.api.core.tooltips;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextCollection implements ITextInstance<TextCollection, TextCompound, TextCollection> {
	private final List<Component> lines = new ArrayList<>();
	@Nullable
	private Component last;

	@Override
	public TextCompound singleLine() {
		return new TextCompound(this);
	}

	@Override
	public TextCollection create() {
		return this;
	}

	@Override
	public boolean isEmpty() {
		return this.lines.isEmpty();
	}

	@Nullable
	@Override
	public Component lastComponent() {
		return this.last;
	}

	@Override
	public TextCollection add(Component line) {
        this.lines.add(line);
        this.last = line;
		return this;
	}

	public TextCollection addAll(@Nullable TextCollection lines) {
		if (lines == null) {
			return this;
		}
		addAll(lines.getLines());
		return this;
	}

	@Override
	public TextCollection cast() {
		return this;
	}

	public void clear() {
        this.lines.clear();
	}

	public List<Component> getLines() {
		return Collections.unmodifiableList(this.lines);
	}
}
