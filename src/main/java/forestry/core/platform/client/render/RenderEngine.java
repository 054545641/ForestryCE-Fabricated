package forestry.core.platform.client.render;

import forestry.core.content.energy.tiles.EngineBlockEntity;
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

public class RenderEngine<T extends EngineBlockEntity> implements BlockEntityRenderer<T, MachineRenderState> {
	private final EngineGeometry geometry;
	public RenderEngine(BlockEntityRendererProvider.Context context, String texture) {
		this.geometry = new EngineGeometry(context.bakeLayer(ForestryModelLayers.ENGINE_LAYER), texture);
	}
	public static LayerDefinition createBodyLayer() { return EngineGeometry.createBodyLayer(); }
	@Override public MachineRenderState createRenderState() { return new MachineRenderState(); }
	@Override public void extractRenderState(T engine, MachineRenderState state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
		BlockEntityRenderer.super.extractRenderState(engine, state, partialTick, camera, breaking);
		float progress = engine.hasLevel() ? engine.progress + (engine.stagePiston != 0 ? engine.pistonSpeedServer * partialTick : 0) : 0.25f;
		int heat = switch (engine.hasLevel() ? engine.getTemperatureState() : forestry.core.platform.tile.TemperatureState.COOL) {
			case OVERHEATING -> 0; case RUNNING_HOT -> 1; case OPERATING_TEMPERATURE -> 2; case WARMED_UP -> 3; default -> 4;
		};
		state.pose = new MachineGeometry.Pose(engine.getBlockState().getValue(forestry.core.content.energy.blocks.EngineBlock.VERTICAL_FACING), MachineGeometry.stroke(progress, 6f), heat);
	}
	@Override public void submit(MachineRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		this.geometry.submit(state.pose, stack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
	}
}
