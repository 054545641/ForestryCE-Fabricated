package forestry.arboriculture.models;

import forestry.arboriculture.leaves.DefaultFruitLeavesBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

public class DefaultFruitLeavesModel extends DecorativeLeavesModel<DefaultFruitLeavesBlock> {
	public DefaultFruitLeavesModel() {
		super(DefaultFruitLeavesBlock.class);
	}

	@Override
	protected DefaultLeavesModel.Key getInventoryKey(ItemStack stack) {
		Block block = Block.byItem(stack.getItem());
		return new DefaultLeavesModel.Key(((DefaultFruitLeavesBlock) block).getSpeciesId(), Minecraft.getInstance().options.cutoutLeaves().get());
	}

	@Override
	protected DefaultLeavesModel.Key getWorldKey(BlockState state, ModelData extraData) {
		Block block = state.getBlock();
		return new DefaultLeavesModel.Key(((DefaultFruitLeavesBlock) block).getSpeciesId(), Minecraft.getInstance().options.cutoutLeaves().get());
	}

}
