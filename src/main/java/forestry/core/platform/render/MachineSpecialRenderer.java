package forestry.core.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import java.util.function.Consumer;
import org.joml.Vector3fc;

/** Resource-baked item geometry: no synthetic block entity, global dispatcher or stale reload cache. */
public final class MachineSpecialRenderer implements NoDataSpecialModelRenderer {
	public enum Kind implements StringRepresentable {
		PEAT_ENGINE("peat_engine", "engine_copper_"), BIOGAS_ENGINE("biogas_engine", "engine_bronze_"),
		CLOCKWORK_ENGINE("clockwork_engine", "engine_clock_"), COMBUSTION_ENGINE("combustion_engine", "engine_iron_"),
		SOLAR_ENGINE("solar_engine", "engine_tin_"), RAINMAKER("rainmaker", "rainmaker_"),
		APIARISTS_CHEST("apiarists_chest", "apiaristchest"), ARBORISTS_CHEST("arborists_chest", "arbchest"),
		LEPIDOPTERISTS_CHEST("lepidopterists_chest", "lepichest");
		public static final com.mojang.serialization.Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
		private final String name;
		private final String texture;
		Kind(String name, String texture) { this.name = name; this.texture = texture; }
		@Override public String getSerializedName() { return this.name; }
	}
	private final MachineGeometry geometry;
	private final MachineGeometry.Pose pose;
	public MachineSpecialRenderer(MachineGeometry geometry, MachineGeometry.Pose pose) { this.geometry = geometry; this.pose = pose; }
	@Override public void submit(PoseStack stack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
		this.geometry.submit(this.pose, stack, collector, light, overlay, outline, null);
	}
	@Override public void getExtents(Consumer<Vector3fc> output) { this.geometry.getExtents(this.pose, output); }
	public record Unbaked(Kind kind) implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Kind.CODEC.fieldOf("kind").forGetter(Unbaked::kind)).apply(instance, Unbaked::new));
		@Override public MapCodec<Unbaked> type() { return CODEC; }
		@Override public MachineSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
			return switch (this.kind) {
				case RAINMAKER -> new MachineSpecialRenderer(new MillGeometry(context.entityModelSet().bakeLayer(ForestryModelLayers.MILL_LAYER),
			"textures/block/" + this.kind.texture), new MachineGeometry.Pose(Direction.SOUTH, 0, 0));
				case APIARISTS_CHEST, ARBORISTS_CHEST, LEPIDOPTERISTS_CHEST -> new MachineSpecialRenderer(
			new NaturalistChestGeometry(context.entityModelSet().bakeLayer(ForestryModelLayers.NATURALIST_CHEST_LAYER), this.kind.texture),
			new MachineGeometry.Pose(Direction.SOUTH, 0, 0));
				default -> new MachineSpecialRenderer(new EngineGeometry(context.entityModelSet().bakeLayer(ForestryModelLayers.ENGINE_LAYER),
			"textures/block/" + this.kind.texture), new MachineGeometry.Pose(Direction.UP, 3, 4));
			};
		}
	}
}
