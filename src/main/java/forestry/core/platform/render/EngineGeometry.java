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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class EngineGeometry extends MachineGeometry {
	private static final float[] ANGLE_MAP = new float[6];

	private enum Textures {
		BASE, PISTON, EXTENSION, TRUNK_HIGHEST, TRUNK_HIGHER, TRUNK_HIGH, TRUNK_MEDIUM, TRUNK_LOW
	}

	private final Identifier[] textures;
	private final ModelPart boiler;
	private final ModelPart trunk;
	private final ModelPart piston;
	private final ModelPart extension;

	static {
		ANGLE_MAP[Direction.EAST.ordinal()] = -Mth.HALF_PI;
		ANGLE_MAP[Direction.NORTH.ordinal()] = -Mth.HALF_PI;
		ANGLE_MAP[Direction.WEST.ordinal()] = Mth.HALF_PI;
		ANGLE_MAP[Direction.SOUTH.ordinal()] = Mth.HALF_PI;
		ANGLE_MAP[Direction.UP.ordinal()] = 0;
		ANGLE_MAP[Direction.DOWN.ordinal()] = Mth.PI;
	}

	public EngineGeometry(ModelPart root, String baseTexture) {

		this.boiler = root.getChild("boiler");
		this.trunk = root.getChild("trunk");
		this.piston = root.getChild("piston");
		this.extension = root.getChild("extension");

		this.textures = new Identifier[]{
			ForestryConstants.forestry(baseTexture + "base.png"),
			ForestryConstants.forestry(baseTexture + "piston.png"),
			ForestryConstants.forestry(baseTexture + "extension.png"),
			ForestryConstants.forestry("textures/block" + "/engine_trunk_highest.png"),
			ForestryConstants.forestry("textures/block" + "/engine_trunk_higher.png"),
			ForestryConstants.forestry("textures/block" + "/engine_trunk_high.png"),
			ForestryConstants.forestry("textures/block" + "/engine_trunk_medium.png"),
			ForestryConstants.forestry("textures/block" + "/engine_trunk_low.png"),
		};
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("boiler", CubeListBuilder.create().texOffs(0, 0)
			.addBox(0, 0, 0, 16, 6, 16), PartPose.offset(0, 0, 0));
		root.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0, 0)
			.addBox(0, 0, 0, 8, 12, 8), PartPose.offset(4, 4, 4));
		root.addOrReplaceChild("piston", CubeListBuilder.create().texOffs(0, 0)
			.addBox(0, 0, 0, 12, 4, 12), PartPose.offset(2, 6, 2));
		root.addOrReplaceChild("extension", CubeListBuilder.create().texOffs(0, 0)
			.addBox(0, 0, 0, 10, 2, 10), PartPose.offset(3, 5, 3));

		return LayerDefinition.create(mesh, 64, 32);
	}


	@Override public void visit(Pose pose, PoseStack stack, PartVisitor visitor) {
		stack.pushPose();
		stack.translate(0.5, 0.5, 0.5);
		Direction direction = pose.facing();
		switch (direction) {
			case EAST, WEST, DOWN -> stack.mulPose(Axis.ZP.rotation(ANGLE_MAP[direction.ordinal()]));
			default -> stack.mulPose(Axis.XP.rotation(ANGLE_MAP[direction.ordinal()]));
		}
		stack.translate(-0.5, -0.5, -0.5);
		visitor.accept(this.boiler, this.textures[Textures.BASE.ordinal()], stack);
		float step = pose.motion();
		stack.pushPose();
		stack.translate(0, step / 16f, 0);
		visitor.accept(this.piston, this.textures[Textures.PISTON.ordinal()], stack);
		stack.popPose();
		visitor.accept(this.trunk, this.textures[Textures.TRUNK_HIGHEST.ordinal() + pose.texture()], stack);
		if (step > 0) {
			for (int i = 0; i <= step + 2; i += 2) {
				visitor.accept(this.extension, this.textures[Textures.EXTENSION.ordinal()], stack);
				stack.translate(0, 2f / 16f, 0);
			}
		}
		stack.popPose();
	}

}
