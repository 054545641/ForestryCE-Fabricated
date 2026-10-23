package forestry.core.platform.render;

import forestry.core.content.analyzer.TileAnalyzer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderAnalyzer implements BlockEntityRenderer<TileAnalyzer, DisplayItemRenderState> {
	private final ItemModelResolver items;
	public RenderAnalyzer(BlockEntityRendererProvider.Context context) { this.items = context.itemModelResolver(); }
	@Override public DisplayItemRenderState createRenderState() { return new DisplayItemRenderState(); }
	@Override public void extractRenderState(TileAnalyzer tile, DisplayItemRenderState state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
		BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breaking);
		this.items.updateForTopItem(state.item, tile.getIndividualOnDisplay().copy(), ItemDisplayContext.GROUND, tile.getLevel(), null, 1);
		state.age = tile.getLevel() == null ? 0 : (float) tile.getLevel().getGameTime() + partialTick;
	}
	@Override public void submit(DisplayItemRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		state.submit(stack, collector, 0.2f, 1f);
	}
}
