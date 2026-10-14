package forestry.arboriculture.leaves;

import forestry.api.arboriculture.genetics.ITree;
import forestry.api.client.IForestryClientApi;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ITintedItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item for the tile-less genetic leaves ({@link DefaultLeavesBlock} and
 * {@link DefaultFruitLeavesBlock}). Their stacks carry no genome NBT, so the species must
 * be resolved from the block's {@link ForestryLeafType} - exactly as {@code ItemBlockDecorativeLeaves} does for
 * decorative leaves. Using {@code ItemBlockLeaves} here (its no-NBT fallbacks) is what made these items show the raw
 * grammar name and a uniform default-foliage tint.
 */
public class DefaultLeavesBlockItem extends ItemBlockForestry<AbstractLeavesBlock> implements ITintedItem {
	public DefaultLeavesBlockItem(AbstractLeavesBlock block, Item.Properties properties) {
		super(block, properties);
	}

	private ForestryLeafType leafType() {
		return ((ILeafTypeBlock) getBlock()).getType();
	}

	@Override
	public Component getName(ItemStack itemStack) {
		return LeavesBlockItem.getDisplayName(leafType().getIndividual().getSpecies());
	}

	@Override
	public int getColorFromItemStack(ItemStack itemStack, int renderPass) {
		ForestryLeafType type = leafType();
		if (renderPass == AbstractLeavesBlock.FRUIT_COLOR_INDEX) {
			return type.getFruit().getDecorativeColor();
		}
		ITree individual = type.getIndividual();
		return IForestryClientApi.INSTANCE.getTreeManager().getTint(individual.getSpecies()).get(null, null);
	}
}
