package forestry.apiculture.bees;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BlockWax extends Block {
	private final boolean meltable;

	// Adding this for compatibility reasons. Other bee mods might want meltable wax blocks yknow.
	// Deviation from 1.20.1: held as a Supplier so the block never resolves a deferred fluid while
	// the block registry is still being filled
	@Nullable
	private final Supplier<Fluid> meltingFluid;

	public BlockWax(Properties properties, boolean melts, @Nullable Supplier<Fluid> meltingFluid) {
		super(properties);
		this.meltable = melts;
		this.meltingFluid = meltingFluid;
	}

    public boolean melts() { return this.meltable; }
    public boolean melt(Level level, BlockPos pos) {
        return this.meltable && this.meltingFluid != null && level.setBlock(pos, this.meltingFluid.get().defaultFluidState().createLegacyBlock(), 3);
    }
}
