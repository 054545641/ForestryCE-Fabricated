package forestry.core.platform.network.packets;

import forestry.core.platform.particles.CoreParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public record PacketRefractoryWax(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PacketRefractoryWax> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("refractory_wax_on"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketRefractoryWax msg) {
		buffer.writeBlockPos(msg.pos);
	}

	public static PacketRefractoryWax decode(RegistryFriendlyByteBuf buffer) {
		return new PacketRefractoryWax(buffer.readBlockPos());
	}

	public static void handle(PacketRefractoryWax msg, Player player) {
		Level level = player.level();
		BlockPos pos = msg.pos;

		ParticleUtils.spawnParticlesOnBlockFaces(level, pos, CoreParticles.REFRACTORY_WAX.get(), UniformInt.of(3, 5));
		level.playLocalSound(pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1f, 1f, false);
	}
}
