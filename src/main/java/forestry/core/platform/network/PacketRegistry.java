package forestry.core.platform.network;

import forestry.api.modules.IPacketRegistry;
import forestry.core.platform.util.NetworkUtil;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.*;
import java.util.function.BiConsumer;

/** Common payload registration. Client receiver installation is in the explicit client package. */
public final class PacketRegistry implements IPacketRegistry {
    public static final PacketRegistry INSTANCE = new PacketRegistry();
    private final List<ClientRegistration<?>> clientHandlers = new ArrayList<>();
    public record ClientRegistration<P extends CustomPacketPayload>(CustomPacketPayload.Type<P> type, BiConsumer<P, Player> handler) {}
    public List<ClientRegistration<?>> clientHandlers() { return List.copyOf(clientHandlers); }

    @Override public <P extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<P> type,
            StreamEncoder<RegistryFriendlyByteBuf, P> encoder, StreamDecoder<RegistryFriendlyByteBuf, P> decoder, BiConsumer<P, ServerPlayer> handler) {
        PayloadTypeRegistry.serverboundPlay().register(type, bounded(encoder, decoder, 65_536));
        // Fabric invokes play handlers on the server thread. Business handlers still validate access to their target.
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(payload, context.player()));
    }
    @Override public <P extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<P> type,
            StreamEncoder<RegistryFriendlyByteBuf, P> encoder, StreamDecoder<RegistryFriendlyByteBuf, P> decoder, BiConsumer<P, Player> handler) {
        PayloadTypeRegistry.clientboundPlay().register(type, bounded(encoder, decoder, NetworkUtil.MAX_PAYLOAD_BYTES));
        clientHandlers.add(new ClientRegistration<>(type, handler));
    }
    private static <P> StreamCodec<RegistryFriendlyByteBuf, P> bounded(StreamEncoder<RegistryFriendlyByteBuf, P> encoder,
            StreamDecoder<RegistryFriendlyByteBuf, P> decoder, int maximum) {
        return StreamCodec.of((buffer, value) -> {
            int start = buffer.writerIndex();
            encoder.encode(buffer, value);
            NetworkUtil.boundedLength(buffer.writerIndex() - start, maximum);
        }, buffer -> {
            NetworkUtil.boundedLength(buffer.readableBytes(), maximum);
            return decoder.decode(buffer);
        });
    }
}
