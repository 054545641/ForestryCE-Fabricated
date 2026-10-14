package forestry.lepidopterology.network;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import forestry.lepidopterology.butterflies.genetics.ButterflySpeciesDefinition;
import forestry.lepidopterology.butterflies.genetics.ButterflySpeciesManager;
import forestry.lepidopterology.butterflies.genetics.LepidopterologyReloadHandler;

public record ButterflySpeciesSyncPacket(Map<Identifier, ButterflySpeciesDefinition> definitions) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return LepidopterologyPacketIds.BUTTERFLY_SPECIES_SYNC;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, ButterflySpeciesSyncPacket msg) {
		definitionsStreamCodec().encode(buffer, msg.definitions);
	}

	public static ButterflySpeciesSyncPacket decode(RegistryFriendlyByteBuf buffer) {
		return new ButterflySpeciesSyncPacket(definitionsStreamCodec().decode(buffer));
	}

	public static void handle(ButterflySpeciesSyncPacket msg, Player player) {
		// integrated servers already populated these shared singletons during reload
		if (Minecraft.getInstance().hasSingleplayerServer()) {
			return;
		}
		ButterflySpeciesManager.INSTANCE.setDefinitions(msg.definitions);
		LepidopterologyReloadHandler.rebuildButterflySpecies(msg.definitions);
	}

	private static StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, ButterflySpeciesDefinition>> definitionsStreamCodec() {
		return ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ButterflySpeciesDefinition.streamCodec());
	}
}
