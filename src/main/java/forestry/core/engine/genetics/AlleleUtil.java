package forestry.core.engine.genetics;

import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

public class AlleleUtil {
	@Nullable
	public static <S extends ISpecies<I>, I extends IIndividual> S getSpecies(ISpeciesType<S, I> speciesType, CompoundTag nbt, String key) {
		Identifier id = Identifier.tryParse(nbt.getStringOr(key, ""));
		return id == null ? null : speciesType.getSpecies(id);
	}
}
