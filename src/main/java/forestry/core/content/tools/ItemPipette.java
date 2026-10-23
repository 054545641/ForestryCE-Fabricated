package forestry.core.content.tools;

import forestry.api.core.IToolPipette;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.fluids.PipetteContents;
import forestry.core.platform.item.ITintedItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import forestry.core.platform.item.ItemForestry;

public class ItemPipette extends ItemForestry implements IToolPipette, ITintedItem {
	public ItemPipette(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public boolean canPipette(ItemStack itemstack) {
		PipetteContents contained = PipetteContents.create(itemstack);
		return contained == null || !contained.isFull();
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> builder, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, display, builder, flag);

		PipetteContents contained = PipetteContents.create(itemstack);
		if (contained != null) {
			List<Component> lines = new java.util.ArrayList<>();
			contained.addTooltip(lines);
			lines.forEach(builder);
		}
	}

	public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> createFluidHandler(net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext context) {
        return new forestry.core.platform.fluids.PipetteFluidStorage(context);
    }

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		if (tintIndex == 1) {
			PipetteContents contents = PipetteContents.create(stack);

			if (contents != null) {
				return forestry.core.platform.client.ClientsideCode.getFluidColor(contents.getContents().getFluid());
			}
		}
		return 0xffffff;
	}
}
