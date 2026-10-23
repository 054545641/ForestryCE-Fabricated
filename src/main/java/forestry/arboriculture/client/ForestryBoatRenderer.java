package forestry.arboriculture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import forestry.arboriculture.entities.ForestryBoat;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class ForestryBoatRenderer extends EntityRenderer<ForestryBoat, ForestryBoatRenderState> {
	private final BoatRenderGeometry geometry;
	private final boolean hasChest;

	public ForestryBoatRenderer(EntityRendererProvider.Context context, boolean hasChest) {
		super(context);
		this.hasChest = hasChest;
		this.shadowRadius = 0.8f;
		this.geometry = new BoatRenderGeometry(context.bakeLayer(hasChest ? BoatRenderGeometry.CHEST_BOAT : BoatRenderGeometry.BOAT),
			context.bakeLayer(ModelLayers.BOAT_WATER_PATCH));
	}

	@Override public ForestryBoatRenderState createRenderState() { return new ForestryBoatRenderState(); }

	@Override public void extractRenderState(ForestryBoat entity, ForestryBoatRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.texture = BoatRenderGeometry.texture(entity.getWoodType().getSerializedName(), this.hasChest);
		state.yRot = entity.getYRot(partialTicks);
		state.hurtTime = entity.getHurtTime() - partialTicks;
		state.hurtDir = entity.getHurtDir();
		state.damageTime = Math.max(entity.getDamage() - partialTicks, 0);
		state.bubbleAngle = entity.getBubbleAngle(partialTicks);
		state.isUnderWater = entity.isUnderWater();
		state.rowingTimeLeft = entity.getRowingTime(0, partialTicks);
		state.rowingTimeRight = entity.getRowingTime(1, partialTicks);
	}

	@Override public void submit(ForestryBoatRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
		this.geometry.submit(state, stack, collector);
		super.submit(state, stack, collector, camera);
	}
}
