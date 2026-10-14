package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Contains resolved item layers only; submission never reads a live inventory or level. */
public final class DisplayItemRenderState extends BlockEntityRenderState {
	public final ItemStackRenderState item = new ItemStackRenderState();
	public float age;
	public void submit(PoseStack stack, SubmitNodeCollector collector, float height, float scale) {
		if (this.item.isEmpty()) return;
		stack.pushPose();
		stack.translate(0.5, height, 0.5);
		stack.scale(scale, scale, scale);
		// Native item extents include the ground transform and also work with layered/special item models.
		stack.translate(0, Mth.sin(this.age / 10f) * 0.1f + 0.1f - this.item.getModelBoundingBox().minY + 0.0625f, 0);
		stack.mulPose(Axis.YP.rotation(this.age / 20f));
		this.item.submit(stack, collector, this.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		stack.popPose();
	}
}
