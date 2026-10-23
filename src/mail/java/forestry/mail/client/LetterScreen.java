package forestry.mail.client;

import forestry.api.ForestryConstants;
import forestry.core.platform.util.NetworkUtil;
import forestry.mail.gui.LetterMenu;
import forestry.mail.network.packets.PacketLetterInfoRequest;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native menu for the server-owned letter inventory. The original artwork and slot layout are retained. */
public final class LetterScreen extends AbstractContainerScreen<LetterMenu> {
    private EditBox recipient;
    private MultiLineEditBox text;
    private int recipientDelay;

    public LetterScreen(LetterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 223);
    }

    @Override protected void init() {
        super.init();
        boolean writable = !this.menu.getLetter().isProcessed();
        this.recipient = addRenderableWidget(new EditBox(this.font, this.leftPos + 17, this.topPos + 16, 125, 16, Component.translatable("for.gui.mail.to")));
        this.recipient.setMaxLength(64);
        this.recipient.setValue(this.menu.getLetter().getRecipientString());
        this.recipient.setEditable(writable);
        this.recipient.setResponder(value -> this.recipientDelay = 10);
        var carrier = addRenderableWidget(Button.builder(Component.translatable(this.menu.getCarrier().getTranslationKey()), button -> {
            this.menu.advanceCarrierType();
            button.setMessage(Component.translatable(this.menu.getCarrier().getTranslationKey()));
            requestRecipient();
        }).bounds(this.leftPos + 17, this.topPos + 34, 125, 16).build());
        carrier.active = writable;
        this.text = addRenderableWidget(MultiLineEditBox.builder().setX(this.leftPos + 17).setY(this.topPos + 52)
            .build(this.font, 125, 37, Component.translatable("item.forestry.letter")));
        this.text.setCharacterLimit(128);
        this.text.setValue(this.menu.getText());
        this.text.active = writable;
        this.text.setValueListener(value -> { if (writable) this.menu.setText(value); });
    }

    private void requestRecipient() {
        if (!this.menu.getLetter().isProcessed()) NetworkUtil.sendToServer(new PacketLetterInfoRequest(this.menu.containerId, this.recipient.getValue(), this.menu.getCarrier()));
        this.recipientDelay = 0;
    }

    @Override protected void containerTick() {
        if (this.recipientDelay > 0 && --this.recipientDelay == 0) requestRecipient();
    }

    @Override public void onClose() {
        if (this.recipientDelay > 0) requestRecipient();
        super.onClose();
    }

    @Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ForestryConstants.forestry("textures/gui/letter.png"), this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}
}
