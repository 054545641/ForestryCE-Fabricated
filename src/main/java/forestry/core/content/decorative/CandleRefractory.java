package forestry.core.content.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A vanilla-shaped candle that burns with soul fire.
 */
public class CandleRefractory extends CandleBlock {
	public CandleRefractory(BlockBehaviour.Properties properties) {
		// Deviation from 1.20.1: the properties and the light level were built in this constructor there.
		// The registry owns them here, as it does for every other block in this tree
		super(properties);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT)) {
			this.getParticleOffsets(state).forEach(v -> addParticlesAndSound(level, v.add(pos.getX(), pos.getY(), pos.getZ()), random));
		}
	}

	// Lifted from the vanilla candle code
	private static void addParticlesAndSound(Level level, Vec3 offset, RandomSource random) {
		float f = random.nextFloat();
		if (f < 0.3F) {
			level.addParticle(ParticleTypes.SMOKE, offset.x, offset.y, offset.z, 0.0, 0.0, 0.0);
			if (f < 0.17F) {
				level.playLocalSound(offset.x + 0.5, offset.y + 0.5, offset.z + 0.5, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
			}
		}
		// There is no small soul fire flame, so these burn with a full-size one
		level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, offset.x, offset.y, offset.z, 0.0, 0.0, 0.0);
	}
}
