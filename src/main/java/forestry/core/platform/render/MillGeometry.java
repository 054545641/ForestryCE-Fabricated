package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import forestry.api.ForestryConstants;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// This is called Mill because it used to be for the Forester and Treetap blocks in older versions of Forestry
public final class MillGeometry extends MachineGeometry {
	private enum Textures {PEDESTAL, EXTENSION, BLADE, CHARGE}

	private final Identifier[] textures;
	private final ModelPart pedestal;
	private final ModelPart column;
	private final ModelPart extension;
	private final ModelPart blade;

	public MillGeometry(ModelPart root, String baseTexture) {

		this.pedestal = root.getChild(Textures.PEDESTAL.name());
		this.column = root.getChild(Textures.CHARGE.name());
		this.extension = root.getChild(Textures.EXTENSION.name());
		this.blade = root.getChild(Textures.BLADE.name());

		this.textures = new Identifier[11];

		this.textures[Textures.PEDESTAL.ordinal()] = ForestryConstants.forestry(baseTexture + "pedestal.png");
		this.textures[Textures.EXTENSION.ordinal()] = ForestryConstants.forestry(baseTexture + "extension.png");
		this.textures[Textures.BLADE.ordinal()] = ForestryConstants.forestry(baseTexture + "blade.png");

		for (int i = 0; i < 8; i++) {
			this.textures[Textures.CHARGE.ordinal() + i] = ForestryConstants.forestry(baseTexture + "column_" + i + ".png");
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild(Textures.PEDESTAL.name(), CubeListBuilder.create().texOffs(0, 0)
			.addBox(0f, 0f, 0f, 16, 1, 16), PartPose.offset(0, 0, 0));
		partdefinition.addOrReplaceChild(Textures.CHARGE.name(), CubeListBuilder.create().texOffs(0, 0)
			.addBox(0f, 0f, 0f, 4, 15, 4), PartPose.offset(6, 1, 6));
		partdefinition.addOrReplaceChild(Textures.EXTENSION.name(), CubeListBuilder.create().texOffs(0, 0)
			.addBox(0f, 0f, 0f, 14, 2, 2), PartPose.offset(1, 8, 7));
		partdefinition.addOrReplaceChild(Textures.BLADE.name(), CubeListBuilder.create().texOffs(0, 0)
			.addBox(0f, 0f, 0f, 1, 12, 8), PartPose.offset(10, 3, 4));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}


	@Override public void visit(Pose pose, PoseStack stack, PartVisitor visitor) {
		stack.pushPose();
		rotateHorizontal(stack, pose.facing());
		visitor.accept(this.pedestal, this.textures[Textures.PEDESTAL.ordinal()], stack);
		visitor.accept(this.column, this.textures[Textures.CHARGE.ordinal() + pose.texture()], stack);
		visitor.accept(this.extension, this.textures[Textures.EXTENSION.ordinal()], stack);
		float step = pose.motion() / 16f;
		stack.pushPose();
		stack.translate(step, 0, 0);
		visitor.accept(this.blade, this.textures[Textures.BLADE.ordinal()], stack);
		stack.popPose();
		stack.translate(1, 0, 1);
		stack.mulPose(Axis.YP.rotation(Mth.PI));
		stack.translate(step, 0, 0);
		visitor.accept(this.blade, this.textures[Textures.BLADE.ordinal()], stack);
		stack.popPose();
	}

}
