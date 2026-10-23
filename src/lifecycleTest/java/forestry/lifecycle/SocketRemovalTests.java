package forestry.lifecycle;

import com.mojang.authlib.GameProfile;
import forestry.api.IForestryApi;
import forestry.api.core.circuits.ForestryCircuitLayouts;
import forestry.api.core.circuits.ICircuit;
import forestry.core.content.energy.blocks.EngineBlockType;
import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.energy.menu.PeatEngineMenu;
import forestry.core.content.energy.tiles.PeatEngineBlockEntity;
import forestry.core.content.resources.EnumElectronTube;
import forestry.core.engine.circuits.EnumCircuitBoardType;
import forestry.core.engine.circuits.ItemCircuitBoard;
import forestry.core.features.CoreItems;
import forestry.core.platform.network.packets.PacketSolderingIronClick;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

/** Exercises the removal packet with the same four-apatite board as the reported failure. */
final class SocketRemovalTests {

	static void cursorRemoval(GameTestHelper h) {
		for (GameType mode : new GameType[]{GameType.CREATIVE, GameType.SURVIVAL}) {
			Fixture f = fixture(h);
			f.player.setGameMode(mode);
			f.player.containerMenu.setCarried(f.iron);
			remove(f);
			assertRemoved(h, f);
			h.assertTrue(f.iron.getDamageValue() == (mode == GameType.CREATIVE ? 0 : 1), "Only survival removal consumes durability");
		}
		h.succeed();
	}

	static void heldIronRejected(GameTestHelper h) {
		for (InteractionHand hand : InteractionHand.values()) {
			Fixture f = fixture(h);
			remove(f);
			h.assertTrue(ItemStack.matches(f.engine.getSocket(0), f.board), "An empty cursor without an iron cannot remove the board");
			f.player.setItemInHand(hand, f.iron);
			f.player.containerMenu.setCarried(new ItemStack(Items.APPLE));
			remove(f);
			h.assertTrue(ItemStack.matches(f.engine.getSocket(0), f.board), "A different item on the cursor takes precedence over a held iron");
			f.player.containerMenu.setCarried(ItemStack.EMPTY);
			remove(f);
			h.assertTrue(ItemStack.matches(f.engine.getSocket(0), f.board), "An iron in either hand cannot remove the board with an empty cursor");
			h.assertTrue(f.iron.getDamageValue() == 0, "Rejected removal does not damage a held iron");
			f.player.setItemInHand(hand, ItemStack.EMPTY);
			f.player.containerMenu.setCarried(f.iron);
			remove(f);
			assertRemoved(h, f);
			h.assertTrue(f.iron.getDamageValue() == 1, "Moving the same iron onto the cursor allows removal");
		}
		h.succeed();
	}

	static void fullInventory(GameTestHelper h) {
		Fixture f = fixture(h);
		f.player.containerMenu.setCarried(f.iron);
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			f.player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		}
		remove(f);
		h.assertTrue(ItemStack.matches(f.engine.getSocket(0), f.board), "A full backpack retains the entire upgraded board");
		h.assertTrue(f.iron.getDamageValue() == 0, "Failed removal does not damage the iron");
		h.assertTrue(f.player.getOffhandItem().isEmpty(), "An empty offhand is not used as a spare backpack slot");
		f.player.getInventory().setItem(Inventory.INVENTORY_SIZE - 1, ItemStack.EMPTY);
		remove(f);
		assertRemoved(h, f);
		h.assertTrue(ItemStack.matches(f.player.getInventory().getItem(Inventory.INVENTORY_SIZE - 1), f.board), "The last backpack slot accepts the board");
		h.succeed();
	}

	static void cursorIronBreaks(GameTestHelper h) {
		Fixture f = fixture(h);
		f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
		f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
		f.iron.setDamageValue(f.iron.getMaxDamage() - 1);
		f.player.containerMenu.setCarried(f.iron);
		remove(f);
		assertRemoved(h, f);
		h.assertTrue(f.player.containerMenu.getCarried().isEmpty(), "The worn cursor iron breaks");
		h.assertTrue(f.player.getMainHandItem().is(Items.APPLE) && f.player.getOffhandItem().is(Items.STICK), "Breaking the cursor iron does not clear either hand");
		h.succeed();
	}

	private static Fixture fixture(GameTestHelper h) {
		BlockPos pos = new BlockPos(2, 2, 2);
		h.setBlock(pos, EnergyBlocks.ENGINES.get(EngineBlockType.PEAT).block());
		var engine = (PeatEngineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
		var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "SocketTest"));
		player.setGameMode(GameType.SURVIVAL);
		player.setPos(engine.getBlockPos().getX() + 0.5, engine.getBlockPos().getY(), engine.getBlockPos().getZ() + 1.5);
		player.containerMenu = new PeatEngineMenu(1, player.getInventory(), engine);
		var circuits = IForestryApi.INSTANCE.getCircuitManager();
		var layout = circuits.getLayout(ForestryCircuitLayouts.ENGINE_UPGRADE);
		ICircuit apatite = circuits.getCircuit(layout, CoreItems.ELECTRON_TUBES.stack(EnumElectronTube.APATITE, 1));
		h.assertTrue(apatite != null, "The apatite engine upgrade is registered");
		ItemStack board = ItemCircuitBoard.createCircuitboard(EnumCircuitBoardType.INTRICATE, layout, new ICircuit[]{apatite, apatite, apatite, apatite});
		engine.setSocket(0, board.copy());
		return new Fixture(engine, player, board, CoreItems.SOLDERING_IRON.stack());
	}

	private static void remove(Fixture f) {
		PacketSolderingIronClick.handle(new PacketSolderingIronClick(0), f.player);
	}

	private static void assertRemoved(GameTestHelper h, Fixture f) {
		h.assertTrue(f.engine.getSocket(0).isEmpty(), "The peat engine socket is emptied");
		int returned = 0;
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack stack = f.player.getInventory().getItem(slot);
			if (ItemStack.isSameItemSameComponents(stack, f.board)) returned += stack.getCount();
		}
		h.assertTrue(returned == 1, "Exactly one board returns with all four circuits intact");
		remove(f);
		h.assertTrue(f.engine.getSocket(0).isEmpty(), "Repeated removal leaves the socket empty");
	}

	private record Fixture(PeatEngineBlockEntity engine, ServerPlayer player, ItemStack board, ItemStack iron) {}
}
