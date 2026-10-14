package forestry.core.content.tools;

import forestry.api.core.IToolPipette;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.fluids.PipetteContents;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.util.RenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;

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

	public ItemAccessFluidHandler createFluidHandler(ItemAccess access) {
		return new ItemAccessFluidHandler(access, CoreDataComponents.FLUID_CONTENT.get(), FluidType.BUCKET_VOLUME);
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		if (tintIndex == 1) {
			PipetteContents contents = PipetteContents.create(stack);

			if (contents != null) {
				return RenderUtil.getFluidColor(contents.getContents().getFluid());
			}
		}
		return 0xffffff;
	}
}
