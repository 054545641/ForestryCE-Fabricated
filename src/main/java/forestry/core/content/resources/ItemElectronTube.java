package forestry.core.content.resources;

import forestry.api.IForestryApi;
import forestry.api.core.circuits.ICircuit;
import forestry.api.core.circuits.ICircuitLayout;
import forestry.api.core.circuits.ICircuitManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import forestry.core.platform.item.ItemForestry;

public class ItemElectronTube extends ItemForestry {

	// The subtype is what the registry keys the item by, nothing here reads it back
	public ItemElectronTube(EnumElectronTube type, Properties properties) {
		super(properties);
	}

	// todo show the combined speed, efficiency and fortune multipliers
	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
		ArrayList<Pair<ICircuitLayout, ICircuit>> circuits = getCircuits(itemstack);
		if (!circuits.isEmpty()) {
			for (var entry : circuits) {
				list.add(entry.left().getUsage().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE));
				entry.right().addTooltip(list);
			}
		} else {
			list.add(Component.literal("<")
				.append(Component.translatable("for.gui.noeffect")
					.append(">").withStyle(ChatFormatting.GRAY)));
		}
	}

	private static ArrayList<Pair<ICircuitLayout, ICircuit>> getCircuits(ItemStack stack) {
		ArrayList<Pair<ICircuitLayout, ICircuit>> circuits = new ArrayList<>();
		ICircuitManager manager = IForestryApi.INSTANCE.getCircuitManager();

		for (ICircuitLayout layout : manager.getLayouts()) {
			ICircuit circuit = manager.getCircuit(layout, stack);
			if (circuit != null) {
				circuits.add(Pair.of(layout, circuit));
			}
		}

		return circuits;
	}
}
