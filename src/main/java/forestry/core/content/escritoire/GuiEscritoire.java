package forestry.core.content.escritoire;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.widgets.GameTokenWidget;
import forestry.core.platform.gui.widgets.ProbeButton;
import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.render.ColourProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.FormattedCharSequence;
import forestry.core.platform.gui.GuiForestry;
import forestry.core.platform.gui.GuiUtil;

import java.util.List;

public class GuiEscritoire extends GuiForestry<ContainerEscritoire> {
	private static final int NOTES_X = 170;
	private static final int NOTES_Y = 23;
	private static final int NOTES_WIDTH = 46;
	private static final int NOTES_HEIGHT = 49;
	private final ItemStack LEVEL_ITEM = new ItemStack(Items.PAPER);
	private final EscritoireTextSource textSource = new EscritoireTextSource();
	private final TileEscritoire tile;
	private int noteScroll;
	private String lastNote = "";

	public GuiEscritoire(ContainerEscritoire container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/escritoire.png", container, inv, title, 228, 235);

		this.tile = container.getTile();

		this.widgetManager.add(new ProbeButton(this, this.widgetManager, 14, 16));

		EscritoireGame game = this.tile.getGame();

		// Inner ring
		addTokenWidget(game, 115, 51, 0);
		addTokenWidget(game, 115, 77, 1);
		addTokenWidget(game, 94, 90, 2);
		addTokenWidget(game, 73, 77, 3);
		addTokenWidget(game, 73, 51, 4);
		addTokenWidget(game, 94, 38, 5);

		// Outer ring
		addTokenWidget(game, 115, 25, 6);
		addTokenWidget(game, 136, 38, 7);
		addTokenWidget(game, 136, 64, 8);

		addTokenWidget(game, 136, 90, 9);
		addTokenWidget(game, 115, 103, 10);
		addTokenWidget(game, 94, 116, 11);

		addTokenWidget(game, 73, 103, 12);
		addTokenWidget(game, 52, 90, 13);
		addTokenWidget(game, 52, 64, 14);

		addTokenWidget(game, 52, 38, 15);
		addTokenWidget(game, 73, 25, 16);
		addTokenWidget(game, 94, 12, 17);

		// Corners
		addTokenWidget(game, 52, 12, 18);
		addTokenWidget(game, 136, 12, 19);
		addTokenWidget(game, 52, 116, 20);
		addTokenWidget(game, 136, 116, 21);
	}

	private void addTokenWidget(EscritoireGame game, int x, int y, int index) {
		Widget gameTokenWidget = new GameTokenWidget(game, this.widgetManager, x, y, index);
        this.widgetManager.add(gameTokenWidget);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		for (int i = 0; i <= this.tile.getGame().getBountyLevel() / 4; i++) {
			GuiUtil.drawItemStack(graphics, this, this.LEVEL_ITEM, this.leftPos + 170 + i * 8, this.topPos + 7);
		}

		List<FormattedCharSequence> lines = noteLines();
		int visible = visibleNoteLines();
		int x = this.leftPos + NOTES_X;
		int y = this.topPos + NOTES_Y;
		int color = 0xFF000000 | ColourProperties.INSTANCE.get("gui.mail.lettertext");
		// Keep the native font size: shrinking Chinese glyphs loses entire strokes.
		for (int row = 0; row < visible && this.noteScroll + row < lines.size(); row++) {
			graphics.text(this.font, lines.get(this.noteScroll + row), x, y + row * (this.font.lineHeight + 1), color, false);
		}
		if (lines.size() > visible) {
			int thumbHeight = Math.max(6, NOTES_HEIGHT * visible / lines.size());
			int thumbY = y + (NOTES_HEIGHT - thumbHeight) * this.noteScroll / (lines.size() - visible);
			graphics.fill(x + NOTES_WIDTH + 1, y, x + NOTES_WIDTH + 3, y + NOTES_HEIGHT, 0xFF282020);
			graphics.fill(x + NOTES_WIDTH + 1, thumbY, x + NOTES_WIDTH + 3, thumbY + thumbHeight, color);
		}
	}

	private Component note() {
		return Component.empty()
			.append(Component.translatable("for.gui.escritoire.attempt.number", EscritoireGame.BOUNTY_MAX - this.tile.getGame().getBountyLevel()).withStyle(ChatFormatting.UNDERLINE))
			.append("\n").append(this.textSource.getText(this.tile.getGame()));
	}

	private List<FormattedCharSequence> noteLines() {
		Component note = note();
		if (!note.getString().equals(this.lastNote)) {
			this.lastNote = note.getString();
			this.noteScroll = 0;
		}
		var lines = this.font.split(note, NOTES_WIDTH);
		this.noteScroll = Math.min(this.noteScroll, Math.max(0, lines.size() - visibleNoteLines()));
		return lines;
	}

	private int visibleNoteLines() {
		return Math.max(1, (NOTES_HEIGHT + 1) / (this.font.lineHeight + 1));
	}

	private boolean isOverNotes(double mouseX, double mouseY) {
		return mouseX >= this.leftPos + NOTES_X && mouseX < this.leftPos + NOTES_X + NOTES_WIDTH + 3
			&& mouseY >= this.topPos + NOTES_Y && mouseY < this.topPos + NOTES_Y + NOTES_HEIGHT;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (isOverNotes(mouseX, mouseY) && scrollY != 0) {
			int maxScroll = Math.max(0, noteLines().size() - visibleNoteLines());
			if (maxScroll > 0) {
				this.noteScroll = Math.clamp(this.noteScroll - (int) Math.signum(scrollY), 0, maxScroll);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		if (isOverNotes(mouseX, mouseY)) {
			// The full note remains readable at a comfortable width without covering inventory slots permanently.
			graphics.setTooltipForNextFrame(this.font.split(note(), Math.min(200, this.width - 20)), mouseX, mouseY);
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("escritoire");
	}
}
