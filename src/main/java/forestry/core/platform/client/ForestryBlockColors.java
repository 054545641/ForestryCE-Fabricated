package forestry.core.platform.client;

import forestry.core.platform.block.IColoredBlock;
import forestry.core.platform.tile.IRenderableTile;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public final class ForestryBlockColors {
    public static void initialize() {
        var tints = java.util.stream.IntStream.range(0, 3).mapToObj(index -> (BlockTintSource) new BlockTintSource() {
            @Override public int color(BlockState state) { return -1; }
            @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                if (state.getBlock() instanceof IColoredBlock colored) return 0xff000000 | colored.colorMultiplier(state, level, pos, index);
                if (level.getBlockEntity(pos) instanceof IRenderableTile tile) {
                    var volume = (index == 0 ? tile.getResourceTankInfo() : tile.getProductTankInfo()).getFluidVolume();
                    if (!volume.isEmpty()) return 0xff000000 | ClientsideCode.getFluidColor(volume.getFluid());
                }
                return -1;
            }
        }).toList();
        for (var block : BuiltInRegistries.BLOCK) {
            if (block instanceof IColoredBlock || block instanceof forestry.core.content.machines.blocks.BlockFactoryPlain) BlockColorRegistry.register(tints, block);
        }
        BlockColorRegistry.register(List.of(BlockTintSources.grass()),
            BuiltInRegistries.BLOCK.getValue(forestry.api.ForestryConstants.forestry("turf")),
            BuiltInRegistries.BLOCK.getValue(forestry.api.ForestryConstants.forestry("turf_block")));
    }
}
