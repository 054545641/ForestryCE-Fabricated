package forestry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.core.circuits.ForestryCircuitLayouts;
import forestry.api.core.circuits.ICircuit;
import forestry.api.core.circuits.ICircuitLayout;
import forestry.api.core.circuits.ICircuitManager;
import forestry.core.content.energy.blocks.EngineBlockType;
import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.energy.tiles.BiogasEngineBlockEntity;
import forestry.core.content.resources.EnumElectronTube;
import forestry.core.engine.circuits.EnumCircuitBoardType;
import forestry.core.engine.circuits.ItemCircuitBoard;
import forestry.core.features.CoreItems;

/**
 * Covers the biogas engine's circuit socket, which players reported as the engine losing its upgrade.
 * <p>
 * Two separate defects produced that report and each has a test below. The engine overrode
 * {@code writeData(FriendlyByteBuf)} while every call site passes a {@code RegistryFriendlyByteBuf}, so the
 * override was dead code that still compiled under {@code @Override} and the socket never reached the client.
 * Separately, {@code setSocket} wrote through an InventoryAdapter whose chain ends in an empty
 * {@code setChanged} body, so an idle engine could drop the chipset on chunk unload.
 * <p>
 * Neither shows up while the engine is running. The chipset stays on disk because {@code burn} marks the tile
 * changed whenever it generates energy, and the client keeps whatever a socket click last pushed to it, so the
 * gap only opens on a relog or an idle engine.
 * <p>
 * The sync test asserts on what the server writes, not on a round trip. {@code readData} is annotated
 * {@code @OnlyIn(Dist.CLIENT)} and is stripped in this dedicated server run, so reading the payload back here
 * would exercise the empty superclass method rather than the engine's.
 */


public class EngineSocketTest {
	private static final BlockPos ENGINE_POS = new BlockPos(8, 1, 8);

	/** The engine has exactly one socket. */
	private static final int SOCKET = 0;

	/**
	 * The description packet is what rebuilds the client's copy on chunk load, and it must carry the socket. With
	 * the dead override the payload held only the fields EngineBlockEntity writes, so socketing a chipset changed
	 * nothing on the wire and the client drew an empty slot after every relog.
	 */

	public static void theSocketReachesTheDescriptionPacket(GameTestHelper helper) {
		BiogasEngineBlockEntity engine = engineAt(helper);
		HolderLookup.Provider registries = helper.getLevel().registryAccess();

		CompoundTag withoutChipset = engine.getUpdateTag(registries);
		engine.setSocket(SOCKET, engineChipset(helper));
		CompoundTag withChipset = engine.getUpdateTag(registries);

		helper.assertTrue(!withoutChipset.equals(withChipset),
			"the description packet was identical with and without a socketed chipset, so writeData never ran");
		helper.succeed();
	}

	/**
	 * Socketing a chipset must mark the chunk, or the write is lost when nothing else dirties it before unload.
	 */

	public static void socketingMarksTheChunkDirty(GameTestHelper helper) {
		BiogasEngineBlockEntity engine = engineAt(helper);
		LevelChunk chunk = helper.getLevel().getChunkAt(engine.getBlockPos());

		chunk.tryMarkSaved();
		engine.setSocket(SOCKET, engineChipset(helper));

		helper.assertTrue(chunk.isUnsaved(),
			"socketing left the chunk clean, so an idle engine would drop the chipset on unload");
		helper.succeed();
	}

	/**
	 * Emptying the socket must mark the chunk too. This is the soldering iron and the block break path, where a
	 * missed mark restores the removed chipset on the next load and duplicates it.
	 */

	public static void emptyingTheSocketMarksTheChunkDirty(GameTestHelper helper) {
		BiogasEngineBlockEntity engine = engineAt(helper);
		engine.setSocket(SOCKET, engineChipset(helper));

		LevelChunk chunk = helper.getLevel().getChunkAt(engine.getBlockPos());
		chunk.tryMarkSaved();
		engine.setSocket(SOCKET, ItemStack.EMPTY);

		helper.assertTrue(chunk.isUnsaved(), "emptying the socket left the chunk clean");
		helper.succeed();
	}

	/**
	 * Pins the save and load round trip. This one passed before either fix, since the serialization was always
	 * sound, and it is here so a change to how sockets are written cannot break the disk format unnoticed.
	 */

	public static void theSocketSurvivesSaveAndLoad(GameTestHelper helper) {
		BiogasEngineBlockEntity engine = engineAt(helper);
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		ItemStack chipset = engineChipset(helper);
		engine.setSocket(SOCKET, chipset);

		CompoundTag saved = engine.saveWithFullMetadata(registries);
		// A fresh block entity, the way a chunk load builds one
		BlockEntity reloaded = BlockEntity.loadStatic(engine.getBlockPos(), engine.getBlockState(), saved, registries);
		helper.assertTrue(reloaded instanceof BiogasEngineBlockEntity, "the engine did not load back as a biogas engine");

		ItemStack socket = ((BiogasEngineBlockEntity) reloaded).getSocket(SOCKET);
		helper.assertTrue(!socket.isEmpty(), "the socket was empty after a save and load round trip");
		helper.assertTrue(ItemStack.isSameItemSameComponents(socket, chipset),
			"the socket held " + socket + " after a round trip, expected " + chipset);
		helper.succeed();
	}

	private static BiogasEngineBlockEntity engineAt(GameTestHelper helper) {
		helper.setBlock(ENGINE_POS, EnergyBlocks.ENGINES.get(EngineBlockType.BIOGAS).block());
		return (BiogasEngineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(ENGINE_POS));
	}

	/**
	 * Builds the chipset a player would solder: a gold electron tube on the engine upgrade layout, in a basic
	 * circuit board.
	 *
	 * @return The circuit board stack to socket
	 */
	private static ItemStack engineChipset(GameTestHelper helper) {
		ICircuitManager circuits = IForestryApi.INSTANCE.getCircuitManager();

		ICircuitLayout layout = circuits.getLayout(ForestryCircuitLayouts.ENGINE_UPGRADE);
		helper.assertTrue(layout != null, "the engine upgrade circuit layout is not registered");

		ICircuit circuit = circuits.getCircuit(layout, CoreItems.ELECTRON_TUBES.stack(EnumElectronTube.GOLD, 1));
		helper.assertTrue(circuit != null, "no engine circuit is registered for a gold electron tube");

		ItemStack chipset = ItemCircuitBoard.createCircuitboard(EnumCircuitBoardType.BASIC, layout, new ICircuit[]{circuit});
		helper.assertTrue(circuits.isCircuitBoard(chipset), "the built chipset is not recognized as a circuit board");
		return chipset;
	}
}
