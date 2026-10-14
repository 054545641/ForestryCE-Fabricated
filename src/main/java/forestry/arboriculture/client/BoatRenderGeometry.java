package forestry.arboriculture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import forestry.api.ForestryConstants;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import org.joml.Quaternionf;

/** Current vanilla boat transforms, with the wood texture carried by each extracted state. */
public final class BoatRenderGeometry {
	public static final ModelLayerLocation BOAT = new ModelLayerLocation(ForestryConstants.forestry("boat"), "main");
	public static final ModelLayerLocation CHEST_BOAT = new ModelLayerLocation(ForestryConstants.forestry("chest_boat"), "main");
	private final BoatModel model;
	private final Model.Simple waterPatch;

	public BoatRenderGeometry(ModelPart root, ModelPart waterPatch) {
		this.model = new BoatModel(root);
		this.waterPatch = new Model.Simple(waterPatch, texture -> RenderTypes.waterMask());
	}

	public static Identifier texture(String wood, boolean chest) {
		// Entity textures retain the original Forestry names; item and block textures use the newer species names.
		String asset = switch (wood) {
			case "camelthorn" -> "acacia_desert";
			case "sour_cherry" -> "hill_cherry";
			case "lemon" -> "citrus";
			case "giant_sequoia" -> "giganteum";
			case "zebrano" -> "zebrawood";
			default -> wood;
		};
		return ForestryConstants.forestry("textures/entity/" + (chest ? "chest_boat/" : "boat/") + asset + ".png");
	}

	public void submit(ForestryBoatRenderState state, PoseStack stack, SubmitNodeCollector collector) {
		stack.pushPose();
		stack.translate(0, 0.375f, 0);
		stack.mulPose(Axis.YP.rotationDegrees(180f - state.yRot));
		if (state.hurtTime > 0) {
			stack.mulPose(Axis.XP.rotationDegrees(Mth.sin(state.hurtTime) * state.hurtTime * state.damageTime / 10f * state.hurtDir));
		}
		if (!state.isUnderWater && !Mth.equal(state.bubbleAngle, 0)) {
			stack.mulPose(new Quaternionf().setAngleAxis(state.bubbleAngle * Mth.DEG_TO_RAD, 1, 0, 1));
		}
		stack.scale(-1, -1, 1);
		stack.mulPose(Axis.YP.rotationDegrees(90));
		collector.submitModel(this.model, state, stack, state.texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		if (!state.isUnderWater) {
			collector.submitModel(this.waterPatch, Unit.INSTANCE, stack, state.texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		}
		stack.popPose();
	}
}
