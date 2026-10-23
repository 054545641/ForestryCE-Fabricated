package forestry.core.platform.network;

import forestry.core.platform.network.packets.FlowerTypeSyncPacket;
import forestry.core.platform.network.packets.TaxonSyncPacket;
import forestry.core.platform.network.packets.PacketActiveUpdate;
import forestry.core.platform.network.packets.PacketErrorUpdate;
import forestry.core.platform.network.packets.PacketGenomeTrackerSync;
import forestry.core.platform.network.packets.PacketGuiEnergy;
import forestry.core.platform.network.packets.PacketGuiLayoutSelect;
import forestry.core.platform.network.packets.PacketGuiStream;
import forestry.core.platform.network.packets.PacketItemStackDisplay;
import forestry.core.platform.network.packets.PacketSocketUpdate;
import forestry.core.platform.network.packets.PacketTankLevelUpdate;
import forestry.core.platform.network.packets.PacketTileStream;
import forestry.core.platform.network.packets.RecipeCachePacket;
import forestry.core.platform.network.packets.PacketRefractoryWax;
import forestry.core.content.machines.network.packets.PacketRecipeTransferUpdate;
import forestry.core.content.sorting.network.packets.PacketGuiFilterUpdate;
import forestry.core.content.worktable.network.packets.PacketWorktableMemoryUpdate;
import forestry.core.content.worktable.network.packets.PacketWorktableRecipeUpdate;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static forestry.core.platform.network.PacketIdServer.type;

/**
 * Packets sent to the client from the server
 */
public class PacketIdClient {
	// Core
	public static final CustomPacketPayload.Type<RecipeCachePacket> RECIPE_CACHE = type("recipe_cache");
	// Core Gui
	public static final CustomPacketPayload.Type<PacketErrorUpdate> ERROR_UPDATE = type("error_update");
	public static final CustomPacketPayload.Type<PacketGuiStream> GUI_UPDATE = type("gui_update");
	public static final CustomPacketPayload.Type<PacketGuiLayoutSelect> GUI_LAYOUT_SELECT = type("gui_layout_select");
	public static final CustomPacketPayload.Type<PacketGuiEnergy> GUI_ENERGY = type("gui_energy");
	public static final CustomPacketPayload.Type<PacketSocketUpdate> SOCKET_UPDATE = type("socket_update");
	// Core Tile Entities
	public static final CustomPacketPayload.Type<PacketTileStream> TILE_FORESTRY_UPDATE = type("tile_forestry_update");
	public static final CustomPacketPayload.Type<PacketItemStackDisplay> ITEMSTACK_DISPLAY = type("itemstack_display");
	public static final CustomPacketPayload.Type<PacketTankLevelUpdate> TANK_LEVEL_UPDATE = type("tank_level_update");
	public static final CustomPacketPayload.Type<PacketRefractoryWax> REFRACTORY_WAX_ON = type("refractory_wax_on");
	// Core Genome
	public static final CustomPacketPayload.Type<PacketGenomeTrackerSync> GENOME_TRACKER_UPDATE = type("genome_tracker_update");
	// Factory
	public static final CustomPacketPayload.Type<PacketWorktableMemoryUpdate> WORKTABLE_MEMORY_UPDATE = type("worktable_memory_update");
	public static final CustomPacketPayload.Type<PacketWorktableRecipeUpdate> WORKTABLE_CRAFTING_UPDATE = type("worktable_crafting_update");
	// Apiculture
	public static final CustomPacketPayload.Type<PacketActiveUpdate> TILE_FORESTRY_ACTIVE = type("tile_forestry_active");
	// Genetics
	public static final CustomPacketPayload.Type<TaxonSyncPacket> TAXON_SYNC = type("taxon_sync");
	// Flower types are shared by bees and butterflies, so base owns the sync. The path is unchanged
	// from when this lived in ApiculturePacketIds, so the wire id is the same
	public static final CustomPacketPayload.Type<FlowerTypeSyncPacket> FLOWER_TYPE_SYNC = type("flower_type_sync");
	// Mail
	// Sorting
	public static final CustomPacketPayload.Type<PacketGuiFilterUpdate> GUI_UPDATE_FILTER = type("gui_update_filter");
	// JEI
	public static final CustomPacketPayload.Type<PacketRecipeTransferUpdate> RECIPE_TRANSFER_UPDATE = type("recipe_transfer_update");
}
