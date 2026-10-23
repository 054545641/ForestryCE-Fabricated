package forestry.apiculture.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nonnull;

public class BeeParticleType extends ParticleType<BeeParticleData> {
	private final MapCodec<BeeParticleData> codec;
	private final StreamCodec<RegistryFriendlyByteBuf, BeeParticleData> streamCodec = StreamCodec.of(
		(buffer, data) -> {
			buffer.writeBlockPos(data.destination());
			buffer.writeInt(data.color());
		},
		buffer -> new BeeParticleData(this, buffer.readBlockPos(), buffer.readInt())
	);

	public BeeParticleType() {
		super(false);
		this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
			net.minecraft.core.BlockPos.CODEC.fieldOf("destination").forGetter(BeeParticleData::destination),
			Codec.INT.fieldOf("color").forGetter(BeeParticleData::color)
		).apply(instance, (destination, color) -> new BeeParticleData(this, destination, color)));
	}

	@Nonnull
	@Override
	public MapCodec<BeeParticleData> codec() {
		return this.codec;
	}

	@Override
	public StreamCodec<? super RegistryFriendlyByteBuf, BeeParticleData> streamCodec() {
		return this.streamCodec;
	}
}
