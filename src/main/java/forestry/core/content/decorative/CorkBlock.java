package forestry.core.content.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block of cork, which is springy enough to take most of the fall damage out of a landing.
 * <p>
 * Deviation from 1.20.1: that tree carried four constructors, three of which built the properties
 * themselves or took a flammability and a spread speed. The registry owns the properties here, and
 * BlockBurnable no longer takes a rate, so the one below stands in for all four.
 */
public class CorkBlock extends BlockBurnable {
	public CorkBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
		entity.causeFallDamage(distance, 0.2f, level.damageSources().fall());
	}
}
