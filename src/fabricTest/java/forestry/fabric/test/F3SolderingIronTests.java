package forestry.fabric.test;

import com.mojang.authlib.GameProfile;
import forestry.api.IForestryApi;
import forestry.api.core.circuits.ForestryCircuitLayouts;
import forestry.api.core.circuits.ICircuit;
import forestry.core.content.energy.menu.PeatEngineMenu;
import forestry.core.content.energy.tiles.PeatEngineBlockEntity;
import forestry.core.content.resources.EnumElectronTube;
import forestry.core.engine.circuits.ContainerSolderingIron;
import forestry.core.engine.circuits.EnumCircuitBoardType;
import forestry.core.engine.circuits.ItemCircuitBoard;
import forestry.core.features.CoreUtilityItems;
import forestry.core.platform.inventory.SolderingIronInventory;
import forestry.core.platform.network.packets.PacketSolderingIronClick;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import java.util.UUID;
import static forestry.fabric.test.F1RegistrationTests.*;

public final class F3SolderingIronTests {
    @GameTest
    public void survivalRemovalConsumesOneUseAndFifthRemovalBreaksCursorIron(GameTestHelper test) {
        var fixture = fixture(test, GameType.SURVIVAL);
        require(fixture.iron().isDamageableItem() && fixture.iron().getMaxDamage() == 5, "Soldering iron lost its five-use durability");
        fixture.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
        fixture.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        fixture.player().containerMenu.setCarried(fixture.iron());
        for (int uses = 1; uses <= 5; uses++) {
            fixture.engine().setSocket(0, fixture.board().copy());
            remove(fixture);
            require(fixture.engine().getSocket(0).isEmpty() && returnedBoards(fixture) == uses, "Successful removal lost or duplicated a programmed board");
            var carried = fixture.player().containerMenu.getCarried();
            require(uses == 5 ? carried.isEmpty() : carried.getDamageValue() == uses, "Survival removal did not consume exactly one durability");
            var previous = carried.copy();
            remove(fixture);
            require(ItemStack.matches(previous, fixture.player().containerMenu.getCarried()) && returnedBoards(fixture) == uses, "Empty-socket click consumed durability or duplicated a board");
        }
        require(fixture.player().getMainHandItem().is(Items.APPLE) && fixture.player().getOffhandItem().is(Items.STICK), "Breaking the cursor iron cleared an equipped item");
        test.succeed();
    }

    @GameTest
    public void creativeRemovalDoesNotConsumeDurability(GameTestHelper test) {
        var fixture = fixture(test, GameType.CREATIVE);
        fixture.player().containerMenu.setCarried(fixture.iron());
        for (int uses = 1; uses <= 6; uses++) {
            fixture.engine().setSocket(0, fixture.board().copy());
            remove(fixture);
            require(fixture.engine().getSocket(0).isEmpty() && returnedBoards(fixture) == uses, "Creative removal did not return exactly one board");
            require(!fixture.iron().isEmpty() && fixture.iron().getDamageValue() == 0, "Creative removal consumed durability");
        }
        test.succeed();
    }

    @GameTest
    public void rejectedRemovalKeepsBoardAndDurability(GameTestHelper test) {
        var fixture = fixture(test, GameType.SURVIVAL);
        var player = fixture.player();
        player.containerMenu.setCarried(fixture.iron());
        PacketSolderingIronClick.handle(new PacketSolderingIronClick(player.containerMenu.containerId + 1, 0), player);
        for (int slot : new int[]{-1, fixture.engine().getSocketCount()}) {
            PacketSolderingIronClick.handle(new PacketSolderingIronClick(player.containerMenu.containerId, slot), player);
        }
        assertRejected(fixture);
        for (var hand : InteractionHand.values()) {
            player.setItemInHand(hand, fixture.iron());
            player.containerMenu.setCarried(ItemStack.EMPTY);
            remove(fixture);
            assertRejected(fixture);
            player.containerMenu.setCarried(new ItemStack(Items.APPLE));
            remove(fixture);
            assertRejected(fixture);
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
        player.containerMenu.setCarried(fixture.iron());
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        remove(fixture);
        assertRejected(fixture);
        require(player.getOffhandItem().isEmpty(), "A full inventory used the offhand as a spare board slot");
        player.getInventory().setItem(Inventory.INVENTORY_SIZE - 1, ItemStack.EMPTY);
        var position = fixture.engine().getBlockPos();
        player.setPos(position.getX() + 100, position.getY(), position.getZ());
        remove(fixture);
        assertRejected(fixture);
        player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 1.5);
        remove(fixture);
        require(fixture.engine().getSocket(0).isEmpty() && fixture.iron().getDamageValue() == 1, "Valid removal after rejection did not consume one use");
        require(ItemStack.matches(player.getInventory().getItem(Inventory.INVENTORY_SIZE - 1), fixture.board()), "Last backpack slot did not receive the intact board");
        test.succeed();
    }

    @GameTest
    public void solderingAndTakingFinishedBoardDoesNotDamageIron(GameTestHelper test) {
        var fixture = fixture(test, GameType.SURVIVAL);
        var player = fixture.player();
        fixture.iron().setDamageValue(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, fixture.iron());
        var inventory = new SolderingIronInventory(player, fixture.iron());
        inventory.setLayout(IForestryApi.INSTANCE.getCircuitManager().getLayout(ForestryCircuitLayouts.ENGINE_UPGRADE));
        inventory.setItem(0, new ItemStack(fixture.board().getItem()));
        for (int slot = 2; slot < 6; slot++) inventory.setItem(slot, new ItemStack(BuiltInRegistries.ITEM.getValue(id(EnumElectronTube.APATITE.registryPath()))));
        var menu = new ContainerSolderingIron(38, player, inventory);
        player.containerMenu = menu;
        inventory.onSlotClick(2, player);
        require(inventory.getItem(0).isEmpty() && ItemStack.matches(inventory.getItem(1), fixture.board()), "Soldering did not produce the configured board");
        for (int slot = 2; slot < 6; slot++) require(inventory.getItem(slot).isEmpty(), "Soldering did not consume its electron tubes");
        var output = menu.slots.stream().filter(slot -> slot.container == inventory && slot.getContainerSlot() == 1).findFirst().orElseThrow();
        menu.clicked(output.index, 0, ContainerInput.PICKUP, player);
        require(inventory.getItem(1).isEmpty() && ItemStack.matches(menu.getCarried(), fixture.board()), "Finished board could not be taken through the menu");
        require(fixture.iron().getDamageValue() == 2, "Soldering or taking the finished board damaged the iron");
        test.succeed();
    }

    private static Fixture fixture(GameTestHelper test, GameType mode) {
        var position = test.absolutePos(new BlockPos(2, 2, 2));
        var engine = (PeatEngineBlockEntity) F2MachineTests.place(test.getLevel(), position, "peat_engine");
        var player = FakePlayer.get(test.getLevel(), new GameProfile(UUID.randomUUID(), "SolderingTest"));
        player.setGameMode(mode);
        player.getInventory().clearContent();
        player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 1.5);
        player.containerMenu = new PeatEngineMenu(37, player.getInventory(), engine);
        var manager = IForestryApi.INSTANCE.getCircuitManager();
        var layout = manager.getLayout(ForestryCircuitLayouts.ENGINE_UPGRADE);
        var circuit = manager.getCircuit(layout, new ItemStack(BuiltInRegistries.ITEM.getValue(id(EnumElectronTube.APATITE.registryPath()))));
        require(circuit != null, "Apatite engine upgrade is not registered");
        var board = ItemCircuitBoard.createCircuitboard(EnumCircuitBoardType.INTRICATE, layout, new ICircuit[]{circuit, circuit, circuit, circuit});
        engine.setSocket(0, board.copy());
        return new Fixture(engine, player, board, CoreUtilityItems.SOLDERING_IRON.stack());
    }

    private static void remove(Fixture fixture) {
        PacketSolderingIronClick.handle(new PacketSolderingIronClick(fixture.player().containerMenu.containerId, 0), fixture.player());
    }

    private static void assertRejected(Fixture fixture) {
        require(ItemStack.matches(fixture.engine().getSocket(0), fixture.board()), "Rejected removal changed the installed board");
        require(fixture.iron().getDamageValue() == 0 && returnedBoards(fixture) == 0, "Rejected removal consumed durability or returned a board");
    }

    private static int returnedBoards(Fixture fixture) {
        int count = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            var stack = fixture.player().getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, fixture.board())) count += stack.getCount();
        }
        return count;
    }

    private record Fixture(PeatEngineBlockEntity engine, ServerPlayer player, ItemStack board, ItemStack iron) {}
}
