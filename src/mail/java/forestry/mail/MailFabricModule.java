package forestry.mail;

import forestry.api.modules.*;
import forestry.core.platform.registration.FeatureRegistry;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.ItemStamp;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

public final class MailFabricModule implements IForestryModule {
    public static forestry.core.platform.registration.FeatureItemGroup<ItemStamp, EnumStampDefinition> STAMPS;
    public static forestry.core.platform.registration.FeatureItemTable<forestry.mail.letters.LetterItem, forestry.mail.letters.LetterItem.Size, forestry.mail.letters.LetterItem.State> LETTERS;
    public static forestry.core.platform.registration.FeatureMenuType<forestry.mail.gui.LetterMenu> LETTER_MENU;
    public static forestry.core.platform.registration.FeatureItem<forestry.mail.tradestation.CatalogItem> CATALOG;
    @Override public Identifier getId() { return ForestryModuleIds.MAIL; }
    @Override public String getOwnerModId() { return "forestrymail"; }
    @Override public List<Identifier> getModuleDependencies() { return List.of(ForestryModuleIds.CORE); }
    @Override public void register() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registries, selection) ->
            dispatcher.register(net.minecraft.commands.Commands.literal("forestry").then(forestry.mail.commands.MailCommand.register())));
        FeatureRegistry registry = forestry.core.platform.registration.ModFeatureRegistry.get(getId());
        STAMPS = registry.itemGroup(ItemStamp::new, "stamp", EnumStampDefinition.VALUES);
        LETTERS = registry.itemTable(forestry.mail.letters.LetterItem::new, forestry.mail.letters.LetterItem.Size.values(), forestry.mail.letters.LetterItem.State.values(), "letter");
        LETTER_MENU = registry.menuType(forestry.mail.gui.LetterMenu::fromNetwork, "letter");
        CATALOG = registry.item(forestry.mail.tradestation.CatalogItem::new, "catalog");
        forestry.mail.features.MailDataComponents.LETTER_DATA.getId();
        forestry.mail.features.MailBlocks.BASE.getFeatures();
        forestry.mail.features.MailMenuTypes.MAILBOX.getName();
        registry.addRegistryListener(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, () ->
            net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (tile, side) -> tile.isRemoved() ? null : tile.getAutomatedMailHandler(), forestry.mail.features.MailBlockEntities.MAILBOX.tileType()));
        forestry.mail.carriers.PostalCarriers.PLAYER.getId();
        registry.creativeTab("mail", tab -> tab.icon(() -> STAMPS.getItems().getFirst().getDefaultInstance())
            .displayItems((parameters, output) -> {
                STAMPS.getItems().forEach(output::accept);
                LETTERS.getBlocks().forEach(output::accept);
                output.accept(CATALOG.stack());
                forestry.mail.features.MailBlocks.BASE.getFeatures().forEach(feature -> output.accept(feature.stack()));
            }));
        var packets = forestry.core.platform.network.PacketRegistry.INSTANCE;
        packets.serverbound(forestry.mail.network.packets.PacketTraderAddressRequest.TYPE, forestry.mail.network.packets.PacketTraderAddressRequest::encode, forestry.mail.network.packets.PacketTraderAddressRequest::decode, forestry.mail.network.packets.PacketTraderAddressRequest::handle);
        packets.clientbound(forestry.mail.network.packets.PacketTraderAddressResponse.TYPE, forestry.mail.network.packets.PacketTraderAddressResponse::encode, forestry.mail.network.packets.PacketTraderAddressResponse::decode, forestry.mail.network.packets.PacketTraderAddressResponse::handle);
        packets.clientbound(forestry.mail.network.packets.PacketPOBoxInfoResponse.TYPE, forestry.mail.network.packets.PacketPOBoxInfoResponse::encode, forestry.mail.network.packets.PacketPOBoxInfoResponse::decode, forestry.mail.network.packets.PacketPOBoxInfoResponse::handle);
        packets.clientbound(forestry.mail.network.packets.PacketLetterInfoResponsePlayer.TYPE, forestry.mail.network.packets.PacketLetterInfoResponsePlayer::encode, forestry.mail.network.packets.PacketLetterInfoResponsePlayer::decode, forestry.mail.network.packets.PacketLetterInfoResponsePlayer::handle);
        packets.clientbound(forestry.mail.network.packets.PacketLetterInfoResponseTrader.TYPE, forestry.mail.network.packets.PacketLetterInfoResponseTrader::encode, forestry.mail.network.packets.PacketLetterInfoResponseTrader::decode, forestry.mail.network.packets.PacketLetterInfoResponseTrader::handle);
        packets.serverbound(forestry.mail.network.packets.PacketLetterTextSet.TYPE, forestry.mail.network.packets.PacketLetterTextSet::encode, forestry.mail.network.packets.PacketLetterTextSet::decode, forestry.mail.network.packets.PacketLetterTextSet::handle);
        packets.serverbound(forestry.mail.network.packets.PacketLetterInfoRequest.TYPE, forestry.mail.network.packets.PacketLetterInfoRequest::encode, forestry.mail.network.packets.PacketLetterInfoRequest::decode, forestry.mail.network.packets.PacketLetterInfoRequest::handle);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            var box = forestry.mail.carriers.players.POBoxRegistry.getOrCreate(player.level()).getOrCreatePOBox(new forestry.mail.letters.MailAddress(player.getGameProfile()));
            sender.sendPacket(new forestry.mail.network.packets.PacketPOBoxInfoResponse(box.getPOBoxInfo(), false));
        });
    }
}
