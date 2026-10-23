package forestry.core.platform.compat.jei;

import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class IndividualSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
	@Nullable
	@Override
	public Object getSubtypeData(ItemStack ingredient, UidContext context) {
		IIndividual individual = IIndividualHandlerItem.getIndividual(ingredient);
		return individual != null ? individual.getGenome().getActiveSpecies().id() : null;
	}

}
