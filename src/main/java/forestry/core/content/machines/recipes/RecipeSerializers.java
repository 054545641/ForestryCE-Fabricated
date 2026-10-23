package forestry.core.content.machines.recipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RecipeSerializers {
	static <E> void write(FriendlyByteBuf buffer, List<E> list, BiConsumer<FriendlyByteBuf, E> consumer) {
		buffer.writeVarInt(list.size());

		for (E e : list) {
			consumer.accept(buffer, e);
		}
	}

	static <E> List<E> read(FriendlyByteBuf buffer, Function<FriendlyByteBuf, E> reader) {
		int size = buffer.readVarInt();

		ArrayList<E> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(reader.apply(buffer));
		}

		return list;
	}

	public static FluidStack deserializeFluid(JsonObject object, HolderLookup.Provider registries) {
		return FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), object).getOrThrow();
	}

	public static JsonObject serializeFluid(FluidStack fluid, HolderLookup.Provider registries) {
		return FluidStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), fluid).getOrThrow().getAsJsonObject();
	}

	public static ItemStack item(JsonObject object, HolderLookup.Provider registries) {
		return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), object).getOrThrow();
	}

	public static JsonObject item(ItemStack stack, HolderLookup.Provider registries) {
		return ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow().getAsJsonObject();
	}

	public static Optional<Ingredient> deserialize(JsonElement resource, HolderLookup.Provider registries) {
		if (resource.isJsonArray() && resource.getAsJsonArray().isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(Ingredient.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), resource).getOrThrow());
	}
}
