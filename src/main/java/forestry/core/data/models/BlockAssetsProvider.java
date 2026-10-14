package forestry.core.data.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Native 26.2 blockstate generators plus Forestry's named JSON model templates. */
public abstract class BlockAssetsProvider implements DataProvider {
	private final PackOutput output;
	private final Map<Identifier, ModelAsset> modelAssets = new LinkedHashMap<>();
	private final Map<Block, JsonElement> states = new LinkedHashMap<>();
	private final ModelAssets blocks = new ModelAssets(this.modelAssets, "block");
	private final ModelAssets items = new ModelAssets(this.modelAssets, "item");
	protected BlockAssetsProvider(PackOutput output) { this.output = output; }
	protected abstract void registerStatesAndModels();
	public ModelAssets models() { return this.blocks; }
	public ModelAssets itemModels() { return this.items; }
	public Identifier modLoc(String name) { return Identifier.fromNamespaceAndPath("forestry", name); }
	public Identifier mcLoc(String name) { return Identifier.withDefaultNamespace(name); }
	public Identifier blockTexture(Block block) { return BuiltInRegistries.BLOCK.getKey(block).withPrefix("block/"); }
	private static String name(Block block) { return BuiltInRegistries.BLOCK.getKey(block).getPath(); }
	private static MultiVariant variant(ModelAsset model) { return BlockModelGenerators.plainVariant(model.getLocation()); }
	private void accept(BlockModelDefinitionGenerator generator) {
		this.states.put(generator.block(), BlockStateModelDispatcher.CODEC.encodeStart(JsonOps.INSTANCE, generator.create()).getOrThrow());
	}
	public ModelAsset cubeAll(Block b) { return models().cubeAll(name(b), blockTexture(b)); }
	public void simpleBlock(Block b) { simpleBlock(b, cubeAll(b)); }
	public void simpleBlock(Block b, ModelAsset model) { accept(BlockModelGenerators.createSimpleBlock(b, variant(model))); }
	public void axisBlock(RotatedPillarBlock b, ModelAsset vertical, ModelAsset horizontal) {
		accept(BlockModelGenerators.createRotatedPillarWithHorizontalVariant(b, variant(vertical), variant(horizontal)));
	}
	public void stairsBlock(StairBlock b, Identifier t) { stairsBlock(b, models().stairs(name(b), t,t,t), models().stairsInner(name(b)+"_inner",t,t,t), models().stairsOuter(name(b)+"_outer",t,t,t)); }
	public void stairsBlock(StairBlock b, ModelAsset straight, ModelAsset inner, ModelAsset outer) { accept(BlockModelGenerators.createStairs(b, variant(inner), variant(straight), variant(outer))); }
	public void slabBlock(SlabBlock b, Identifier full, Identifier t) { slabBlock(b, models().slab(name(b),t,t,t), models().slabTop(name(b)+"_top",t,t,t), new ModelAsset(full)); }
	public void slabBlock(SlabBlock b, ModelAsset bottom, ModelAsset top, ModelAsset full) { accept(BlockModelGenerators.createSlab(b, variant(bottom),variant(top),variant(full))); }
	public void fourWayBlock(FenceBlock b, ModelAsset post, ModelAsset side) { accept(BlockModelGenerators.createFence(b,variant(post),variant(side))); }
	public void fenceGateBlock(FenceGateBlock b, ModelAsset closed, ModelAsset open, ModelAsset wall, ModelAsset wallOpen) { accept(BlockModelGenerators.createFenceGate(b,variant(open),variant(closed),variant(wallOpen),variant(wall),true)); }
	public void buttonBlock(ButtonBlock b, Identifier t) { accept(BlockModelGenerators.createButton(b,variant(models().template(name(b),"button",t)),variant(models().template(name(b)+"_pressed","button_pressed",t)))); }
	public void pressurePlateBlock(PressurePlateBlock b, Identifier t) { accept(BlockModelGenerators.createPressurePlate(b,variant(models().template(name(b),"pressure_plate_up",t)),variant(models().template(name(b)+"_down","pressure_plate_down",t)))); }
	public void wallBlock(WallBlock b, Identifier t) {
		accept(BlockModelGenerators.createWall(b, variant(models().template(name(b)+"_post","template_wall_post").texture("wall",t)), variant(models().template(name(b)+"_side","template_wall_side").texture("wall",t)), variant(models().template(name(b)+"_side_tall","template_wall_side_tall").texture("wall",t))));
	}
	public void doorBlock(DoorBlock b, Identifier bottom, Identifier top) {
		MultiVariant[] variants = new MultiVariant[8];
		String[] suffixes = {"bottom_left","bottom_left_open","bottom_right","bottom_right_open","top_left","top_left_open","top_right","top_right_open"};
		for (int i=0; i<8; i++) variants[i] = variant(models().withExistingParent(name(b)+"_"+suffixes[i],mcLoc("block/door_"+suffixes[i])).texture("bottom",bottom).texture("top",top).renderType("cutout"));
		accept(BlockModelGenerators.createDoor(b,variants[0],variants[1],variants[2],variants[3],variants[4],variants[5],variants[6],variants[7]));
	}
	public void trapdoorBlockWithRenderType(TrapDoorBlock b, Identifier t, boolean orientable, String renderType) {
		String template = orientable ? "template_orientable_trapdoor_" : "template_trapdoor_";
		var bottom = variant(models().template(name(b)+"_bottom",template+"bottom",t).renderType(renderType));
		var top = variant(models().template(name(b)+"_top",template+"top",t).renderType(renderType));
		var open = variant(models().template(name(b)+"_open",template+"open",t).renderType(renderType));
		accept(orientable ? BlockModelGenerators.createOrientableTrapdoor(b,top,bottom,open) : BlockModelGenerators.createTrapdoor(b,top,bottom,open));
	}
	public void directionalBlock(Block b, ModelAsset model) {
		getVariantBuilder(b).forAllStates(state -> {
			Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
			return ModelVariant.builder().modelFile(model).rotationX(facing==Direction.UP?0:facing==Direction.DOWN?180:90).rotationY(facing.getAxis().isVertical()?0:((int)facing.toYRot()+180)%360).build();
		});
	}
	public Variants getVariantBuilder(Block b) { return new Variants(b); }
	protected void blockStateJson(Block block, JsonObject json) { this.states.put(block,json); }
	public final class Variants {
		private final Block block;
		private final JsonObject variants = new JsonObject();
		Variants(Block b) { this.block=b; JsonObject root=new JsonObject(); root.add("variants",this.variants); states.put(b,root); }
		public void forAllStates(Function<BlockState, ModelVariant[]> factory) {
			for (BlockState state : this.block.getStateDefinition().getPossibleStates()) {
				String key = state.getValues().map(Object::toString).sorted().collect(Collectors.joining(","));
				put(key,factory.apply(state));
			}
		}
		private void put(String key, ModelVariant[] models) {
			if (models.length==1) this.variants.add(key,models[0].json());
			else { JsonArray array=new JsonArray(); for(var model:models) array.add(model.json()); this.variants.add(key,array); }
		}
		public Partial partialState() { return new Partial(); }
		public final class Partial {
			private final Map<String,String> values = new java.util.TreeMap<>();
			private ModelAsset model;
			public <T extends Comparable<T>> Partial with(Property<T> property,T value) { this.values.put(property.getName(),property.getName(value)); return this; }
			public Partial modelForState() { return this; }
			public Partial modelFile(ModelAsset model) { this.model=model; return this; }
			public Variants addModel() { setModels(ModelVariant.builder().modelFile(this.model).build()); return Variants.this; }
			public void setModels(ModelVariant... models) { put(this.values.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(Collectors.joining(",")),models); }
		}
	}
	@SuppressWarnings({"rawtypes","unchecked"})
	private static String valueName(Property property,Comparable value) { return property.getName(value); }
	@Override public CompletableFuture<?> run(CachedOutput cache) {
		this.modelAssets.clear(); this.states.clear(); registerStatesAndModels();
		var futures=new ArrayList<CompletableFuture<?>>();
		var modelPaths=this.output.createPathProvider(PackOutput.Target.RESOURCE_PACK,"models");
		var statePaths=this.output.createPathProvider(PackOutput.Target.RESOURCE_PACK,"blockstates");
		this.modelAssets.forEach((id,model)->futures.add(DataProvider.saveStable(cache,model.json,modelPaths.json(id))));
		this.states.forEach((block,json)-> {
			BlockStateModelDispatcher.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
			futures.add(DataProvider.saveStable(cache,json,statePaths.json(BuiltInRegistries.BLOCK.getKey(block))));
		});
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}
	@Override public String getName() { return getClass().getSimpleName(); }
}
