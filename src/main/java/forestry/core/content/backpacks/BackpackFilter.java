package forestry.core.content.backpacks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.function.Predicate;
/** Uses live native tags; absent rejection tags represent the empty set. */
public final class BackpackFilter implements Predicate<ItemStack> {
    private final TagKey<Item> acceptKey, rejectKey;
    public BackpackFilter(TagKey<Item> acceptKey, TagKey<Item> rejectKey) { this.acceptKey = acceptKey; this.rejectKey = rejectKey; }
    @Override public boolean test(ItemStack stack) { return !stack.isEmpty() && stack.is(acceptKey) && !stack.is(rejectKey); }
}
