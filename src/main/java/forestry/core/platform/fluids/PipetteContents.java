package forestry.core.platform.fluids;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import forestry.api.core.fluids.FluidVolume;

import forestry.core.platform.fluids.FluidHelper;

import javax.annotation.Nullable;
import java.util.List;

public class PipetteContents {
	private final FluidVolume contents;

	@Nullable
	public static PipetteContents create(ItemStack itemStack) {
		FluidVolume contents = FluidHelper.getFluidContained(itemStack);
		if (contents.isEmpty()) {
			return null;
		}
		return new PipetteContents(contents);
	}

	public PipetteContents(FluidVolume contents) {
		this.contents = contents;
	}

	public FluidVolume getContents() {
		return this.contents;
	}

	public boolean isFull() {
		return this.contents.getAmount() >= net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
	}

	public void addTooltip(List<Component> list) {
		list.add(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(this.contents.variant()).copy().append(" (" + java.math.BigDecimal.valueOf(this.contents.getAmount()).divide(java.math.BigDecimal.valueOf(81), 3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " mB)").withStyle(ChatFormatting.GRAY));
	}
}
