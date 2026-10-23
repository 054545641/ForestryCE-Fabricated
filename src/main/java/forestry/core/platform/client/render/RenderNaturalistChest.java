package forestry.core.platform.client.render;

import forestry.core.platform.tile.TileNaturalistChest;
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

public class RenderNaturalistChest<T extends TileNaturalistChest> implements BlockEntityRenderer<T, MachineRenderState> {
	private final NaturalistChestGeometry geometry;
	public RenderNaturalistChest(BlockEntityRendererProvider.Context context, String texture) {
		this.geometry = new NaturalistChestGeometry(context.bakeLayer(ForestryModelLayers.NATURALIST_CHEST_LAYER), texture);
	}
	public static LayerDefinition createBodyLayer() { return NaturalistChestGeometry.createBodyLayer(); }
	@Override public MachineRenderState createRenderState() { return new MachineRenderState(); }
	@Override public void extractRenderState(T engine, MachineRenderState state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
		BlockEntityRenderer.super.extractRenderState(engine, state, partialTick, camera, breaking);
		state.pose = new MachineGeometry.Pose(engine.getBlockState().getValue(forestry.core.platform.block.BlockBase.FACING),
			net.minecraft.util.Mth.lerp(partialTick, engine.prevLidAngle, engine.lidAngle), 0);
	}
	@Override public void submit(MachineRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		this.geometry.submit(state.pose, stack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
	}
}
