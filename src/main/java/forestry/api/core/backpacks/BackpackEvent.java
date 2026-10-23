package forestry.api.core.backpacks;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;



public abstract class BackpackEvent extends forestry.api.core.ForestryEvent {

	private boolean canceled;
    public boolean isCanceled() { return canceled; }
    public void setCanceled(boolean canceled) { this.canceled = canceled; }
    public static <E extends BackpackEvent> E dispatch(E event) { forestry.api.core.ForestryEvent.post(event); return event; }
    public final Player player;
	public final IBackpackDefinition backpackDefinition;
	public final Container backpackInventory;

	public BackpackEvent(Player player, IBackpackDefinition backpackDefinition, Container backpackInventory) {
		this.player = player;
		this.backpackDefinition = backpackDefinition;
		this.backpackInventory = backpackInventory;
	}
}
