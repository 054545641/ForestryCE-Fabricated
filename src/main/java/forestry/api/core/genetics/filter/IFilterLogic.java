package forestry.api.core.genetics.filter;

import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import forestry.api.core.genetics.ISpecies;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IFilterLogic extends INbtWritable, INbtReadable {
	void writeGuiData(FriendlyByteBuf buffer);

	void readGuiData(FriendlyByteBuf data);

	boolean isValid(ItemStack stack, Direction facing);

	boolean isValid(Direction facing, ItemStack stack, FilterData filterData);

	boolean isValidAllelePair(Direction orientation, ISpecies<?> active, ISpecies<?> inactive);

	IFilterRuleType getRule(Direction facing);

	boolean setRule(Direction facing, IFilterRuleType rule);

	@Nullable
	ISpecies<?> getGenomeFilter(Direction facing, int index, boolean active);

	boolean setGenomeFilter(Direction facing, int index, boolean active, @Nullable ISpecies<?> allele);

	void sendToServer(Direction facing, int index, boolean active, @Nullable ISpecies<?> allele);

	void sendToServer(Direction facing, IFilterRuleType rule);

	INetworkHandler getNetworkHandler();

	interface INetworkHandler {
		/**
		 * Sends the data of the logic to the client of all players that have the gui currently open.
		 *
		 * @param player The player that changed the filter.
		 */
		void sendToPlayers(IFilterLogic logic, ServerLevel level, Player player);
	}
}
