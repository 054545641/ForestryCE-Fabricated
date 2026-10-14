package forestry.core.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;

import thedarkcolour.modkit.data.DataHelper;

/**
 * A provider under a name of the caller's choosing. {@link DataGenerator} keys a provider by
 * {@link DataProvider#getName()} and rejects a duplicate, and the recipe and loot table providers make
 * theirs final, so running the same kind of provider once per jar needs the name changed from outside.
 *
 * @param name     The name the generator keys this provider by
 * @param delegate The provider that does the work
 */
public record RenamedProvider(String name, DataProvider delegate) implements DataProvider {
	/**
	 * @param jar The jar every provider the registrar attaches belongs to. Ex. {@code "butterflies"}
	 * @return The registrar a per-jar {@link DataHelper} attaches its providers with
	 */
	public static ProviderRegistrar under(String jar) {
		return (generator, run, provider) ->
			generator.addProvider(run, new RenamedProvider(jar + "/" + provider.getName(), provider));
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		return this.delegate.run(output);
	}

	@Override
	public String getName() {
		return this.name;
	}
}
