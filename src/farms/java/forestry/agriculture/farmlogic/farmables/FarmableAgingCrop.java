package forestry.agriculture.farmlogic.farmables;

import com.google.common.base.Preconditions;
import forestry.api.agriculture.ICrop;
import forestry.api.agriculture.IFarmable;
import forestry.core.platform.util.BlockUtil;
import forestry.core.platform.util.ModUtil;
import forestry.agriculture.farmlogic.crops.CropDestroy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * For blocks that are harvestable once they are a certain age.
 */
public class FarmableAgingCrop implements IFarmable {
	protected final ItemStack germling;
	protected final Block cropBlock;
	protected final Property<Integer> ageProperty;
	protected final int minHarvestAge;
	@Nullable
	protected final Integer replantAge;
	protected final ItemStack[] products;

	public FarmableAgingCrop(Item germling, Block cropBlock, Property<Integer> ageProperty, int minHarvestAge) {
		this(germling, cropBlock, new ItemStack[0], ageProperty, minHarvestAge, null);
	}

	public FarmableAgingCrop(Item germling, Block cropBlock, Property<Integer> ageProperty, int minHarvestAge, @Nullable Integer replantAge) {
		this(germling, cropBlock, new ItemStack[0], ageProperty, minHarvestAge, replantAge);
	}

	public FarmableAgingCrop(Item germling, Block cropBlock, ItemStack product, Property<Integer> ageProperty, int minHarvestAge) {
		this(germling, cropBlock, new ItemStack[]{product}, ageProperty, minHarvestAge, null);
	}

	public FarmableAgingCrop(Item germling, Block cropBlock, ItemStack product, Property<Integer> ageProperty, int minHarvestAge, @Nullable Integer replantAge) {
		this(germling, cropBlock, new ItemStack[]{product}, ageProperty, minHarvestAge, replantAge);
	}

	public FarmableAgingCrop(Item germling, Block cropBlock, ItemStack[] products, Property<Integer> ageProperty, int minHarvestAge) {
		this(germling, cropBlock, products, ageProperty, minHarvestAge, null);
	}

	public FarmableAgingCrop(Item germling, Block cropBlock, ItemStack[] products, Property<Integer> ageProperty, int minHarvestAge, @Nullable Integer replantAge) {
		ModUtil.checkNotEmpty(germling);
		ModUtil.checkNotEmpty(cropBlock);
		Preconditions.checkNotNull(ageProperty);
		Preconditions.checkNotNull(products);

		this.germling = new ItemStack(germling);
		this.cropBlock = cropBlock;
		this.ageProperty = ageProperty;
		this.minHarvestAge = minHarvestAge;
		this.replantAge = replantAge;
		this.products = products;
	}

	@Override
	public boolean isSaplingAt(Level level, BlockPos pos, BlockState state) {
		return state.getBlock() == this.cropBlock && state.getValue(this.ageProperty) <= this.minHarvestAge;
	}

	@Override
	@Nullable
	public ICrop getCropAt(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() != this.cropBlock) {
			return null;
		}

		if (state.getValue(this.ageProperty) < this.minHarvestAge) {
			return null;
		}

		BlockState replantState = getReplantState(state);
		return new CropDestroy(level, state, pos, replantState, this.germling);
	}

	@Nullable
	protected BlockState getReplantState(BlockState blockState) {
		if (this.replantAge == null) {
			return null;
		}
		return blockState.setValue(this.ageProperty, this.replantAge);
	}

	@Override
	public boolean isGermling(ItemStack stack) {
		return ItemStack.isSameItem(this.germling, stack);
	}

	@Override
	public void addGermlings(Consumer<ItemStack> accumulator) {
		accumulator.accept(this.germling);
	}

	@Override
	public void addProducts(Consumer<ItemStack> accumulator) {
		for (ItemStack product : this.products) {
			accumulator.accept(product);
		}
	}

	@Override
	public boolean plantSaplingAt(Player player, ItemStack germling, Level level, BlockPos pos) {
		BlockState plantedState = this.cropBlock.defaultBlockState().setValue(this.ageProperty, 0);
		return BlockUtil.setBlockWithPlaceSound(level, pos, plantedState);
	}

	@Override
	public boolean isWindfall(ItemStack stack) {
		return false;
	}
}
