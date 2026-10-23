package forestry.mail.carriers.trading;

import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Computes payment and change from actual attachments; never fabricates template components. */
public record TradePayment(int orders, List<ItemStack> received, List<ItemStack> surplus) {
	public static TradePayment plan(List<ItemStack> offered, List<ItemStack> requested, int maxOrders) {
		Map<ItemVariant, Integer> required = counts(requested);
		Map<ItemVariant, Integer> available = counts(offered);
		int orders = required.isEmpty() ? 0 : Math.max(0, maxOrders);
		for (var entry : required.entrySet()) orders = Math.min(orders, available.getOrDefault(entry.getKey(), 0) / entry.getValue());
		List<ItemStack> received = new ArrayList<>();
		for (var entry : required.entrySet()) {
			int used = Math.multiplyExact(entry.getValue(), orders);
			append(received, entry.getKey(), used);
			available.computeIfPresent(entry.getKey(), (key, count) -> count - used);
		}
		List<ItemStack> surplus = new ArrayList<>();
		available.forEach((resource, count) -> append(surplus, resource, count));
		return new TradePayment(orders, received, surplus);
	}
	private static Map<ItemVariant, Integer> counts(List<ItemStack> stacks) {
		Map<ItemVariant, Integer> result = new LinkedHashMap<>();
		for (ItemStack stack : stacks) if (!stack.isEmpty()) result.merge(ItemVariant.of(stack), stack.getCount(), Math::addExact);
		return result;
	}
	private static void append(List<ItemStack> stacks, ItemVariant resource, int count) {
		while (count > 0) {
			int size = Math.min(count, resource.toStack().getMaxStackSize());
			stacks.add(resource.toStack(size));
			count -= size;
		}
	}
}
