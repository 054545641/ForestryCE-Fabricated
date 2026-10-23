package forestry.core.engine.genetics;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

public abstract class SpeciesManager<D extends ISpeciesDefinition> extends DefinitionManager<D> {
	protected SpeciesManager(String folder, Supplier<Codec<D>> codec, Consumer<Map<Identifier, D>> rebuild) {
		super(folder, codec, rebuild);
	}
}
