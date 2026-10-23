package forestry.core.platform.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import forestry.core.platform.fluids.ForestryFluids;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.awt.*;

public class RenderUtil {
	// requires external push/pop
	public static void rotateByHorizontalDirection(PoseStack stack, Direction facing) {
		if (facing != Direction.SOUTH) {
			stack.translate(0.5, 0.5, 0.5);
			stack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
			stack.translate(-0.5, -0.5, -0.5);
		}
	}


	public static int getFluidColor(Fluid fluid) {
		var model = net.minecraft.client.Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
		int color = model.tintSource() == null ? -1 : model.tintSource().color(fluid.defaultFluidState().createLegacyBlock());
		ForestryFluids definition = ForestryFluids.getFluidDefinition(fluid);
		if (color == -1) {
			color = 0x0000ff;
			if (definition != null) {
				color = definition.getParticleColor();
			}
		}
		return color;
	}

	// this usage of Color is fine since it's client-only
	public static Color getRainbowColor(long time, float partialTicks) {
		return Color.getHSBColor((180 * Mth.sin((time + partialTicks) / 30.0f) - 180) / 360.0f, 0.5f, 0.8f);
	}

	/**
	 * Pulsing alpha in {@code [0.2, 1.0]} for the flashing-white spectacle outline drawn around the parts of an
	 * unformed multiblock (a faster sine than the rainbow hue so the two styles read differently at a glance).
	 */
	public static float getFlashingAlpha(long time, float partialTicks) {
		return 0.6f + 0.4f * Mth.sin((time + partialTicks) / 8.0f);
	}

	// VANILLA COPY
	public static int getYImage(Button button) {
		int i = 1;
		if (!button.active) {
			i = 0;
		} else if (button.isHoveredOrFocused()) {
			i = 2;
		}

		return i;
	}
}
