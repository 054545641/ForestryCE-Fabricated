package forestry.core.platform.models;

import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import org.joml.Vector3f;

/**
 * Shared item-transform tables for Forestry's baked models.
 */
public class ModelTransforms {
	// copied from "minecraft:block/block.json" model
	public static final ItemTransforms BLOCK = new ItemTransforms(
		new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
		new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f / 16f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
		new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
		new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
		ItemTransform.NO_TRANSFORM,
		new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f)),
		new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 3 / 16f, 0), new Vector3f(0.25f, 0.25f, 0.25f)),
		new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f)),
		ItemTransform.NO_TRANSFORM
	);

	private ModelTransforms() {}
}
