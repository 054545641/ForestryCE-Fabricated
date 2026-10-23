package forestry.apiculture.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketHabitatBiomePointer(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PacketHabitatBiomePointer> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("habitat_biome_pointer"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketHabitatBiomePointer msg) {
		buffer.writeBlockPos(msg.pos);
	}

	public static PacketHabitatBiomePointer decode(RegistryFriendlyByteBuf buffer) {
		return new PacketHabitatBiomePointer(buffer.readBlockPos());
	}

	public static void handle(PacketHabitatBiomePointer msg, Player player) {
		BlockPos pos = msg.pos();
		//TextureHabitatLocator.getInstance().setTargetCoordinates(pos);//TODO: TextureHabitatLocator
	}
}
