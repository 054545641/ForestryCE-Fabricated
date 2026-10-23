package forestry.lepidopterology.network;

import forestry.core.platform.network.PacketIdServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet ids owned by the lepidopterology module. The path strings are the packets' wire identity and
 * must never change.
 */
public class LepidopterologyPacketIds {
	public static final CustomPacketPayload.Type<ButterflySpeciesSyncPacket> BUTTERFLY_SPECIES_SYNC = PacketIdServer.type("butterfly_species_sync");
}
