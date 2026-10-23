package forestry.core.engine.genetics;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.core.genetics.*;
import forestry.core.features.CoreDataComponents;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class Individual<S extends ISpecies<I>, I extends IIndividual, T extends ISpeciesType<S, I>> implements IIndividual {
	protected final S species;
	protected final S inactiveSpecies;
	protected final IGenome genome;

	@Nullable
	protected IGenome mate;
	protected boolean analyzed;

	protected Individual(IGenome genome) {
		this.species = genome.getActiveSpecies();
		this.inactiveSpecies = genome.getInactiveSpecies();
		this.genome = genome;
	}

	// For codec
	protected Individual(IGenome genome, Optional<IGenome> mate, boolean analyzed) {
		this(genome);

		this.mate = mate.orElse(null);
		this.analyzed = analyzed;
	}

	// For "inheritance" in codecs
	protected static <I extends IIndividual> Products.P3<RecordCodecBuilder.Mu<I>, IGenome, Optional<IGenome>, Boolean> fields(RecordCodecBuilder.Instance<I> instance, Codec<IGenome> genomeCodec) {
		return instance.group(
			genomeCodec.fieldOf("genome").forGetter(I::getGenome),
			genomeCodec.optionalFieldOf("mate").forGetter(I::getMateOptional),
			Codec.BOOL.fieldOf("analyzed").forGetter(I::isAnalyzed)
		);
	}

	@Override
	public void setMate(@Nullable IGenome mate) {
		if (mate == null || this.genome.getKaryotype() == mate.getKaryotype()) {
			this.mate = mate;
		}
	}

	@Nullable
	@Override
	public IGenome getMate() {
		return this.mate;
	}

	public Optional<IGenome> getMateOptional() {
		return Optional.ofNullable(this.mate);
	}

	@Override
	public IGenome getGenome() {
		return this.genome;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getType() {
		return (T) this.species.getType();
	}

	@Override
	public S getSpecies() {
		return this.genome.getActiveSpecies();
	}

	@Override
	public S getInactiveSpecies() {
		return this.genome.getInactiveSpecies();
	}

	@Override
	public boolean isAnalyzed() {
		return this.analyzed;
	}

	@Override
	public boolean analyze() {
		if (this.analyzed) {
			return false;
		}

		this.analyzed = true;
		return true;
	}

	@Override
	public I copy() {
		return copyWithGenome(this.genome);
	}

	@Override
	public I copyWithGenome(IGenome newGenome) {
		I individual = getSpecies().createIndividual(newGenome);
		copyPropertiesTo(individual);
		return individual;
	}

	@OverridingMethodsMustInvokeSuper
	protected void copyPropertiesTo(I other) {
		// todo should we copy the mate here? currently, /forestry bee modify erases the mate because it isn't copied here
	}

	@Override
	public void saveToStack(ItemStack stack) {
		stack.set(CoreDataComponents.GENOME.get(), this.genome);
		setOptional(stack, CoreDataComponents.MATE_GENOME.get(), this.mate);
		setOptional(stack, CoreDataComponents.ANALYZED.get(), this.analyzed ? Boolean.TRUE : null);
		savePropertiesToStack(stack);
	}

	protected void savePropertiesToStack(ItemStack stack) {
	}

	public void loadPropertiesFromStack(ItemStack stack) {
		setMate(stack.get(CoreDataComponents.MATE_GENOME.get()));
		this.analyzed = stack.getOrDefault(CoreDataComponents.ANALYZED.get(), Boolean.FALSE);
	}

	protected static <V> void setOptional(ItemStack stack, net.minecraft.core.component.DataComponentType<V> component, @Nullable V value) {
		if (value != null) {
			stack.set(component, value);
		} else {
			stack.remove(component);
		}
	}

	@Override
	public ItemStack createStack(ILifeStage stage) {
		ItemStack stack = new ItemStack(stage.getItemForm());
		saveToStack(stack);
		return stack;
	}
}
