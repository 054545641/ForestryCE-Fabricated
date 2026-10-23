package forestry.core.platform.network;

import forestry.api.ForestryConstants;
import forestry.core.platform.network.packets.PacketChipsetClick;
import forestry.core.platform.network.packets.PacketGuiSelectRequest;
import forestry.core.platform.network.packets.PacketPipetteClick;
import forestry.core.platform.network.packets.PacketSolderingIronClick;
import forestry.core.content.machines.network.packets.PacketRecipeTransferRequest;
import forestry.core.content.sorting.network.packets.PacketFilterChangeGenome;
import forestry.core.content.sorting.network.packets.PacketFilterChangeRule;
import forestry.core.content.worktable.network.packets.PacketWorktableRecipeRequest;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packets sent to the server from the client
 */
public class PacketIdServer {
	// Core Gui
	public static final CustomPacketPayload.Type<PacketGuiSelectRequest> GUI_SELECTION_REQUEST = type("gui_selection_request");
	public static final CustomPacketPayload.Type<PacketPipetteClick> PIPETTE_CLICK = type("pipette_click");
	public static final CustomPacketPayload.Type<PacketChipsetClick> CHIPSET_CLICK = type("chipset_click");
	public static final CustomPacketPayload.Type<PacketSolderingIronClick> SOLDERING_IRON_CLICK = type("soldering_iron_click");
	// Sorting
	public static final CustomPacketPayload.Type<PacketFilterChangeRule> FILTER_CHANGE_RULE = type("filter_change_rule");
	public static final CustomPacketPayload.Type<PacketFilterChangeGenome> FILTER_CHANGE_GENOME = type("filter_change_genome");
	// JEI
	public static final CustomPacketPayload.Type<PacketWorktableRecipeRequest> WORKTABLE_RECIPE_REQUEST = type("worktable_recipe_request");
	public static final CustomPacketPayload.Type<PacketRecipeTransferRequest> RECIPE_TRANSFER_REQUEST = type("recipe_transfer_request");
	// Mail

	public static <P extends CustomPacketPayload> CustomPacketPayload.Type<P> type(String path) {
		return new CustomPacketPayload.Type<>(ForestryConstants.forestry(path));
	}
}
