package forestry.core.platform.fluids;

import forestry.core.platform.registration.FeatureFluid;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.*;

/** Native finite fluid, retaining the previous BaseFlowingFluid flow defaults (4 / 1 / 5). */
public abstract class ForestryFlowingFluid extends FlowingFluid {
    private final FeatureFluid feature;
    protected ForestryFlowingFluid(FeatureFluid feature) { this.feature = feature; }
    public forestry.core.platform.registration.FluidProperties forestryProperties() { return feature.properties(); }
    @Override public Fluid getSource() { return feature.fluid(); }
    @Override public Fluid getFlowing() { return feature.flowing(); }
    @Override public Item getBucket() { return feature.properties().bucket.get(); }
    @Override protected boolean canConvertToSource(ServerLevel level) { return false; }
    @Override protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
    }
    @Override protected int getSlopeFindDistance(LevelReader level) { return 4; }
    @Override protected int getDropOff(LevelReader level) { return 1; }
    @Override public int getTickDelay(LevelReader level) { return 5; }
    @Override protected float getExplosionResistance() { return 1; }
    @Override public BlockState createLegacyBlock(FluidState state) {
        return feature.fluidBlock().block().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }
    @Override public boolean isSame(Fluid fluid) { return fluid == getSource() || fluid == getFlowing(); }
    @Override protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction side) {
        return side == Direction.DOWN && !isSame(fluid);
    }
    public static final class Source extends ForestryFlowingFluid {
        public Source(FeatureFluid feature) { super(feature); }
        @Override public boolean isSource(FluidState state) { return true; }
        @Override public int getAmount(FluidState state) { return 8; }
    }
    public static final class Flowing extends ForestryFlowingFluid {
        public Flowing(FeatureFluid feature) { super(feature); }
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
        @Override public boolean isSource(FluidState state) { return false; }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
    }
}
