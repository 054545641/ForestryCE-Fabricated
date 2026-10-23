package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/** Shared geometry for world snapshots and resource-baked special items. Parts never change pose. */
public abstract class MachineGeometry {
	public record Pose(Direction facing, float motion, int texture) {}
	@FunctionalInterface
	public interface PartVisitor { void accept(ModelPart part, Identifier texture, PoseStack stack); }
	public abstract void visit(Pose pose, PoseStack stack, PartVisitor visitor);
	public final void submit(Pose pose, PoseStack stack, SubmitNodeCollector collector, int light, int overlay,
			int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
		visit(pose, stack, (part, texture, transform) -> collector.submitModelPart(part, transform,
			RenderTypes.entityCutout(texture), light, overlay, null, -1, breaking, outline));
	}
	public final void getExtents(Pose pose, Consumer<Vector3fc> output) {
		visit(pose, new PoseStack(), (part, texture, transform) -> part.getExtentsForGui(transform, output));
	}
	public static float stroke(float progress, float length) {
		return progress > 0.5f ? length - (progress - 0.5f) * 2f * length : progress * 2f * length;
	}
	public static void rotateHorizontal(PoseStack stack, Direction facing) {
		if (facing != Direction.SOUTH) {
			stack.translate(0.5, 0.5, 0.5);
			stack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
			stack.translate(-0.5, -0.5, -0.5);
		}
	}
}
