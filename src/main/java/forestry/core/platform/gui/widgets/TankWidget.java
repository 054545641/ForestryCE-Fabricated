package forestry.core.platform.gui.widgets;

import net.minecraft.client.renderer.RenderPipelines;

import forestry.api.core.IToolPipette;
import forestry.api.core.tooltips.ToolTip;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.gui.ContainerTile;
import forestry.core.platform.gui.IContainerLiquidTanks;
import forestry.core.platform.gui.IContainerTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import forestry.api.core.fluids.FluidVolume;

import javax.annotation.Nullable;

/**
 * Slot for liquid tanks
 */
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
		FluidVolume contents = tank.getFluid();
		if (!contents.isEmpty()) {
			var minecraft = Minecraft.getInstance();
			var fluidState = contents.getFluid().defaultFluidState();
			var model = minecraft.getModelManager().getFluidStateModelSet().get(fluidState);
			var sprite = model.stillMaterial().sprite();
			int color = -1;
			if (model.tintSource() != null) {
				BlockPos pos = this.manager.gui.getMenu() instanceof ContainerTile<?> tileMenu ? tileMenu.getTile().getBlockPos()
					: minecraft.player == null ? BlockPos.ZERO : minecraft.player.blockPosition();
				var blockState = fluidState.createLegacyBlock();
				color = ARGB.opaque(minecraft.level == null ? model.tintSource().color(blockState)
					: model.tintSource().colorInWorld(blockState, minecraft.level, pos));
			}
			int amount = (int) Math.clamp((double) contents.getAmount() * this.height / tank.getCapacity(), 1, this.height);
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
		ToolTip tooltip = new ToolTip();
		tooltip.add(tank.isEmpty() ? net.minecraft.network.chat.Component.translatable("for.gui.empty") : net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getName(tank.getResource()));
		tooltip.add(net.minecraft.network.chat.Component.literal(formatAmount(tank.getAmount()) + " / " + formatAmount(tank.getCapacity()) + " mB"));
		return tooltip;
	}

	private static String formatAmount(long droplets) {
		long unit = forestry.api.core.fluids.FluidUnits.DROPLETS_PER_MILLIBUCKET;
		long whole = droplets / unit;
		long remainder = droplets % unit;
		return remainder == 0 ? Long.toString(whole) : whole + " + " + remainder + "/" + unit;
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
