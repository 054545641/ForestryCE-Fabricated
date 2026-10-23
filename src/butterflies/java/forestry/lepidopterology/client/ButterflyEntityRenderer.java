package forestry.lepidopterology.client;

import com.mojang.blaze3d.vertex.PoseStack;

import forestry.lepidopterology.entities.EntityButterfly;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class ButterflyEntityRenderer extends MobRenderer<EntityButterfly, ButterflyRenderState, ButterflyModel> {
	public ButterflyEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new ButterflyModel(context.bakeLayer(ButterfliesFabricClient.LAYER)), 0.25f);
	}
	@Override public ButterflyRenderState createRenderState() { return new ButterflyRenderState(); }
	@Override public void extractRenderState(EntityButterfly entity, ButterflyRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.renderable = entity.isRenderable();
		state.size = entity.getSize();
		state.wingFlap = state.renderable ? entity.getWingFlap(partialTick) : 0;
		if (state.renderable) state.texture = entity.getTexture();
	}
	@Override public void submit(ButterflyRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.renderable) return;
		stack.pushPose();
		stack.translate(0, 0.2, 0);
		super.submit(state, stack, collector, camera);
		stack.popPose();
	}
	@Override public Identifier getTextureLocation(ButterflyRenderState state) { return state.texture; }
}
