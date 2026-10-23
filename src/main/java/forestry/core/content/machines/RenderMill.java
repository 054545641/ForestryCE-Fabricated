package forestry.core.content.machines;

import forestry.core.content.machines.TileMill;
import forestry.core.platform.render.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderMill implements BlockEntityRenderer<TileMill, MachineRenderState> {
	private final MillGeometry geometry;
	public RenderMill(BlockEntityRendererProvider.Context context, String texture) {
		this.geometry = new MillGeometry(context.bakeLayer(ForestryModelLayers.MILL_LAYER), texture);
	}
	public static LayerDefinition createBodyLayer() { return MillGeometry.createBodyLayer(); }
	@Override public MachineRenderState createRenderState() { return new MachineRenderState(); }
	@Override public void extractRenderState(TileMill engine, MachineRenderState state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
		BlockEntityRenderer.super.extractRenderState(engine, state, partialTick, camera, breaking);
		float progress = engine.hasLevel() ? engine.progress + (engine.stage != 0 ? engine.speed * partialTick : 0) : 0;
		// The rainmaker retains its fixed orientation until its rotated voxel shape is implemented.
		state.pose = new MachineGeometry.Pose(net.minecraft.core.Direction.SOUTH, MachineGeometry.stroke(progress, 3.99f), net.minecraft.util.Mth.clamp(engine.charge, 0, 7));
	}
	@Override public void submit(MachineRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		this.geometry.submit(state.pose, stack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
	}
}
