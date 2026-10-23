package forestry.agriculture.farmlogic.crops;

import forestry.api.core.genetics.IFruitBearer;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class CropFruit extends Crop {
	public CropFruit(Level world, BlockPos position) {
		super(world, position);
	}

	@Override
	protected boolean isCrop(Level world, BlockPos pos) {
		IFruitBearer bearer = TileUtil.getTile(world, pos, IFruitBearer.class);
		return bearer != null && bearer.hasFruit() && bearer.getRipeness() >= 1.0f;
	}

	@Override
	protected List<ItemStack> harvestBlock(Level level, BlockPos pos) {
		IFruitBearer tile = TileUtil.getTile(level, pos, IFruitBearer.class);
		if (tile == null) {
			return NonNullList.create();
		}

		BlockUtil.sendDestroyEffects(level, pos, level.getBlockState(pos));
		return tile.pickFruit(ItemStack.EMPTY);
	}
}
