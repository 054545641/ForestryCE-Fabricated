package forestry.core.platform.gui.widgets;

import net.minecraft.client.renderer.RenderPipelines;

import forestry.api.core.IToolPipette;
import forestry.api.core.tooltips.ToolTip;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.gui.IContainerLiquidTanks;
import forestry.core.platform.gui.IContainerTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

/**
 * Slot for liquid tanks
 */
@OnlyIn(Dist.CLIENT)
public class TankWidget extends Widget {

	private int overlayTexX = 176;
	private int overlayTexY = 0;
	private final int slot;
	protected boolean drawOverlay = true;

	public TankWidget(WidgetManager manager, int xPos, int yPos, int slot) {
		super(manager, xPos, yPos);
		this.slot = slot;
		this.height = 58;
	}

	public TankWidget setOverlayOrigin(int x, int y) {
        this.overlayTexX = x;
        this.overlayTexY = y;
		return this;
	}

	@Nullable
	public StandardTank getTank() {
		AbstractContainerMenu container = this.manager.gui.getMenu();
		if (container instanceof IContainerTank tanks) {
			return tanks.getTank(this.slot);
		}
		return null;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		StandardTank tank = getTank();
		if (tank == null || tank.getCapacity() <= 0) return;
		FluidStack contents = tank.getFluid();
		if (!contents.isEmpty()) {
			var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(contents.getFluid().defaultFluidState());
			var sprite = model.stillMaterial().sprite();
			int color = model.fluidTintSource() == null ? -1 : model.fluidTintSource().colorAsStack(contents);
			int amount = (int) Math.clamp((long) contents.getAmount() * this.height / tank.getCapacity(), 1, this.height);
			int left = startX + this.xPos;
			int bottom = startY + this.yPos + this.height;
			graphics.enableScissor(left, bottom - amount, left + this.width, bottom);
			for (int x = 0; x < this.width; x += 16) {
				for (int y = 0; y < amount; y += 16) {
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, left + x, bottom - y - 16, 16, 16, color);
				}
			}
			graphics.disableScissor();
		}
		if (this.drawOverlay) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.manager.gui.textureFile,
				startX + this.xPos, startY + this.yPos, this.overlayTexX, this.overlayTexY, 16, 60, 256, 256);
		}
	}

	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		StandardTank tank = getTank();
		if (tank == null) {
			return null;
		}
		return tank.getToolTip();
	}

	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
		Player player = this.manager.minecraft.player;
		ItemStack itemstack = player.containerMenu.getCarried();
		if (itemstack.isEmpty()) {
			return;
		}

		Item held = itemstack.getItem();
		AbstractContainerMenu container = this.manager.gui.getMenu();
		if (held instanceof IToolPipette && container instanceof IContainerLiquidTanks) {
			((IContainerLiquidTanks) container).handlePipetteClickClient(this.slot, player);
		}
	}
}
