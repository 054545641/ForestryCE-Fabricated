package forestry.lepidopterology.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class ButterflyRenderState extends LivingEntityRenderState {
	public boolean renderable;
	public float size = 1;
	public float wingFlap;
	public Identifier texture = Identifier.withDefaultNamespace("textures/misc/white.png");
}
