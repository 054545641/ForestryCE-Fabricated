package forestry.core.data.models;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ItemLike;

import java.util.Map;

/** Shared model templates; item selection/tints are generated separately. */
public final class ModelAssets {
	private final Map<Identifier, ModelAsset> models;
	private final String folder;
	public ModelAssets(Map<Identifier, ModelAsset> models, String folder) { this.models = models; this.folder = folder; }
	public Identifier modLoc(String path) { return Identifier.fromNamespaceAndPath("forestry", path); }
	public Identifier mcLoc(String path) { return Identifier.withDefaultNamespace(path); }
	public ModelAsset getBuilder(String path) {
		Identifier id = Identifier.parse(path);
		if (!path.contains(":")) id = modLoc(path);
		if (!id.getPath().startsWith("block/") && !id.getPath().startsWith("item/")) id = id.withPrefix(this.folder + "/");
		return this.models.computeIfAbsent(id, ModelAsset::new);
	}
	public ModelAsset getExistingFile(Identifier id) { return new ModelAsset(id); }
	public ModelAsset withExistingParent(String name, Identifier parent) { return getBuilder(name).parent(parent); }
	public ModelAsset withExistingParent(String name, String parent) { return withExistingParent(name, Identifier.parse(parent)); }
	public ModelAsset template(String name, String parent, Identifier... textures) {
		var model = withExistingParent(name, mcLoc("block/" + parent));
		String[] keys = switch (parent) {
			case "cube_all" -> new String[]{"all"};
			case "cube_column" -> new String[]{"side", "end"};
			case "cube_bottom_top" -> new String[]{"side", "bottom", "top"};
			case "slab", "slab_top", "stairs", "inner_stairs", "outer_stairs" -> new String[]{"side", "bottom", "top"};
			case "cross" -> new String[]{"cross"};
			default -> new String[]{"texture"};
		};
		for (int i = 0; i < textures.length; i++) model.texture(keys[i], textures[i]);
		return model;
	}
	public ModelAsset cubeAll(String n, Identifier t) { return template(n, "cube_all", t); }
	public ModelAsset cubeColumn(String n, Identifier s, Identifier e) { return template(n, "cube_column", s, e); }
	public ModelAsset cubeBottomTop(String n, Identifier s, Identifier b, Identifier t) { return template(n, "cube_bottom_top", s, b, t); }
	public ModelAsset slab(String n, Identifier s, Identifier b, Identifier t) { return template(n, "slab", s, b, t); }
	public ModelAsset slabTop(String n, Identifier s, Identifier b, Identifier t) { return template(n, "slab_top", s, b, t); }
	public ModelAsset stairs(String n, Identifier s, Identifier b, Identifier t) { return template(n, "stairs", s, b, t); }
	public ModelAsset stairsInner(String n, Identifier s, Identifier b, Identifier t) { return template(n, "inner_stairs", s, b, t); }
	public ModelAsset stairsOuter(String n, Identifier s, Identifier b, Identifier t) { return template(n, "outer_stairs", s, b, t); }
	public ModelAsset fencePost(String n, Identifier t) { return template(n, "fence_post", t); }
	public ModelAsset fenceSide(String n, Identifier t) { return template(n, "fence_side", t); }
	public ModelAsset fenceInventory(String n, Identifier t) { return template(n, "fence_inventory", t); }
	public ModelAsset fenceGate(String n, Identifier t) { return template(n, "template_fence_gate", t); }
	public ModelAsset fenceGateOpen(String n, Identifier t) { return template(n, "template_fence_gate_open", t); }
	public ModelAsset fenceGateWall(String n, Identifier t) { return template(n, "template_fence_gate_wall", t); }
	public ModelAsset fenceGateWallOpen(String n, Identifier t) { return template(n, "template_fence_gate_wall_open", t); }
	public ModelAsset wallInventory(String n, Identifier t) { return template(n, "wall_inventory", t).texture("wall", t); }
	public ModelAsset buttonInventory(String n, Identifier t) { return template(n, "button_inventory", t); }
	public ModelAsset trapdoorBottom(String n, Identifier t) { return template(n, "template_orientable_trapdoor_bottom", t).renderType("cutout"); }
	public ModelAsset cross(String n, Identifier t) { return template(n, "cross", t).renderType("cutout"); }
	public ModelAsset cube(String n, Identifier down, Identifier up, Identifier north, Identifier south, Identifier east, Identifier west) {
		return withExistingParent(n, mcLoc("block/cube")).texture("down", down).texture("up", up).texture("north", north).texture("south", south).texture("east", east).texture("west", west);
	}
	public void generic2d(ItemLike item) { generic2d(BuiltInRegistries.ITEM.getKey(item.asItem())); }
	public void generic2d(Identifier id) { layer0(id, "item/generated"); }
	public void handheld(Identifier id) { layer0(id, "item/handheld"); }
	public void layer0(Identifier id, String parent) { withExistingParent(id.toString(), mcLoc(parent)).texture("layer0", id.withPrefix("item/")); }
}
