package forestry.core.platform.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps inventory and additional drops in the native, server-only removal lifecycle. */
public abstract class DropAwareBlockEntity extends BlockEntity {
	protected DropAwareBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public final void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (this.level instanceof ServerLevel server) {
			// BlockEntity already drains Containers. Do not drop the inventory a second time.
			super.preRemoveSideEffects(pos, state);
			dropAdditionalContents(server);
		}
	}

	protected abstract void dropAdditionalContents(ServerLevel level);
}
