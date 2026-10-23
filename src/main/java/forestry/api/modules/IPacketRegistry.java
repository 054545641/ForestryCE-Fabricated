package forestry.api.modules;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public interface IPacketRegistry {
	/**
	 * Register a packet during the play phase that is handled on the main server thread when the sender is not null.
	 */
	<P extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<P> type, StreamEncoder<RegistryFriendlyByteBuf, P> encoder, StreamDecoder<RegistryFriendlyByteBuf, P> decoder, BiConsumer<P, ServerPlayer> packetHandler);

	/**
	 * Register a packet during the play phase that is handled on the main render thread on the client.
	 */
	<P extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<P> type, StreamEncoder<RegistryFriendlyByteBuf, P> encoder, StreamDecoder<RegistryFriendlyByteBuf, P> decoder, BiConsumer<P, Player> packetHandler);
}
