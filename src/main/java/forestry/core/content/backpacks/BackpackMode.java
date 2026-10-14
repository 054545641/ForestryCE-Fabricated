package forestry.core.content.backpacks;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Locale;

public enum BackpackMode implements StringRepresentable {
	NEUTRAL(null),
	LOCKED("for.storage.backpack.mode.locked"),
	RECEIVE("for.storage.backpack.mode.receiving"),
	RESUPPLY("for.storage.backpack.mode.resupply");

	public static final BackpackMode[] VALUES = values();
	public static final Codec<BackpackMode> CODEC = StringRepresentable.fromEnum(BackpackMode::values);

	public static DataComponentType<BackpackMode> componentType() {
		return DataComponentType.<BackpackMode>builder().persistent(CODEC)
			.networkSynchronized(ByteBufCodecs.fromCodec(CODEC)).build();
	}

	public static BackpackMode read(ItemStack stack, DataComponentType<BackpackMode> component) {
		BackpackMode mode = stack.get(component);
		if (mode != null) return mode;
		// Legacy backpacks stored mode in DAMAGE despite having no durability.
		int legacy = stack.getOrDefault(DataComponents.DAMAGE, 0);
		return VALUES[Math.clamp(legacy, 0, VALUES.length - 1)];
	}

	public void write(ItemStack stack, DataComponentType<BackpackMode> component) {
		stack.set(component, this);
		stack.remove(DataComponents.DAMAGE);
	}

	public BackpackMode next(boolean resupplyEnabled) {
		int next = ordinal() + 1;
		if (!resupplyEnabled && next == RESUPPLY.ordinal()) next++;
		return VALUES[next % VALUES.length];
	}

	@Nullable
	private final String translationKey;

	BackpackMode(@Nullable String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}

	@Nullable
	public String getTranslationKey() {
		return this.translationKey;
	}
}
