package forestry.apiculture.bees;

import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.engine.genetics.ItemGE;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ForestryBeeItem extends ItemGE implements ITintedItem {
	public ForestryBeeItem(BeeLifeStage type, Properties properties) {
		super(type != BeeLifeStage.DRONE ? properties.stacksTo(1) : properties, type);
	}

	@Override
	public ISpeciesType<?, ?> getType() {
		return SpeciesUtil.BEE_TYPE.get();
	}

	@Override
	protected IBeeSpecies getSpecies(ItemStack stack) {
		return IIndividualHandlerItem.getSpecies(stack, SpeciesUtil.BEE_TYPE.get());
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		if (!hasIndividual(stack)) {
			return;
		}

		if (this.stage != BeeLifeStage.DRONE) {
			IIndividualHandlerItem.ifPresent(stack, individual -> {
				if (((IBee) individual).isPristine()) {
					list.add(Component.translatable("for.bees.stock.pristine").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
				} else {
					list.add(Component.translatable("for.bees.stock.ignoble").withStyle(ChatFormatting.YELLOW));
				}
			});
		}

		super.appendHoverText(stack, context, list, flag);
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int tintIndex) {
		IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
		if (individual instanceof IBee bee) {
			IBeeSpecies species = bee.getSpecies();

			return switch (tintIndex) {
				case 2 -> species.getStripes();
				case 1 -> species.getBody();
				case 0 -> species.getOutline();
				default -> 0xffffff;
			};
		} else {
			if (tintIndex == 1) {
				// 1 = body
				return 0xffdc16;
			} else if (tintIndex == 2) {
				// 2 = stripes
				return 0;
			} else {
				// 0 = outline
				return 0xffffff;
			}
		}
	}

	public final BeeLifeStage getStage() {
		return (BeeLifeStage) this.stage;
	}
}
