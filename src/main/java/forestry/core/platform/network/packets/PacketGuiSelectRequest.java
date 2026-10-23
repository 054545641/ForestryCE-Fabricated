package forestry.core.platform.network.packets;

import forestry.core.platform.gui.IGuiSelectable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record PacketGuiSelectRequest(int containerId, int primaryIndex, int secondaryIndex) implements CustomPacketPayload {
    public PacketGuiSelectRequest(int primaryIndex, int secondaryIndex) {
        this(forestry.core.platform.client.ClientsideCode.currentContainerId(), primaryIndex, secondaryIndex);
    }
    public static final Type<PacketGuiSelectRequest> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("gui_selection_request"));
	public static void handle(PacketGuiSelectRequest msg, ServerPlayer player) {
		AbstractContainerMenu container = player.containerMenu;

		if (container.containerId == msg.containerId && container.stillValid(player) && container instanceof IGuiSelectable guiSelectable) {
			guiSelectable.handleSelectionRequest(player, msg.primaryIndex(), msg.secondaryIndex());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiSelectRequest msg) {
		buffer.writeVarInt(msg.containerId);
		buffer.writeVarInt(msg.primaryIndex);
		buffer.writeVarInt(msg.secondaryIndex);
	}

	public static PacketGuiSelectRequest decode(RegistryFriendlyByteBuf buffer) {
		return new PacketGuiSelectRequest(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
	}
}
