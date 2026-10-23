package forestry.arboriculture.features;

import forestry.arboriculture.wood.ForestryWoodType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Only item lookup is substituted. Boat entity, synchronization, serialization and container code are production sources. */
public final class ArboricultureItems {
    public static final Group BOAT = new Group(false), CHEST_BOAT = new Group(true);
    public record Group(boolean chest) {
        public Item item(ForestryWoodType wood) {
            return switch (wood) {
                case LARCH -> chest ? Items.OAK_CHEST_BOAT : Items.OAK_BOAT;
                case TEAK -> chest ? Items.BIRCH_CHEST_BOAT : Items.BIRCH_BOAT;
                default -> chest ? Items.ACACIA_CHEST_BOAT : Items.ACACIA_BOAT;
            };
        }
    }
}
