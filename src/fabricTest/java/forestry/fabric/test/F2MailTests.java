package forestry.fabric.test;

import com.mojang.authlib.GameProfile;
import forestry.mail.MailFabricModule;
import forestry.mail.carriers.players.POBox;
import forestry.mail.carriers.players.POBoxRegistry;
import forestry.mail.carriers.trading.TradeStation;
import forestry.mail.features.MailDataComponents;
import forestry.mail.gui.LetterMenu;
import forestry.mail.inventory.LetterInventory;
import forestry.mail.letters.*;
import forestry.mail.network.packets.PacketLetterTextSet;
import forestry.mail.postoffice.PostOffice;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static forestry.fabric.test.F1RegistrationTests.require;

public final class F2MailTests {
    public static void openNamedTradeStation(net.minecraft.server.MinecraftServer server, net.minecraft.core.BlockPos pos) {
        var player = server.getPlayerList().getPlayers().getFirst();
        player.closeContainer();
        var tile = (forestry.mail.tradestation.TradeStationBlockEntity) server.overworld().getBlockEntity(pos);
        tile.getOwnerHandler().setOwner(player.getGameProfile());
        require(tile.handleSetAddressRequest("F3TradeStation"), "Trade station naming failed");
        require(player.openMenu(tile).isPresent(), "Linked trade station menu failed");
    }

    @GameTest public void catalogAndTradeStationRecipesPreserveComponents(GameTestHelper test) {
        var manager = test.getLevel().getServer().getRecipeManager();
        var registry = test.getLevel().registryAccess();
        var catalog = manager.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
            F1RegistrationTests.id("catalog"))).orElseThrow();
        require(((net.minecraft.world.item.crafting.CraftingRecipe) catalog.value()).assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY).is(MailFabricModule.CATALOG.item()), "Catalog recipe missing");
        var station = (net.minecraft.world.item.crafting.ShapedRecipe) manager.byKey(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.RECIPE, F1RegistrationTests.id("trade_station"))).orElseThrow().value();
        var boardRecipe = (forestry.api.core.machines.ICarpenterRecipe) manager.byKey(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.RECIPE, F1RegistrationTests.id("carpenter/circuits/refined"))).orElseThrow().value();
        var ingredient = station.getIngredients().get(7).orElseThrow();
        var board = boardRecipe.getResultItem(registry);
        require(ingredient.test(board), "Trade station rejected a freshly crafted refined board");
        var renamed = board.copy();
        renamed.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Changed"));
        require(!ingredient.test(renamed), "Strict board ingredient ignored an extra component");
        var programmed = board.copy();
        var tag = programmed.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        tag.putString("CA.I0", "forestry:board_programmed");
        programmed.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        require(!ingredient.test(programmed), "Trade station consumed a programmed board");
        var player = FakePlayer.get(test.getLevel());
        require(MailFabricModule.CATALOG.item().getContainer(3, player, MailFabricModule.CATALOG.stack()) instanceof forestry.mail.gui.CatalogMenu,
            "Catalog item no longer opens its business menu");
        test.succeed();
    }

    private static boolean enabled(GameTestHelper test) {
        if (FabricLoader.getInstance().isModLoaded("forestrymail")) return true;
        test.succeed();
        return false;
    }
    private static MailAddress address(String name) { return new MailAddress(new GameProfile(UUID.nameUUIDFromBytes((name + ProcessHandle.current().pid()).getBytes(java.nio.charset.StandardCharsets.UTF_8)), name)); }
    private static ItemStack stamp(EnumStampDefinition value, int count) { return MailFabricModule.STAMPS.stack(value, count); }

    @GameTest public void physicalMailboxLookupCommitsExactlyOneDelivery(GameTestHelper test) {
        if (!enabled(test)) return;
        var pos = test.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        F2MachineTests.place(test.getLevel(), pos, "mailbox");
        var mailbox = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(test.getLevel(), pos, net.minecraft.core.Direction.NORTH);
        require(mailbox != null, "Mailbox does not expose Fabric ItemStorage");
        var recipient = address("F2PipeDelivery");
        var letter = new Letter(address("F2PipeSender"), recipient); letter.addStamps(stamp(EnumStampDefinition.P_5, 1));
        letter.addAttachment(new ItemStack(Items.DIAMOND, 2));
        var variant = net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(LetterUtils.createLetterStack(letter));
        try (var outer = Transaction.openOuter()) {
            try (var inner = outer.openNested()) { require(mailbox.insert(variant, 64, inner) == 1, "Mailbox did not accept exactly one letter"); inner.commit(); }
        }
        var boxes = POBoxRegistry.getOrCreate(test.getLevel());
        require(boxes.getPOBox(recipient) == null, "Canceled automation created a mailbox delivery");
        try (var tx = Transaction.openOuter()) { require(mailbox.insert(variant, 1, tx) == 1, "Mailbox delivery failed"); tx.commit(); }
        require(boxes.getPOBox(recipient).getPOBoxInfo().playerLetters() == 1, "Committed automation duplicated or lost the letter");
        var delivered = LetterUtils.getLetterData(boxes.getPOBox(recipient).getItem(0));
        require(delivered.isProcessed() && delivered.getItem(0).getCount() == 2, "Delivery changed attachments or processing state");
        test.succeed();
    }

    @GameTest public void originalPostageAndVaultRegressions(GameTestHelper test) {
        if (!enabled(test)) return;
        for (var value : EnumStampDefinition.VALUES) require(PostageUtil.getPostage(stamp(value, 1)) == value.getPostage(), "Postage data map lost " + value);
        require(PostageUtil.sumPostage(java.util.List.of(stamp(EnumStampDefinition.P_10, 3), stamp(EnumStampDefinition.P_1, 4), new ItemStack(Items.PAPER))) == 34, "Postage counting changed");
        var office = new PostOffice();
        office.collectPostage(NonNullList.of(ItemStack.EMPTY, stamp(EnumStampDefinition.P_10, 4), stamp(EnumStampDefinition.P_1, 7)));
        var original = office.save(new CompoundTag(), test.getLevel().registryAccess());
        office.setDirty(false);
        try (var tx = Transaction.openOuter()) {
            require(office.getAnyStamp(1).is(MailFabricModule.STAMPS.item(EnumStampDefinition.P_1)), "Cheapest-first withdrawal changed");
            require(!office.isDirty(), "Vault dirtied before root commit");
        }
        require(original.equals(office.save(new CompoundTag(), test.getLevel().registryAccess())) && !office.isDirty(), "Canceled withdrawal leaked");
        office.getAnyStamp(1);
        require(office.isDirty(), "Withdrawal not marked for persistence");
        var restored = new PostOffice(office.save(new CompoundTag(), test.getLevel().registryAccess()));
        require(restored.getAnyStamp(MailFabricModule.STAMPS.item(EnumStampDefinition.P_1), 64).getCount() == 6, "Saved withdrawal duplicated stamps");
        var legacy = new CompoundTag(); legacy.putInt("CPS1", 5); legacy.putInt("CPS7", 2);
        restored = new PostOffice(legacy);
        require(restored.getAnyStamp(64).getCount() == 5 && restored.getAnyStamp(64).getCount() == 2, "Legacy denominations did not migrate");
        test.succeed();
    }

    @GameTest public void deliveryCommitsMailboxAndPostageTogether(GameTestHelper test) {
        if (!enabled(test)) return;
        var registry = POBoxRegistry.getOrCreate(test.getLevel());
        require(registry == POBoxRegistry.getOrCreate(test.getLevel()), "PO box cache identity lost");
        var recipient = address("F2Delivery");
        var letter = new Letter(address("F2Sender"), recipient);
        letter.setText("transaction");
        letter.addAttachment(new ItemStack(Items.DIAMOND, 3));
        letter.addStamps(stamp(EnumStampDefinition.P_5, 1));
        var stack = LetterUtils.createLetterStack(letter);
        var office = new PostOffice();
        registry.setDirty(false);
        try (var tx = Transaction.openOuter()) {
            require(office.lodgeLetter(test.getLevel(), stack, tx).isOk(), "Valid delivery rejected");
            require(!registry.isDirty(), "Mailbox notification before root commit");
        }
        require(registry.getPOBox(recipient) == null && office.getAnyStamp(64).isEmpty(), "Canceled delivery left mailbox or postage");
        try (var tx = Transaction.openOuter()) {
            require(office.lodgeLetter(test.getLevel(), stack, tx).isOk(), "Committed delivery rejected");
            tx.commit();
        }
        var box = registry.getPOBox(recipient);
        require(box != null && box.getPOBoxInfo().playerLetters() == 1 && registry.isDirty(), "Delivery not saved");
        var delivered = LetterUtils.getLetterData(box.getItem(0));
        require(delivered.isProcessed() && delivered.getPostage().stream().allMatch(ItemStack::isEmpty) && delivered.getItem(0).getCount() == 3, "Delivered payload or stamp invalidation changed");
        require(!LetterUtils.getLetterData(stack).isProcessed(), "Delivery mutated caller's source stack");
        test.succeed();
    }

    @GameTest public void fullMailboxRejectsWithoutCollectingPostage(GameTestHelper test) {
        if (!enabled(test)) return;
        var recipient = address("F2FullBox");
        var box = POBoxRegistry.getOrCreate(test.getLevel()).getOrCreatePOBox(recipient);
        for (int i = 0; i < box.getContainerSize(); i++) box.setItem(i, new ItemStack(Items.PAPER, 64));
        var letter = new Letter(address("F2FullSender"), recipient);
        letter.addStamps(stamp(EnumStampDefinition.P_1, 1));
        var office = new PostOffice();
        try (var tx = Transaction.openOuter()) {
            require(!office.lodgeLetter(test.getLevel(), LetterUtils.createLetterStack(letter), tx).isOk(), "Full mailbox accepted mail");
            tx.commit();
        }
        require(office.getAnyStamp(64).isEmpty(), "Failed delivery charged postage");
        test.succeed();
    }

    @GameTest public void paidTradeRollsBackStockPaymentAndReply(GameTestHelper test) {
        if (!enabled(test)) return;
        var buyer = address("F2Buyer");
        var shop = new MailAddress("F2Shop");
        var station = new TradeStation(null, shop);
        station.setItem(TradeStation.SLOT_TRADEGOOD, new ItemStack(Items.DIAMOND));
        station.setItem(TradeStation.SLOT_EXCHANGE_1, new ItemStack(Items.EMERALD, 2));
        station.setItem(TradeStation.SLOT_SEND_BUFFER, new ItemStack(Items.DIAMOND, 8));
        station.setItem(TradeStation.SLOT_LETTERS_1, new ItemStack(Items.PAPER, 4));
        station.setItem(TradeStation.SLOT_STAMPS_1, stamp(EnumStampDefinition.P_5, 4));
        var order = new Letter(buyer, shop); order.addAttachment(new ItemStack(Items.EMERALD, 4));
        var original = station.save(new CompoundTag(), test.getLevel().registryAccess());
        var notifications = new AtomicInteger(); station.registerUpdateWatcher(notifications::incrementAndGet);
        try (var tx = Transaction.openOuter()) {
            require(station.handleLetter(test.getLevel(), shop, LetterUtils.createLetterStack(order), tx).isOk(), "Valid trade rejected");
        }
        require(original.equals(station.save(new CompoundTag(), test.getLevel().registryAccess())) && notifications.get() == 0, "Aborted trade changed stock or notified");
        require(POBoxRegistry.getOrCreate(test.getLevel()).getPOBox(buyer) == null, "Aborted trade delivered reply");
        try (var tx = Transaction.openOuter()) {
            require(station.handleLetter(test.getLevel(), shop, LetterUtils.createLetterStack(order), tx).isOk(), "Committed trade rejected"); tx.commit();
        }
        require(station.getItem(TradeStation.SLOT_SEND_BUFFER).getCount() == 6 && station.getItem(TradeStation.SLOT_RECEIVE_BUFFER).getCount() == 4, "Trade resource count wrong");
        require(POBoxRegistry.getOrCreate(test.getLevel()).getPOBox(buyer).getPOBoxInfo().tradeLetters() == 1 && notifications.get() > 0, "Trade reply/notification missing");
        test.succeed();
    }

    @GameTest public void letterComponentRoundTripRejectsMalformedData(GameTestHelper test) {
        if (!enabled(test)) return;
        var letter = new Letter(address("F2Codec"), address("F2CodecRecipient")); letter.setText("component data"); letter.addAttachment(new ItemStack(Items.APPLE, 7));
        var stack = LetterUtils.createLetterStack(letter);
        var ops = test.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var saved = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        require(ItemStack.matches(stack, ItemStack.CODEC.parse(ops, saved).getOrThrow()), "Letter component not preserved");
        var encoded = (CompoundTag) Letter.CODEC.encodeStart(ops, letter).getOrThrow();
        encoded.putString("text", "x".repeat(129));
        require(Letter.CODEC.parse(ops, encoded).error().isPresent(), "Oversized letter accepted");
        var addressTag = (CompoundTag) MailAddress.CODEC.encodeStart(ops, address("F2Codec")).getOrThrow();
        addressTag.putString("carrier", "forestry:unknown");
        require(MailAddress.CODEC.parse(ops, addressTag).error().isPresent(), "Unknown carrier silently remapped");
        test.succeed();
    }

    @GameTest public void staleMenuAndRemovedItemCannotEditLetter(GameTestHelper test) {
        if (!enabled(test)) return;
        var player = FakePlayer.get(test.getLevel(), new GameProfile(UUID.fromString("e7a12f56-8797-4e99-8d1f-a77822210004"), "F2MailEditor"));
        var oldMenu = player.containerMenu; var oldHand = player.getMainHandItem();
        try {
            var stack = new ItemStack(MailFabricModule.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.FRESH));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var inventory = new LetterInventory(player, stack);
            var menu = new LetterMenu(5, player, inventory); player.containerMenu = menu;
            PacketLetterTextSet.handle(new PacketLetterTextSet(4, "wrong window"), player);
            require(menu.getText().isEmpty(), "Stale window edited letter");
            PacketLetterTextSet.handle(new PacketLetterTextSet(5, "accepted"), player);
            require(menu.getText().equals("accepted"), "Valid editor rejected");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            PacketLetterTextSet.handle(new PacketLetterTextSet(5, "removed"), player);
            require(menu.getText().equals("accepted"), "Removed letter still editable");
        } finally { player.containerMenu = oldMenu; player.setItemInHand(InteractionHand.MAIN_HAND, oldHand); }
        test.succeed();
    }
}
