package forestry.apiculture.network.packets;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import forestry.apiculture.bees.genetics.ApicultureReloadHandler;
import forestry.apiculture.bees.genetics.BeeSpeciesDefinition;
import forestry.apiculture.bees.genetics.BeeSpeciesManager;
public record BeeSpeciesSyncPacket(Map<Identifier, BeeSpeciesDefinition> definitions) implements CustomPacketPayload {
    public static final Type<BeeSpeciesSyncPacket> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("bee_species_sync"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, BeeSpeciesSyncPacket msg) {
		definitionsStreamCodec().encode(buffer, msg.definitions);
	}

	public static BeeSpeciesSyncPacket decode(RegistryFriendlyByteBuf buffer) {
		return new BeeSpeciesSyncPacket(definitionsStreamCodec().decode(buffer));
	}

	public static void handle(BeeSpeciesSyncPacket msg, Player player) {
		// integrated servers already populated these shared singletons during reload
		if (Minecraft.getInstance().hasSingleplayerServer()) {
			return;
		}
		BeeSpeciesManager.INSTANCE.setDefinitions(msg.definitions);
		ApicultureReloadHandler.rebuildSpecies(msg.definitions);
	}

	private static StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, BeeSpeciesDefinition>> definitionsStreamCodec() {
		return ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, BeeSpeciesDefinition.streamCodec());
	}
}
