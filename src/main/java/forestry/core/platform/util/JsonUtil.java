package forestry.core.platform.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import forestry.Forestry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.util.JsonUtils;

public class JsonUtil {
	public static ItemStack deserializeItemStack(JsonObject object, ItemStack fallback) {
		return deserializeItemStack(object, fallback, false);
	}

	public static ItemStack deserializeItemStack(JsonObject object, ItemStack fallback, boolean logError) {
		if (!object.has("item")) {
			if (logError) {
				Forestry.LOGGER.error("Unsupported icon type, currently only items are supported (add 'item' key)");
			}
			return fallback;
		}
		try {
			Holder<net.minecraft.world.item.Item> item = GsonHelper.getAsItem(object, "item");
			int count = GsonHelper.getAsInt(object, "count", 1);
			ItemStack stack = new ItemStack(item, count);
			if (object.has("nbt")) {
				stack.set(DataComponents.CUSTOM_DATA, CustomData.of(JsonUtils.readNBT(object, "nbt")));
			}
			return stack;
		} catch (JsonSyntaxException e) {
			if (logError) {
				Forestry.LOGGER.trace("Filed to parse item.", e);
			}
			return fallback;
		}
	}

	public static <T> T deserialize(Codec<T> codec, JsonElement json) {
		return codec.decode(JsonOps.INSTANCE, json).result().get().getFirst();
	}

	public static <T> JsonElement serialize(Codec<T> codec, T object) {
		return codec.encodeStart(JsonOps.INSTANCE, object).result().get();
	}
}
