package forestry.core.platform.fluids;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import forestry.api.core.fluids.FluidVolume;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import javax.annotation.Nullable;

public interface ITankManager extends SlottedStorage<FluidVariant> {
	// Used to send all tanks to the player upon first opening the screen
	void sendAllTanks(AbstractContainerMenu container, ServerPlayer player);

	// Used to send incremental changes in tanks to players who already have the initial state of the tanks
	void broadcastChanges(AbstractContainerMenu container, ServerPlayer players);

	// Used to clean up cached item stacks when a player closes the screen
	void onClosed(AbstractContainerMenu container);

	@Nullable
	StandardTank getTank(int tankIndex);

	boolean canFillFluidType(FluidVolume fluidStack);

	/**
	 * For updating tanks on the client
	 */
	void processTankUpdate(int tankIndex, @Nullable FluidVolume contents);

	void processTankUpdate(int tankIndex, FluidVolume contents, long capacity);
}
