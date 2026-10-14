package forestry.core.platform.compat.curios.client;

import com.mojang.blaze3d.vertex.PoseStack;
import forestry.api.ForestryConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class SpectaclesCurioRenderer implements ICurioRenderer {
	private static final Identifier TEXTURE = ForestryConstants.forestry("textures/item/naturalist_armor_1.png");
	private final HumanoidModel<HumanoidRenderState> armorModel;

	public SpectaclesCurioRenderer() {
		// Curios recreates the renderer on AddLayers, including after a resource reload.
		this.armorModel = new HumanoidModel<>(Minecraft.getInstance().getEntityModels()
			.bakeLayer(ModelLayers.PLAYER_ARMOR.get(EquipmentSlot.HEAD)));
		this.armorModel.body.visible = false;
		this.armorModel.leftArm.visible = false;
		this.armorModel.rightArm.visible = false;
		this.armorModel.leftLeg.visible = false;
		this.armorModel.rightLeg.visible = false;
		this.armorModel.head.visible = true;
		this.armorModel.hat.visible = true;
	}

	@Override
	public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(
		ItemStack stack, SlotContext slotContext, PoseStack poseStack, SubmitNodeCollector collector,
		int light, S state, RenderLayerParent<S, M> parent, EntityRendererProvider.Context context,
		float yRotation, float xRotation) {
		if (!(state instanceof HumanoidRenderState humanoid)) return;
		collector.order(1).submitModel(this.armorModel, humanoid, poseStack,
			RenderTypes.armorCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY,
			-1, null, state.outlineColor, null);
		if (stack.hasFoil()) {
			collector.order(2).submitModel(this.armorModel, humanoid, poseStack,
				RenderTypes.armorEntityGlint(), light, OverlayTexture.NO_OVERLAY,
				-1, null, state.outlineColor, null);
		}
	}
}
