package forestry.environment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import forestry.core.platform.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Exercises production geometry and native special definitions, without mock Forestry tiles. */
@EventBusSubscriber(modid = EnvironmentProbe.ID, value = Dist.CLIENT)
public final class N5MachineProbe {
    private static Object previousModel;
    private static int passes;
    @SubscribeEvent public static void special(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.parse("forestry:machine"), MachineSpecialRenderer.Unbaked.CODEC);
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ForestryModelLayers.ENGINE_LAYER, EngineGeometry::createBodyLayer);
        event.registerLayerDefinition(ForestryModelLayers.MILL_LAYER, MillGeometry::createBodyLayer);
        event.registerLayerDefinition(ForestryModelLayers.NATURALIST_CHEST_LAYER, NaturalistChestGeometry::createBodyLayer);
    }
    private static void check(boolean condition, String detail) { if (!condition) throw new AssertionError(detail); }
    private static boolean near(float a, float b) { return Math.abs(a - b) < 0.0001f; }
    private record Submission(ModelPart part, Matrix4f matrix, int light, int overlay, int outline,
                              ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        List<Vector3fc> vertices() {
            PoseStack stack = new PoseStack();
            stack.mulPose(this.matrix);
            List<Vector3fc> points = new ArrayList<>();
            this.part.getExtentsForGui(stack, points::add);
            return points;
        }
    }
    private static final class RecordingCollector extends SubmitNodeStorage {
        final List<Submission> submitted = new ArrayList<>();
        @Override public <S> void submitModel(Model<? super S> model, S state, PoseStack stack, RenderType type,
                                             int light, int overlay, int color, @Nullable TextureAtlasSprite sprite,
                                             int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
            this.submitted.add(new Submission(model.root(), new Matrix4f(stack.last().pose()), light, overlay, outline, breaking));
            super.submitModel(model, state, stack, type, light, overlay, color, sprite, outline, breaking);
        }
    }
    public static void verify(Minecraft client) {
        check(near(MachineGeometry.stroke(0, 6), 0) && near(MachineGeometry.stroke(.5f, 6), 6)
            && near(MachineGeometry.stroke(1, 6), 0), "Engine stroke has correct endpoints");
        var engine = new EngineGeometry(EngineGeometry.createBodyLayer().bakeRoot(), "textures/block/engine_copper_");
        for (Direction facing : Direction.values()) for (int heat = 0; heat < 5; heat++) {
            var pose = new MachineGeometry.Pose(facing, 3, heat);
            List<String> textures = new ArrayList<>();
            engine.visit(pose, new PoseStack(), (part, texture, stack) -> {
                check(client.getResourceManager().getResource(texture).isPresent(), "Machine texture exists: " + texture);
                textures.add(texture.getPath());
            });
            check(textures.get(2).endsWith(new String[]{"highest.png","higher.png","high.png","medium.png","low.png"}[heat]), "Heat chooses the right trunk");
            RecordingCollector collector = new RecordingCollector();
            PoseStack stack = new PoseStack();
            engine.submit(pose, stack, collector, 123, 456, 789, null);
            check(collector.submitted.size() == 6, "Idle piston has three base parts and three sleeve segments");
            check(stack.last().pose().equals(new Matrix4f()), "Machine leaves caller transform balanced");
            var center = collector.submitted.getFirst().matrix().transformPosition(new Vector3f(.5f, .5f, .5f));
            check(center.distance(new Vector3f(.5f, .5f, .5f)) < .0001f, "Every facing rotates around block center");
            var axis = collector.submitted.getFirst().matrix().transformDirection(new Vector3f(0, 1, 0));
            check(axis.distance(new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ())) < .0001f, "Engine axis follows all six facings");
            List<Vector3fc> expected = new ArrayList<>();
            engine.getExtents(pose, expected::add);
            List<Vector3fc> submitted = collector.submitted.stream().flatMap(part -> part.vertices().stream()).toList();
            check(expected.equals(submitted), "Extents use the same geometry and transforms as deferred submission");
        }
        var chest = new NaturalistChestGeometry(NaturalistChestGeometry.createBodyLayer().bakeRoot(), "apiaristchest");
        RecordingCollector collector = new RecordingCollector();
        var closed = new MachineGeometry.Pose(Direction.SOUTH, 0, 0);
        var open = new MachineGeometry.Pose(Direction.SOUTH, 1, 0);
        var breaking = new ModelFeatureRenderer.CrumblingOverlay(3, new PoseStack().last().copy());
        chest.submit(closed, new PoseStack(), collector, 123, 456, 789, breaking);
        List<Vector3fc> before = collector.submitted.getFirst().vertices();
        chest.submit(open, new PoseStack(), collector, 321, 654, 987, null);
        chest.submit(closed, new PoseStack(), collector, 123, 456, 789, breaking);
        check(before.equals(collector.submitted.getFirst().vertices()), "Later open chest does not mutate earlier deferred closed chest");
        check(!before.equals(collector.submitted.get(3).vertices()), "Lid actually opens");
        check(before.equals(collector.submitted.get(6).vertices()), "Opening does not leak into next closed chest");
        check(collector.submitted.getFirst().light() == 123 && collector.submitted.getFirst().overlay() == 456
            && collector.submitted.getFirst().outline() == 789 && collector.submitted.getFirst().breaking() == breaking,
            "Light, overlay, outline and breaking pass through the native collector");
        check(collector.submitted.stream().allMatch(s -> s.part().xRot == 0), "Shared ModelPart poses remain unchanged");
        var mill = new MillGeometry(MillGeometry.createBodyLayer().bakeRoot(), "textures/block/rainmaker_");
        for (int charge = 0; charge < 8; charge++) {
            List<Identifier> textures = new ArrayList<>();
            mill.visit(new MachineGeometry.Pose(Direction.SOUTH, MachineGeometry.stroke(.5f, 3.99f), charge), new PoseStack(),
                (part, texture, stack) -> { textures.add(texture); check(client.getResourceManager().getResource(texture).isPresent(), "Rainmaker texture exists"); });
            check(textures.size() == 5 && textures.get(1).getPath().endsWith("column_" + charge + ".png"), "All eight rainmaker charges and both blades render");
        }
        for (var kind : MachineSpecialRenderer.Kind.values()) {
            var model = client.getModelManager().getItemModel(Identifier.fromNamespaceAndPath("forestry", kind.getSerializedName()));
            check(model instanceof net.minecraft.client.renderer.item.SpecialModelWrapper<?>, "Native special definition baked: " + kind);
            if (kind == MachineSpecialRenderer.Kind.PEAT_ENGINE) {
                check(model != previousModel, "Reload creates a fresh special model");
                previousModel = model;
            }
            for (ItemDisplayContext context : ItemDisplayContext.values()) {
                var item = new ItemStackRenderState();
                // Direct model updates exercise the selected transforms; title-screen fixtures cannot test hand mirroring.
                model.update(item, ItemStack.EMPTY, client.getItemModelResolver(), context, null, null, 0);
                check(!item.isEmpty() && item.getModelBoundingBox().getSize() > 0, "Special item has native extents: " + kind + "/" + context);
                var material = item.pickParticleMaterial(net.minecraft.util.RandomSource.create());
                check(material != null && !material.sprite().contents().name().getPath().equals("missingno"), "Real machine particle texture: " + kind);
                RecordingCollector output = new RecordingCollector();
                item.submit(new PoseStack(), output, 123, 456, 789);
                check(!output.submitted.isEmpty(), "Special item submits actual model parts: " + kind + "/" + context);
            }
        }
        // Ensure a cleared display state cannot leave a previously resolved item behind.
        var display = new DisplayItemRenderState();
        client.getModelManager().getItemModel(Identifier.parse("forestry:peat_engine")).update(display.item, ItemStack.EMPTY,
            client.getItemModelResolver(), ItemDisplayContext.GROUND, null, null, 0);
        RecordingCollector output = new RecordingCollector();
        display.submit(new PoseStack(), output, .2f, 1f);
        check(!output.submitted.isEmpty(), "Analyzer display submits resolved native item state");
        client.getItemModelResolver().updateForTopItem(display.item, ItemStack.EMPTY, ItemDisplayContext.GROUND, null, null, 1);
        RecordingCollector empty = new RecordingCollector();
        display.submit(new PoseStack(), empty, .9f, .75f);
        check(empty.submitted.isEmpty(), "Cleared display does not submit stale geometry");
        var butterfly = new forestry.lepidopterology.render.ButterflyModel(forestry.lepidopterology.render.ButterflyModel.createLayer().bakeRoot());
        var small = new forestry.lepidopterology.render.ButterflyRenderState();
        small.size = .5f; small.wingFlap = 0;
        butterfly.setupAnim(small);
        float angle = butterfly.root().getChild("left_wing").zRot;
        check(near(butterfly.root().xScale, .5f) && near(butterfly.root().y, 23.2f), "Butterfly preserves size and model offset");
        var large = new forestry.lepidopterology.render.ButterflyRenderState();
        large.size = 2; large.wingFlap = 1;
        butterfly.setupAnim(large);
        check(near(butterfly.root().xScale, 2) && !near(butterfly.root().getChild("left_wing").zRot, angle), "Butterfly animation follows each snapshot");
        butterfly.setupAnim(small);
        check(near(butterfly.root().y, 23.2f) && near(butterfly.root().getChild("left_wing").zRot, angle), "Butterfly animation resets between deferred model draws");
        LogUtils.getLogger().info("N5 machine probe passed (pass {}): six engine facings/five heat textures, chest deferred-pose isolation, eight rainmaker charges, native metadata, nine special items/selected display transforms (hand mirroring untested), display clearing, butterfly pose reset", ++passes);
    }
}

