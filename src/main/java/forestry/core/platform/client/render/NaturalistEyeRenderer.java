package forestry.core.platform.client.render;

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
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import java.util.ArrayList;
import java.util.List;

/** World access happens during extraction; submission reads only immutable outline snapshots. */
public final class NaturalistEyeRenderer {
    private static final RenderPipeline PIPELINE = createPipeline();
    private static final RenderType LINES = RenderType.create("forestry_naturalist_eye", RenderSetup.builder(PIPELINE).createRenderSetup());
    private static final RenderStateDataKey<List<Outline>> OUTLINES = RenderStateDataKey.create(() -> "forestry:naturalist_outlines");
    private record Outline(double relativeX, double relativeY, double relativeZ, int color) {}

    public static void initialize() {
        LevelExtractionEvents.END_EXTRACTION.register(NaturalistEyeRenderer::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(NaturalistEyeRenderer::submit);
    }
    private static RenderPipeline createPipeline() {
        var original = RenderPipelines.LINES;
        var builder = RenderPipeline.builder().withLocation(ForestryConstants.forestry("pipeline/naturalist_eye"))
            .withVertexShader(original.getVertexShader()).withFragmentShader(original.getFragmentShader())
            .withVertexBinding(0, original.getVertexFormatBinding(0)).withPrimitiveTopology(original.getPrimitiveTopology())
            .withCull(original.isCull()).withPolygonMode(original.getPolygonMode())
            .withColorTargetState(original.getColorTargetState())
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false));
        original.getBindGroupLayouts().forEach(builder::withBindGroupLayout);
        return builder.build();
    }
    public static void extract(LevelExtractionContext event) {
        var client = Minecraft.getInstance();
        var player = client.player;
        var outlines = new ArrayList<Outline>();
        if (player != null && GeneticsUtil.hasNaturalistEye(player)) {
            var level = event.level();
            var center = ChunkPos.containing(player.blockPosition());
            var camera = event.camera().position();
            int distance = client.options.renderDistance().get();
            float partial = event.deltaTracker().getGameTimeDeltaPartialTick(false);
            int rainbow = RenderUtil.getRainbowColor(level.getGameTime(), partial).getRGB();
            int flash = (Math.round(255 * RenderUtil.getFlashingAlpha(level.getGameTime(), partial)) << 24) | 0xffffff;
            for (int chunkX = center.x() - distance; chunkX <= center.x() + distance; chunkX++) {
                for (int chunkZ = center.z() - distance; chunkZ <= center.z() + distance; chunkZ++) {
                    var chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
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
        event.levelState().setData(OUTLINES, List.copyOf(outlines));
    }
    public static void submit(LevelRenderContext event) {
        var outlines = event.levelState().getData(OUTLINES);
        if (outlines == null || outlines.isEmpty()) return;
        var stack = event.poseStack();
        for (Outline outline : outlines) {
            stack.pushPose();
            stack.translate(outline.relativeX, outline.relativeY, outline.relativeZ);
            event.submitNodeCollector().submitCustomGeometry(stack, LINES, (pose, vertices) -> {
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
