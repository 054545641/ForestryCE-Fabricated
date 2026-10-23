package forestry.api.apiculture;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A serializer for a kind of {@link IFlowerType}. Mirrors {@code MutationConditionType}: the dispatch key
 * ("type" field in JSON) selects one of these, and its codecs (de)serialize the instance for datapacks and
 * network sync. Registered to {@code ForestryRegistries#FLOWER_TYPE_SERIALIZER}.
 */
public record FlowerTypeType<T extends IFlowerType>(
	MapCodec<T> codec,
	StreamCodec<RegistryFriendlyByteBuf, T> streamCodec
) {}
