package forestry.core.platform.network.packets;

import forestry.core.platform.gui.ContainerTile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketGuiEnergy(int windowId, long value) implements CustomPacketPayload {
    public static final Type<PacketGuiEnergy> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("gui_energy"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiEnergy msg) {
		buffer.writeVarInt(msg.windowId);
		buffer.writeVarLong(msg.value);
	}

	public static PacketGuiEnergy decode(RegistryFriendlyByteBuf buffer) {
		return new PacketGuiEnergy(buffer.readVarInt(), buffer.readVarLong());
	}

	public static void handle(PacketGuiEnergy msg, Player player) {
		if (player.containerMenu.containerId == msg.windowId && player.containerMenu instanceof ContainerTile<?> menu) {
			menu.onGuiEnergy(msg.value);
		}
	}
}
