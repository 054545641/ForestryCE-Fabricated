package forestry.arboriculture.network;

import forestry.core.platform.network.PacketIdServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet ids owned by the arboriculture module. The path strings are the packets' wire identity and
 * must never change.
 */
public class ArboriculturePacketIds {
	public static final CustomPacketPayload.Type<PacketRipeningUpdate> RIPENING_UPDATE = PacketIdServer.type("ripening_update");
	public static final CustomPacketPayload.Type<TreeSpeciesSyncPacket> TREE_SPECIES_SYNC = PacketIdServer.type("tree_species_sync");
}
