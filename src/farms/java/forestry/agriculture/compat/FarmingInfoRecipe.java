package forestry.agriculture.compat;

import forestry.api.core.circuits.ICircuit;
import forestry.api.agriculture.IFarmType;
import net.minecraft.world.item.ItemStack;

public record FarmingInfoRecipe(ItemStack tube, IFarmType properties, ICircuit circuit) {
}
