package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.carriers.players.POBoxInfo;
import forestry.mail.gui.LetterMenu;
import forestry.mail.gui.LetterScreen;
import forestry.mail.gui.MailboxInfoToast;
import forestry.mail.gui.MailboxScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class PostalClientTest {
	private static int ticks;
	private static int worldTicks;
	private static int sinceStep;
	private static int step;
	private static String response = "";
	private static boolean finished;

	@SubscribeEvent
	public static void chat(ClientChatReceivedEvent event) {
		if (Boolean.getBoolean("forestry.lifecycle.postalClient") && event.getMessage().getString().startsWith("POSTAL_")) {
			response = event.getMessage().getString();
		}
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) throws Exception {
		if (!Boolean.getBoolean("forestry.lifecycle.postalClient") || finished) return;
		var client = Minecraft.getInstance();
		if (++ticks > 3600) throw new AssertionError("Postal client timed out, step=" + step + ", screen=" + client.gui.screen());
		if (client.level == null || client.player == null) {
			if (worldTicks > 0) throw new AssertionError("Postal client disconnected during step " + step);
			return;
		}
		worldTicks++;
		sinceStep++;
		if (step == 0 && worldTicks >= 30) {
			PostalTests.check(client.getSingleplayerServer() == null, "Must use a separate dedicated server");
			PostalTests.check(ModList.get().isLoaded("jade"), "Local Jade loaded independently");
			if (Boolean.getBoolean("forestry.lifecycle.postalReconnect")) {
				assertToast(client);
				screenshot(client, "postal-reconnect.png");
				command(client, "mailbox", 4);
			} else command(client, "rollback", 1);
		} else if (step == 1 && response.equals("POSTAL_ROLLBACK_OK")) {
			// Empty-login toasts expire before this check; a rolled-back delivery must not create a new one.
			PostalTests.check(client.gui.toastManager().getToast(MailboxInfoToast.class, Toast.NO_TOKEN) == null, "Rollback emits no mail toast");
			command(client, "commit", 2);
		} else if (step == 2 && response.equals("POSTAL_COMMIT_OK")) {
			assertToast(client);
			step = 3;
			sinceStep = 0;
		} else if (step == 3 && sinceStep >= 20) {
			screenshot(client, "postal-toast-jade.png");
			command(client, "mailbox", 4);
		} else if (step == 4 && sinceStep >= 20 && client.gui.screen() instanceof MailboxScreen screen) {
			long letters = screen.getMenu().slots.stream().filter(slot -> forestry.mail.letters.LetterUtils.isLetter(slot.getItem())).count();
			PostalTests.check(letters == 2, "Mailbox GUI receives both real letter components over network");
			screenshot(client, "postal-mailbox.png");
			command(client, "letter", 7);
		} else if (step == 7 && response.equals("POSTAL_LETTER_EQUIPPED")) {
			PostalTests.check(forestry.mail.letters.LetterUtils.isLetter(client.player.getMainHandItem()), "Letter synchronized before use");
			client.gameMode.useItem(client.player, net.minecraft.world.InteractionHand.MAIN_HAND);
			step = 5;
			sinceStep = 0;
		} else if (step == 5 && sinceStep >= 20 && client.gui.screen() instanceof LetterScreen) {
			var letter = ((LetterMenu) client.player.containerMenu).getLetter();
			PostalTests.check(letter.isProcessed() && letter.getAttachments().stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum() == 4, "Trade letter GUI decodes real goods");
			screenshot(client, "postal-trade-letter.png");
			command(client, "offhand", 8);
		} else if (step == 8 && response.equals("POSTAL_OFFHAND_EQUIPPED")) {
			PostalTests.check(forestry.mail.letters.LetterUtils.isLetter(client.player.getOffhandItem()), "Offhand letter synchronized before use");
			client.gameMode.useItem(client.player, net.minecraft.world.InteractionHand.OFF_HAND);
			step = 9;
			sinceStep = 0;
		} else if (step == 9 && sinceStep >= 20 && client.gui.screen() instanceof LetterScreen) {
			var letter = ((LetterMenu) client.player.containerMenu).getLetter();
			PostalTests.check(letter.isProcessed() && letter.countAttachments() == 3, "Offhand letter GUI uses the correct hand");
			screenshot(client, "postal-offhand-letter.png");
			command(client, "draft", 10);
		} else if (step == 10 && response.equals("POSTAL_DRAFT_EQUIPPED")) {
			client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
			step = 11;
			sinceStep = 0;
		} else if (step == 11 && sinceStep >= 20 && client.gui.screen() instanceof LetterScreen screen) {
			PostalTests.check(!screen.getMenu().getLetter().isProcessed(), "Trade preview uses an unsent draft");
			screen.getMenu().setCarrier(PostalCarriers.TRADER.value());
			// Enter the address only on the first opening, through the production text field's responder.
			recipientField(screen).setValue("NetworkShop");
			step = 12;
			sinceStep = 0;
		} else if (step == 12 && sinceStep >= 20 && client.gui.screen() instanceof LetterScreen screen
			&& (screen.getMenu().getTradeInfo() != null || sinceStep >= 100)) {
			assertTradePreview(screen, 2, 3, "Initial draft");
			screen.onClose();
			step = 16;
			sinceStep = 0;
		} else if (step == 16 && sinceStep >= 20) {
			// Let the container-close packet reach the server before issuing the fixture command.
			PostalTests.check(client.gui.screen() == null && client.player.containerMenu == client.player.inventoryMenu,
				"Draft screen and menu are closed");
			command(client, "draftClosed", 13);
		} else if (step == 13 && response.equals("POSTAL_DRAFT_CLOSED") && sinceStep >= 20) {
			PostalTests.check(client.player.containerMenu == client.player.inventoryMenu, "Draft menu actually closed before reopening");
			client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
			step = 14;
			sinceStep = 0;
		} else if (step == 14 && sinceStep >= 20 && client.gui.screen() instanceof LetterScreen screen
			&& (screen.getMenu().getTradeInfo() != null || sinceStep >= 100)) {
			// No address edits, focus changes, or carrier changes after reopening.
			assertTradePreview(screen, 4, 5, "Reopened draft without editing recipient");
			screenshot(client, "postal-reopened-trade-preview.png");
			LogUtils.getLogger().info("FORESTRY_POSTAL_TRADE_REOPEN_READY recipient_unchanged=true refreshed_offer=true");
			command(client, "draftReopened", 15);
		} else if (step == 15 && response.equals("POSTAL_DRAFT_REOPENED_OK")) {
			command(client, "done", 6);
		} else if (step == 6 && response.equals("POSTAL_DONE_OK")) {
			finished = true;
			LogUtils.getLogger().info("FORESTRY_POSTAL_CLIENT_READY reconnect={} jade=true toast=true mailbox_gui=true letter_gui=true offhand=true trade_reopen=true", Boolean.getBoolean("forestry.lifecycle.postalReconnect"));
			client.stop();
		}
	}

	private static EditBox recipientField(LetterScreen screen) {
		return screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
	}

	private static void assertTradePreview(LetterScreen screen, int goods, int payment, String scenario) {
		PostalTests.check(recipientField(screen).getValue().equals("NetworkShop"), scenario + ": recipient text is retained");
		PostalTests.check(screen.getMenu().getCarrier() == PostalCarriers.TRADER.value(), scenario + ": trader carrier is retained");
		var info = screen.getMenu().getTradeInfo();
		PostalTests.check(info != null, scenario + ": server trade information loads automatically");
		PostalTests.check(info.address().getName().equals("NetworkShop") && info.state().isOk(), scenario + ": valid trade preview is displayed");
		PostalTests.check(info.tradegood().is(Items.DIAMOND) && info.tradegood().getCount() == goods, scenario + ": current offered goods are displayed");
		PostalTests.check(info.required().size() == 1 && info.required().getFirst().is(Items.EMERALD)
			&& info.required().getFirst().getCount() == payment, scenario + ": current payment is displayed");
	}

	private static void command(Minecraft client, String command, int next) {
		response = "";
		step = next;
		sinceStep = 0;
		client.getConnection().sendCommand("postaltest " + command);
	}

	private static void assertToast(Minecraft client) throws Exception {
		var toast = client.gui.toastManager().getToast(MailboxInfoToast.class, Toast.NO_TOKEN);
		PostalTests.check(toast != null, "Real postal packet creates a mail toast");
		// Inspect the rendered toast, not a substitute packet handler or a test-only production hook.
		var field = MailboxInfoToast.class.getDeclaredField("poBox");
		field.setAccessible(true);
		PostalTests.check(field.get(toast).equals(new POBoxInfo(1, 1)), "Toast has actual player/trade counts");
	}

	private static void screenshot(Minecraft client, String name) {
		Screenshot.grab(client.gameDirectory, name, client.gameRenderer.mainRenderTarget(), 1, message -> {});
	}
}
