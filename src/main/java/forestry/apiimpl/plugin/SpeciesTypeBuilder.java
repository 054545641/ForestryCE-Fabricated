package forestry.apiimpl.plugin;

import forestry.core.features.CoreItems;

import com.google.common.base.Preconditions;
import forestry.api.ForestryConstants;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.plugin.IKaryotypeBuilder;
import forestry.api.plugin.ISpeciesTypeBuilder;
import forestry.api.plugin.ISpeciesTypeFactory;
import forestry.core.engine.genetics.Karyotype;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SpeciesTypeBuilder implements ISpeciesTypeBuilder {
	private final ISpeciesTypeFactory typeFactory;
	private final Set<ILifeStage> stages;

	@Nullable
	private ILifeStage defaultStage = null;
	@Nullable
	private Consumer<IKaryotypeBuilder> karyotype = null;
	private Consumer<Reference2FloatMap<Item>> researchMaterials;

	public SpeciesTypeBuilder(ISpeciesTypeFactory typeFactory) {
		this.typeFactory = typeFactory;
		this.stages = new LinkedHashSet<>();

		// The default research materials across all species in Forestry. The comb is resolved by id
		// rather than named: it belongs to apiculture, and the escritoire has to work without it
		this.researchMaterials = map -> {
			map.put(CoreItems.HONEY_DROP.item(), 0.5f);
			map.put(CoreItems.HONEYDEW.item(), 0.7f);
			Item honeyComb = BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry("honey_comb"));
			if (honeyComb != Items.AIR) {
				map.put(honeyComb, 0.4f);
			}
		};
	}

	@Override
	public ISpeciesTypeBuilder setKaryotype(Consumer<IKaryotypeBuilder> karyotype) {
		if (this.karyotype == null) {
			this.karyotype = karyotype;
		} else {
			this.karyotype = this.karyotype.andThen(karyotype);
		}
		return this;
	}

	@Override
	public ISpeciesTypeBuilder addStages(ILifeStage... stages) {
		this.stages.addAll(Arrays.asList(stages));
		return this;
	}

	@Override
	public ISpeciesTypeBuilder setDefaultStage(ILifeStage stage) {
		this.defaultStage = stage;
		return this;
	}

	@Override
	public ISpeciesTypeBuilder addResearchMaterials(Consumer<Reference2FloatMap<Item>> materials) {
		this.researchMaterials = this.researchMaterials.andThen(materials);
		return this;
	}

	@Override
	public List<ILifeStage> getStages() {
		return List.copyOf(this.stages);
	}

	@Override
	public ILifeStage getDefaultStage() {
		Preconditions.checkState(this.defaultStage != null, "Missing default ILifeStage for species type");

		return this.defaultStage;
	}

	@Override
	public void buildResearchMaterials(Reference2FloatMap<Item> materialMap) {
		this.researchMaterials.accept(materialMap);
	}

	public ISpeciesType<?, ?> build(Identifier id) {
		Preconditions.checkState(this.karyotype != null, "Missing karyotype for species type");

		Karyotype.Builder builder = new Karyotype.Builder();
		this.karyotype.accept(builder);
		return this.typeFactory.create(builder.build(id), this);
	}
}
