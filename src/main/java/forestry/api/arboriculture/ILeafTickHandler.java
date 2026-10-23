package forestry.api.arboriculture;

import forestry.api.arboriculture.genetics.ITree;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public interface ILeafTickHandler {
	boolean onRandomLeafTick(ITree tree, Level world, RandomSource rand, BlockPos pos, boolean isDestroyed);
}
