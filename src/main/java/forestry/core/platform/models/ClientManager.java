package forestry.core.platform.models;

import forestry.core.platform.block.IColoredBlock;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.platform.tile.IRenderableTile;
import forestry.core.platform.util.ModUtil;
import forestry.core.platform.util.RenderUtil;
import forestry.core.platform.util.ResourceUtil;
import forestry.core.platform.registration.FeatureBlock;
import forestry.core.platform.registration.FeatureGroup;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureTable;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public enum ClientManager {
	INSTANCE;

	public static final java.util.List<BlockTintSource> FORESTRY_BLOCK_COLOR = java.util.stream.IntStream.range(0, 3)
        .mapToObj(index -> (BlockTintSource) new BlockTintSource() {
            @Override public int color(BlockState state) { return -1; }
            @Override public int colorInWorld(BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
                return 0xff000000 | blockColor(state, level, pos, index);
            }
        }).toList();

    private static int blockColor(BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, int tintIndex) {
		Block block = state.getBlock();
		if (level == null || pos == null) {
			return 0xffffff;
		}
		if (block instanceof IColoredBlock coloredBlock) {
			return coloredBlock.colorMultiplier(state, level, pos, tintIndex);
		}

		// Packaging machine tank slices. tintIndex 0 is the resource tank, tintIndex 1 the product tank
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof IRenderableTile tile) {
			TankRenderInfo tankInfo = tintIndex == 0 ? tile.getResourceTankInfo() : tile.getProductTankInfo();
			FluidStack fluidStack = tankInfo.getFluidStack();
			if (!fluidStack.isEmpty()) {
				return RenderUtil.getFluidColor(fluidStack.getFluid());
			}
			return 0x000000;
		}

		return 0xffffff;
	}

    private final Map<Block, BlockModelEntry> customBlockModels = new LinkedHashMap<>();
    private final Map<Identifier, ItemModel> customItems = new LinkedHashMap<>();

    public void registerModel(ModelBlockDefault<?, ?> model, Object feature) {
        if (feature instanceof FeatureGroup<?, ?, ?> group) group.getFeatures().forEach(f -> registerModel(model, f));
        else if (feature instanceof FeatureTable<?, ?, ?, ?> group) group.getFeatures().forEach(f -> registerModel(model, f));
        else if (feature instanceof FeatureBlock<?, ?> block) registerModel(model, block.block(), block.item());
        else if (feature instanceof FeatureItem<?> item) registerModel((ItemModel) model, item.item());
        else throw new IllegalArgumentException("Unsupported model feature " + feature);
    }
    public void registerModel(ModelBlockDefault<?, ?> model, Block block, @Nullable BlockItem item) {
        registerModel(model, block, item, block.getStateDefinition().getPossibleStates());
    }
    public void registerModel(ModelBlockDefault<?, ?> model, Block block, @Nullable BlockItem item, Collection<BlockState> states) {
        this.customBlockModels.put(block, new BlockModelEntry(model, item, java.util.List.copyOf(states)));
    }
    public void registerModel(ItemModel model, Item item) { this.customItems.put(ModUtil.getRegistryName(item), model); }
    public void onBakeModels(ModelEvent.ModifyBakingResult event) {
        var result = event.getBakingResult();
        for (BlockModelEntry entry : this.customBlockModels.values()) {
            entry.model.setSpriteGetter(event.getTextureGetter());
            for (BlockState state : entry.states) result.blockStateModels().put(state, entry.model.forState(state));
            if (entry.item != null) result.itemStackModels().put(ModUtil.getRegistryName(entry.item), entry.model);
        }
        ModelBlockCached.clear();
        result.itemStackModels().putAll(this.customItems);
    }
    private record BlockModelEntry(ModelBlockDefault<?, ?> model, @Nullable BlockItem item, Collection<BlockState> states) {}
}
