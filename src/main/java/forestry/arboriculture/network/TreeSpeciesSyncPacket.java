package forestry.arboriculture.network;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import forestry.arboriculture.trees.genetics.ArboricultureReloadHandler;
import forestry.arboriculture.trees.genetics.TreeSpeciesDefinition;
import forestry.arboriculture.trees.genetics.TreeSpeciesManager;

public record TreeSpeciesSyncPacket(Map<Identifier, TreeSpeciesDefinition> definitions) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ArboriculturePacketIds.TREE_SPECIES_SYNC;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, TreeSpeciesSyncPacket msg) {
		definitionsStreamCodec().encode(buffer, msg.definitions);
	}

	public static TreeSpeciesSyncPacket decode(RegistryFriendlyByteBuf buffer) {
		return new TreeSpeciesSyncPacket(definitionsStreamCodec().decode(buffer));
	}

	public static void handle(TreeSpeciesSyncPacket msg, Player player) {
		// integrated servers already populated these shared singletons during reload
		if (Minecraft.getInstance().hasSingleplayerServer()) {
			return;
		}
		TreeSpeciesManager.INSTANCE.setDefinitions(msg.definitions);
		ArboricultureReloadHandler.rebuildTreeSpecies(msg.definitions);
		forestry.core.ModuleCore.refreshDataRegistrations();
	}

	private static StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, TreeSpeciesDefinition>> definitionsStreamCodec() {
		return ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, TreeSpeciesDefinition.streamCodec());
	}
}
