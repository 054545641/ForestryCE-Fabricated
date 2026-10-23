package forestry.core.platform.network.packets;

import forestry.api.IForestryApi;
import forestry.api.core.ForestryEvent;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.engine.genetics.BreedingTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public record PacketGenomeTrackerSync(@Nullable CompoundTag nbt) implements CustomPacketPayload {
    public static final Type<PacketGenomeTrackerSync> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("genome_tracker_update"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGenomeTrackerSync msg) {
		buffer.writeNbt(msg.nbt);
	}

	public static PacketGenomeTrackerSync decode(RegistryFriendlyByteBuf buffer) {
		return new PacketGenomeTrackerSync(buffer.readNbt());
	}

	public static void handle(PacketGenomeTrackerSync msg, Player player) {
		if (msg.nbt != null) {
			Identifier type = Identifier.tryParse(msg.nbt.getStringOr(BreedingTracker.TYPE_KEY, ""));
			if (type == null) {
				return;
			}
			ISpeciesType<?, ?> root = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(type);

			if (root != null) {
				IBreedingTracker tracker = root.getBreedingTracker(player.level(), player.getGameProfile());
				tracker.load(msg.nbt);
				ForestryEvent.post(new ForestryEvent.SyncedBreedingTracker(tracker, player));
			}
		}
	}
}
