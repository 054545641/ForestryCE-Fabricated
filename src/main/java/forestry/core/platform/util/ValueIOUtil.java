package forestry.core.platform.util;

import com.mojang.serialization.MapCodec;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.function.Consumer;

public final class ValueIOUtil {
	private ValueIOUtil() {
	}

	public static CompoundTag toTag(ValueInput input) {
		return input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).map(CompoundTag::copy).orElseGet(CompoundTag::new);
	}

	public static void mergeInto(ValueOutput output, CompoundTag tag) {
		output.store(MapCodec.assumeMapUnsafe(CompoundTag.CODEC), tag.copy());
	}

	public static void readTag(CompoundTag tag, HolderLookup.Provider registries, Consumer<ValueInput> reader) {
		try (var reporter = new ProblemReporter.ScopedCollector(LogUtils.getLogger())) {
			reader.accept(TagValueInput.create(reporter, registries, tag));
		}
	}
}
