package forestry.apiculture.plugin;

import com.google.common.collect.ImmutableList;
import forestry.api.apiculture.hives.IHive;
import forestry.api.apiculture.hives.IHiveDefinition;
import forestry.api.apiculture.hives.IHiveDrop;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.plugin.IHiveBuilder;
import forestry.apiculture.bees.genetics.HiveDrop;
import forestry.apiculture.hives.Hive;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class HiveBuilder implements IHiveBuilder {
	/** Documented default per IHiveDefinition javadoc: 1.0 = baseline chance, higher = more, lower = fewer. */
	public static final float DEFAULT_GEN_CHANCE = 1.0f;

	private final IHiveDefinition definition;
	private final ArrayList<IHiveDrop> drops = new ArrayList<>();
	private float generationChance = DEFAULT_GEN_CHANCE;

	public HiveBuilder(IHiveDefinition definition) {
		this.definition = definition;
	}

	@Override
	public IHiveBuilder addDrop(double chance, Identifier speciesId, Supplier<List<ItemStack>> extraItems, float ignobleChance, Map<IChromosome<?>, Allele<?>> alleles) {
		this.drops.add(new HiveDrop(chance, speciesId, extraItems, ignobleChance, alleles));
		return this;
	}

	@Override
	public IHiveBuilder addCustomDrop(IHiveDrop drop) {
		this.drops.add(drop);
		return this;
	}

	@Override
	public IHiveBuilder setGenerationChance(float generationChance) {
		this.generationChance = generationChance;
		return this;
	}

	public IHive build() {
		return new Hive(this.definition, this.generationChance, ImmutableList.copyOf(this.drops));
	}
}
