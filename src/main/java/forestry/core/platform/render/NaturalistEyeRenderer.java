package forestry.core.platform.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import forestry.api.ForestryConstants;
import forestry.api.core.ISpectacleBlock;
import forestry.core.platform.util.GeneticsUtil;
import forestry.core.platform.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import java.util.ArrayList;
import java.util.List;

/** World access happens during extraction; submission reads only immutable outline snapshots. */
public final class NaturalistEyeRenderer {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation(ForestryConstants.forestry("pipeline/naturalist_eye"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).build();
    private static final RenderType LINES = RenderType.create("forestry_naturalist_eye", RenderSetup.builder(PIPELINE).createRenderSetup());
    private static final ContextKey<List<Outline>> OUTLINES = new ContextKey<>(ForestryConstants.forestry("naturalist_outlines"));
    private record Outline(double x, double y, double z, int color) {}

    public static void registerPipeline(RegisterRenderPipelinesEvent event) { event.registerPipeline(PIPELINE); }
    public static void extract(ExtractLevelRenderStateEvent event) {
        var client = Minecraft.getInstance();
        var player = client.player;
        var outlines = new ArrayList<Outline>();
        if (player != null && GeneticsUtil.hasNaturalistEye(player)) {
            var level = event.getLevel();
            var center = ChunkPos.containing(player.blockPosition());
            var camera = event.getCamera().position();
            int distance = client.options.renderDistance().get();
            float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            int rainbow = RenderUtil.getRainbowColor(level.getGameTime(), partial).getRGB();
            int flash = (Math.round(255 * RenderUtil.getFlashingAlpha(level.getGameTime(), partial)) << 24) | 0xffffff;
            for (int x = center.x() - distance; x <= center.x() + distance; x++) {
                for (int z = center.z() - distance; z <= center.z() + distance; z++) {
                    var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                    if (chunk == null) continue;
                    for (var blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity instanceof ISpectacleBlock highlighted && highlighted.isHighlighted(player)) {
                            var pos = blockEntity.getBlockPos();
                            outlines.add(new Outline(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z,
                                highlighted.usesFlashingHighlight(player) ? flash : rainbow));
                        }
                    }
                }
            }
        }
        event.getRenderState().setRenderData(OUTLINES, List.copyOf(outlines));
    }
    public static void submit(SubmitCustomGeometryEvent event) {
        var outlines = event.getLevelRenderState().getRenderData(OUTLINES);
        if (outlines == null || outlines.isEmpty()) return;
        var stack = event.getPoseStack();
        for (Outline outline : outlines) {
            stack.pushPose();
            stack.translate(outline.x, outline.y, outline.z);
            event.getSubmitNodeCollector().submitCustomGeometry(stack, LINES, (pose, vertices) -> {
                for (int corner = 0; corner < 8; corner++) for (int axis = 0; axis < 3; axis++) {
                    if ((corner & (1 << axis)) != 0) continue;
                    int other = corner | (1 << axis);
                    float nx = axis == 0 ? 1 : 0, ny = axis == 1 ? 1 : 0, nz = axis == 2 ? 1 : 0;
                    for (int endpoint : new int[] {corner, other}) {
                        vertices.addVertex(pose, coordinate(endpoint, 0), coordinate(endpoint, 1), coordinate(endpoint, 2))
                            .setColor(outline.color).setNormal(pose, nx, ny, nz).setLineWidth(2.5f);
                    }
                }
            });
            stack.popPose();
        }
    }
    private static float coordinate(int corner, int axis) { return (corner & (1 << axis)) == 0 ? -0.001f : 1.001f; }
}
