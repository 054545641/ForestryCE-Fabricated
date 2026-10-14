package forestry.core.data.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/** 26.2 client item definitions, including Forestry's dynamic selection and tint sources. */
public final class ForestryItemDefinitions implements DataProvider {
	private static final List<String> MACHINES = List.of("peat_engine", "biogas_engine", "clockwork_engine", "combustion_engine", "solar_engine", "rainmaker", "apiarists_chest", "arborists_chest", "lepidopterists_chest");
	private final PackOutput output;
	private final Predicate<Identifier> ownsItem;
	public ForestryItemDefinitions(PackOutput output, Predicate<Identifier> ownsItem) { this.output=output; this.ownsItem=ownsItem; }
	@Override public CompletableFuture<?> run(CachedOutput cache) {
		var definitions = new LinkedHashMap<Identifier, JsonObject>();
		BuiltInRegistries.ITEM.forEach(item -> {
			Identifier id=BuiltInRegistries.ITEM.getKey(item);
			if (!id.getNamespace().equals("forestry") || !this.ownsItem.test(id)) return;
			Identifier model=item.components().get(DataComponents.ITEM_MODEL);
			if (model==null) throw new IllegalStateException("Missing item model component for " + id);
			definitions.put(model,definition(model));
		});
		var paths=this.output.createPathProvider(PackOutput.Target.RESOURCE_PACK,"items");
		return CompletableFuture.allOf(definitions.entrySet().stream().map(e->DataProvider.saveStable(cache,e.getValue(),paths.json(e.getKey()))).toArray(CompletableFuture[]::new));
	}
	private static JsonObject typed(String type) { var json=new JsonObject(); json.addProperty("type",type); return json; }
	private static JsonArray tints(String type,int count) {
		var result=new JsonArray();
		for(int i=0;i<count;i++) { var tint=typed(type); tint.addProperty("tint_index",i); result.add(tint); }
		return result;
	}
	private static JsonObject simple(String model) { var json=typed("minecraft:model"); json.addProperty("model",model); json.add("tints",tints("forestry:item",5)); return json; }
	public static JsonObject definition(Identifier id) {
		String name=id.getPath();
		JsonObject model=simple(id.withPrefix("item/").toString());
		if (MACHINES.contains(name)) {
			model=typed("minecraft:special"); model.addProperty("base",id.withPrefix("item/").toString());
			var special=typed("forestry:machine"); special.addProperty("kind",name); model.add("model",special);
		} else if (List.of("drone_bee","princess_bee","queen_bee").contains(name)) {
			model=typed("forestry:bee_ge"); model.addProperty("stage",name.substring(0,name.indexOf('_')));
		} else if (name.equals("tree_sapling")) model=typed("forestry:sapling_ge");
		else if (name.equals("butterfly")) model=typed("forestry:butterfly_ge");
		else if (name.startsWith("crated_")) { model=typed("forestry:filled_crate"); model.addProperty("model",id.withPrefix("item/").toString()); }
		else if (name.startsWith("bucket_")) {
			model=typed("neoforge:fluid_container"); model.addProperty("fluid","forestry:"+name.substring(7)); model.addProperty("flip_gas",true);
			var textures=new JsonObject(); textures.addProperty("base","minecraft:item/bucket"); textures.addProperty("fluid","neoforge:item/mask/bucket_fluid"); model.add("textures",textures);
		} else if (List.of("can","wax_capsule","refractory_capsule").contains(name)) {
			model=typed("forestry:fluid_container"); model.addProperty("fluid","minecraft:empty");
			String texture=name.equals("can")?"can":name.equals("wax_capsule")?"capsule":"refractory";
			var textures=new JsonObject(); textures.addProperty("base","forestry:item/liquids/"+texture+".bottle"); textures.addProperty("fluid","forestry:item/liquids/"+texture+".contents"); model.add("textures",textures);
		} else if (name.endsWith("_backpack")) {
			String prefix="forestry:item/backpack/"+(name.startsWith("woven_")?"woven_":"normal_");
			model=range("forestry:backpack_mode",prefix+"neutral",List.of(prefix+"locked",prefix+"receive",prefix+"resupply"));
		} else if (name.equals("cocoon")) model=range("forestry:age","forestry:item/cocoon_early",List.of("forestry:item/cocoon_middle","forestry:item/cocoon_late"));
		else if (name.matches("stamp_[0-9]+n")) model.add("tints",tints("forestry:mail_stamp",2));
		else if (name.equals("turf") || name.equals("turf_block")) {
			var tint=typed("minecraft:grass"); tint.addProperty("temperature",0.5); tint.addProperty("downfall",1.0);
			var tints=new JsonArray(); tints.add(tint); model.add("tints",tints);
		}
		var result=new JsonObject(); result.add("model",model); return result;
	}
	private static JsonObject range(String property,String fallback,List<String> models) {
		var result=typed("minecraft:range_dispatch"); result.addProperty("property",property); result.add("fallback",simple(fallback));
		var entries=new JsonArray();
		for(int i=0;i<models.size();i++) { var entry=new JsonObject(); entry.addProperty("threshold",i+1); entry.add("model",simple(models.get(i))); entries.add(entry); }
		result.add("entries",entries); return result;
	}
	@Override public String getName() { return "Forestry client item definitions"; }
}
