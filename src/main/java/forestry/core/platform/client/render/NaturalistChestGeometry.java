package forestry.core.platform.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import forestry.api.ForestryConstants;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class NaturalistChestGeometry extends MachineGeometry {
	private static final String LID = "lid";
	private static final String BASE = "base";
	private static final String LOCK = "lock";

	private final ModelPart lid;
	private final ModelPart base;
	private final ModelPart lock;
	private final Identifier texture;

	public NaturalistChestGeometry(ModelPart root, String textureName) {

		this.lid = root.getChild(LID);
		this.base = root.getChild(BASE);
		this.lock = root.getChild(LOCK);
		this.texture = ForestryConstants.forestry("textures/block" + "/" + textureName + ".png");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild(BASE, CubeListBuilder.create().texOffs(0, 19)
			.addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.offset(0, 0, 0));
		partdefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0)
			.addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0, 9.0F, 1.0F));
		partdefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 0)
			.addBox(7.0F, -1.0F, 15.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0, 8.0F, 0));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}


	@Override public void visit(Pose pose, PoseStack stack, PartVisitor visitor) {
		stack.pushPose();
		rotateHorizontal(stack, pose.facing());
		float angle = 1f - pose.motion();
		float rotation = -(1f - angle * angle * angle) * Mth.HALF_PI;
		// Apply the hinge to the submitted matrix. Never mutate a shared ModelPart before deferred rendering.
		hinge(this.lid, rotation, stack, visitor);
		hinge(this.lock, rotation, stack, visitor);
		visitor.accept(this.base, this.texture, stack);
		stack.popPose();
	}
	private void hinge(ModelPart part, float rotation, PoseStack stack, PartVisitor visitor) {
		stack.pushPose();
		stack.translate(part.x / 16f, part.y / 16f, part.z / 16f);
		stack.mulPose(com.mojang.math.Axis.XP.rotation(rotation));
		stack.translate(-part.x / 16f, -part.y / 16f, -part.z / 16f);
		visitor.accept(part, this.texture, stack);
		stack.popPose();
	}

}
