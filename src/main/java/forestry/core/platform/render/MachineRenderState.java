package forestry.core.platform.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public final class MachineRenderState extends BlockEntityRenderState {
	public MachineGeometry.Pose pose = new MachineGeometry.Pose(Direction.SOUTH, 0, 0);
}
