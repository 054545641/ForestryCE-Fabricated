package forestry.mail.carriers.trading;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Computes payment and change from actual attachments; never fabricates template components. */
public record TradePayment(int orders, List<ItemStack> received, List<ItemStack> surplus) {
	public static TradePayment plan(List<ItemStack> offered, List<ItemStack> requested, int maxOrders) {
		Map<ItemResource, Integer> required = counts(requested);
		Map<ItemResource, Integer> available = counts(offered);
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
	private static Map<ItemResource, Integer> counts(List<ItemStack> stacks) {
		Map<ItemResource, Integer> result = new LinkedHashMap<>();
		for (ItemStack stack : stacks) if (!stack.isEmpty()) result.merge(ItemResource.of(stack), stack.getCount(), Math::addExact);
		return result;
	}
	private static void append(List<ItemStack> stacks, ItemResource resource, int count) {
		while (count > 0) {
			int size = Math.min(count, resource.getMaxStackSize());
			stacks.add(resource.toStack(size));
			count -= size;
		}
	}
}
