package forestry.arboriculture.leaves;

import forestry.api.arboriculture.genetics.ITree;
import forestry.api.core.genetics.ISpecies;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.util.NBTUtilForestry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.FoliageColor;

public class LeavesBlockItem extends ItemBlockForestry<AbstractLeavesBlock> implements ITintedItem {
	public LeavesBlockItem(AbstractLeavesBlock block, Item.Properties properties) {
		super(block, properties);
	}


	@Override
	public Component getName(ItemStack itemstack) {
		CompoundTag tag = NBTUtilForestry.getItemStackTag(itemstack);
		if (tag == null) {
			return Component.translatable("trees.grammar.leaves.type");
		}

		LeavesBlockEntity tileLeaves = new LeavesBlockEntity(BlockPos.ZERO, getBlock().defaultBlockState());
		forestry.core.platform.util.ValueIOUtil.readTag(tag, net.minecraft.core.RegistryAccess.EMPTY, tileLeaves::loadCustomOnly);

		ITree tree = tileLeaves.getTree();
		if (tree == null) {
			return Component.translatable("for.leaves.corrupted");
		}
		return getDisplayName(tree.getSpecies());
	}

	public static Component getDisplayName(ISpecies<?> species) {
		Component leaves = Component.translatable("for.trees.grammar.leaves.type");
		return Component.translatable("for.trees.grammar.leaves", species.getDisplayName(), leaves);
	}

	@Override
	public int getColorFromItemStack(ItemStack itemStack, int renderPass) {
		CompoundTag tag = NBTUtilForestry.getItemStackTag(itemStack);
		if (tag == null) {
			return FoliageColor.FOLIAGE_DEFAULT;
		}

		LeavesBlockEntity tileLeaves = new LeavesBlockEntity(BlockPos.ZERO, getBlock().defaultBlockState());
		forestry.core.platform.util.ValueIOUtil.readTag(tag, net.minecraft.core.RegistryAccess.EMPTY, tileLeaves::loadCustomOnly);

		if (renderPass == AbstractLeavesBlock.FRUIT_COLOR_INDEX) {
			return tileLeaves.getFruitColour();
		} else {

			return tileLeaves.getFoliageColour();
		}
	}
}
