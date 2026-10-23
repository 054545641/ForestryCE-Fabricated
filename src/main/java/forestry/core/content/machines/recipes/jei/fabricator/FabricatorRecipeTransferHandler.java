package forestry.core.content.machines.recipes.jei.fabricator;

import forestry.api.core.machines.IFabricatorRecipe;
import forestry.core.platform.recipes.jei.ForestryRecipeType;
import forestry.core.platform.util.JeiUtil;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.content.machines.features.FactoryMenuTypes;
import forestry.core.content.machines.gui.ContainerFabricator;
import forestry.core.content.machines.network.packets.PacketRecipeTransferRequest;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public class FabricatorRecipeTransferHandler implements IRecipeTransferHandler<ContainerFabricator, IFabricatorRecipe> {
	@Override
	public Class<ContainerFabricator> getContainerClass() {
		return ContainerFabricator.class;
	}

	@Override
	public Optional<MenuType<ContainerFabricator>> getMenuType() {
		return Optional.of(FactoryMenuTypes.FABRICATOR.menuType());
	}

	@Override
	public IRecipeType<IFabricatorRecipe> getRecipeType() {
		return ForestryRecipeType.FABRICATOR;
	}

	@Nullable
	@Override
	public IRecipeTransferError transferRecipe(ContainerFabricator container, IFabricatorRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
		if (doTransfer) {
			Container craftingInventory = container.getTile().getCraftingInventory();
			NonNullList<ItemStack> items = JeiUtil.getCraftingItems(recipeSlots);
			for (int i = 0; i < items.size(); i++) {
				craftingInventory.setItem(i, items.get(i));
			}

			NetworkUtil.sendToServer(new PacketRecipeTransferRequest(container.getTile().getBlockPos(), items));
		}

		return null;
	}
}
