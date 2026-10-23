package forestry.core.platform.network.packets;

import forestry.api.IForestryApi;
import forestry.api.core.circuits.ICircuitLayout;
import forestry.core.engine.circuits.ContainerSolderingIron;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketGuiLayoutSelect(String layoutUid) implements CustomPacketPayload {
    public static final Type<PacketGuiLayoutSelect> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("gui_layout_select"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiLayoutSelect msg) {
		buffer.writeUtf(msg.layoutUid);
	}

	public static PacketGuiLayoutSelect decode(RegistryFriendlyByteBuf buffer) {
		return new PacketGuiLayoutSelect(buffer.readUtf());
	}

	public static void handle(PacketGuiLayoutSelect msg, Player player) {
		if (player.containerMenu instanceof ContainerSolderingIron solderingIron) {
			ICircuitLayout layout = IForestryApi.INSTANCE.getCircuitManager().getLayout(msg.layoutUid);

			if (layout != null) {
				solderingIron.setLayout(layout);
			}
		}
	}
}
