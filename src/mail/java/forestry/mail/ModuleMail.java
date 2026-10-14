package forestry.mail;

import forestry.mail.network.MailPacketIds;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import forestry.api.ForestryDataMaps;
import forestry.api.client.IClientModuleHandler;
import forestry.api.mail.IMailAddress;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.modules.IPacketRegistry;
import forestry.core.platform.util.NetworkUtil;
import forestry.mail.carriers.players.POBox;
import forestry.mail.carriers.players.POBoxRegistry;
import forestry.mail.client.MailClientHandler;
import forestry.mail.commands.MailCommand;
import forestry.mail.features.MailBlockEntities;
import forestry.mail.network.packets.*;
import forestry.modules.BlankForestryModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Consumer;
import forestry.mail.letters.MailAddress;

@ForestryModule
public class ModuleMail extends BlankForestryModule {
	@Override
	public Identifier getId() {
		return ForestryModuleIds.MAIL;
	}

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ModuleMail::registerCapabilities);
		modBus.addListener(ModuleMail::registerDataMaps);
		NeoForge.EVENT_BUS.addListener(ModuleMail::handlePlayerLoggedIn);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, MailBlockEntities.MAILBOX.tileType(), (tile, side) -> tile.getAutomatedMailHandler());
	}

	private static void registerDataMaps(RegisterDataMapTypesEvent event) {
		event.register(ForestryDataMaps.POSTAGE);
	}

	public static void handlePlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) {
			return;
		}

		IMailAddress address = new MailAddress(player.getGameProfile());
		POBox pobox = POBoxRegistry.getOrCreate((ServerLevel) player.level()).getOrCreatePOBox(address);
		PacketPOBoxInfoResponse packet = new PacketPOBoxInfoResponse(pobox.getPOBoxInfo(), false);
		NetworkUtil.sendToPlayer(packet, (ServerPlayer) player);
	}

	@Override
	public void addToRootCommand(LiteralArgumentBuilder<CommandSourceStack> command) {
		command.then(MailCommand.register());
	}

	@Override
	public void registerPackets(IPacketRegistry registry) {
		registry.serverbound(MailPacketIds.LETTER_INFO_REQUEST, PacketLetterInfoRequest::encode, PacketLetterInfoRequest::decode, PacketLetterInfoRequest::handle);
		registry.serverbound(MailPacketIds.TRADING_ADDRESS_REQUEST, PacketTraderAddressRequest::encode, PacketTraderAddressRequest::decode, PacketTraderAddressRequest::handle);
		registry.serverbound(MailPacketIds.LETTER_TEXT_SET, PacketLetterTextSet::encode, PacketLetterTextSet::decode, PacketLetterTextSet::handle);

		registry.clientbound(MailPacketIds.LETTER_INFO_RESPONSE_PLAYER, PacketLetterInfoResponsePlayer::encode, PacketLetterInfoResponsePlayer::decode, PacketLetterInfoResponsePlayer::handle);
		registry.clientbound(MailPacketIds.LETTER_INFO_RESPONSE_TRADER, PacketLetterInfoResponseTrader::encode, PacketLetterInfoResponseTrader::decode, PacketLetterInfoResponseTrader::handle);
		registry.clientbound(MailPacketIds.TRADING_ADDRESS_RESPONSE, PacketTraderAddressResponse::encode, PacketTraderAddressResponse::decode, PacketTraderAddressResponse::handle);
		registry.clientbound(MailPacketIds.POBOX_INFO_RESPONSE, PacketPOBoxInfoResponse::encode, PacketPOBoxInfoResponse::decode, PacketPOBoxInfoResponse::handle);
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new MailClientHandler());
	}
}
