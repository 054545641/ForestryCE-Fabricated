package forestry.arboriculture.leaves;

import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.arboriculture.genetics.ITree;

import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.item.ItemBlockForestry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DecorativeLeavesBlockItem extends ItemBlockForestry<DecorativeLeavesBlock> implements ITintedItem {
	public DecorativeLeavesBlockItem(DecorativeLeavesBlock block, Item.Properties properties) {
		super(block, properties);
	}

	@Override
	public Component getName(ItemStack itemStack) {
		DecorativeLeavesBlock block = getBlock();
		ForestryLeafType treeDefinition = block.getType();
		return treeDefinition.getIndividual() == null ? super.getName(itemStack) : LeavesBlockItem.getDisplayName(treeDefinition.getIndividual().getSpecies());
	}

	@Override
	public int getColorFromItemStack(ItemStack itemStack, int renderPass) {
		DecorativeLeavesBlock block = getBlock();
		ForestryLeafType leafType = block.getType();

		ITree individual = leafType.getIndividual();
        if (individual == null) return net.minecraft.world.level.FoliageColor.FOLIAGE_DEFAULT;
		IGenome genome = individual.getGenome();

		if (renderPass == AbstractLeavesBlock.FRUIT_COLOR_INDEX) {
			IFruit fruitProvider = genome.resolveActive(TreeChromosomes.FRUIT);
			return fruitProvider.getDecorativeColor();
		}
		return forestry.core.platform.client.ClientsideCode.getLeafTint(individual.getSpecies(), null, null);
	}
}
