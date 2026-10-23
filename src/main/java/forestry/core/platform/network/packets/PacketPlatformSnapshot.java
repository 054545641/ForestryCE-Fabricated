package forestry.core.platform.network.packets;

import com.google.gson.*;
import forestry.api.ForestryConstants;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.data.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/** A complete, versioned platform snapshot. Decode and validate everything before replacing client state. */
public record PacketPlatformSnapshot(long revision, JsonObject maps, JsonObject config, JsonObject genetics) implements CustomPacketPayload {
    public static final Type<PacketPlatformSnapshot> TYPE = new Type<>(ForestryConstants.forestry("platform_snapshot"));
    private static final int MAX_JSON_CHARS = 262_000;
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketPlatformSnapshot packet) {
        buffer.writeVarInt(1);
        buffer.writeVarLong(packet.revision());
        JsonObject data = new JsonObject();
        data.add("maps", packet.maps());
        data.add("config", packet.config());
        data.add("genetics", packet.genetics());
        buffer.writeUtf(data.toString(), MAX_JSON_CHARS);
    }
    public static PacketPlatformSnapshot decode(RegistryFriendlyByteBuf buffer) {
        if (buffer.readVarInt() != 1) throw new IllegalArgumentException("Unsupported Forestry snapshot protocol");
        long revision = buffer.readVarLong();
        if (revision <= 0) throw new IllegalArgumentException("Invalid Forestry snapshot revision");
        JsonObject data = JsonParser.parseString(buffer.readUtf(MAX_JSON_CHARS)).getAsJsonObject();
        if (data.size() != 3) throw new IllegalArgumentException("Invalid Forestry snapshot fields");
        return new PacketPlatformSnapshot(revision, data.getAsJsonObject("maps"), data.getAsJsonObject("config"), data.getAsJsonObject("genetics"));
    }
    public static void handle(PacketPlatformSnapshot packet, Player player) {
        if (packet.revision() <= ForestryDataMapRuntime.clientSnapshot().revision()) return;
        var snapshot = DataMapSync.decode(packet.maps(), packet.revision(), player.registryAccess());
        var genetics = GeneticDataLoader.decode(packet.genetics(), player.registryAccess());
        ForestryConfig.validateServerSnapshot(packet.config());
        GeneticDataLoader.apply(genetics);
        ForestryConfig.applyServerSnapshot(packet.config());
        ForestryDataMapRuntime.replaceClient(snapshot);
    }
}
