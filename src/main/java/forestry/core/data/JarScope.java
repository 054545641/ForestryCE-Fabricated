package forestry.core.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;


/**
 * The pieces a content jar registers its providers from, assembled by {@link ContentJarData}. Every
 * one of them is already scoped to that jar, so a provider added here writes into the jar's own root
 * and is keyed by a name no other jar uses.
 *
 * <p>The gather event is held rather than handed out. Reaching it reaches the {@link DataGenerator},
 * and a provider attached straight to the generator keeps the name its own kind gives it, which is the
 * {@link DataProvider#getName()} collision {@link RenamedProvider} exists to prevent.
 */
public final class JarScope {
	private final GatherDataEvent event;
	private final PackOutput output;
	private final ScopedDataHelper helper;
	private final ProviderRegistrar registrar;

	JarScope(GatherDataEvent event, PackOutput output, ScopedDataHelper helper, ProviderRegistrar registrar) {
		this.event = event;
		this.output = output;
		this.helper = helper;
		this.registrar = registrar;
	}

	/**
	 * Adds a provider that runs when the data run includes server data.
	 *
	 * @param provider The provider to attach
	 */
	public void addServer(DataProvider provider) {
		this.registrar.addProvider(this.event.getGenerator(), true, provider);
	}

	/**
	 * Adds a provider that runs when the data run includes client data.
	 *
	 * @param provider The provider to attach
	 */
	public void addClient(DataProvider provider) {
		this.registrar.addProvider(this.event.getGenerator(), true, provider);
	}

	/**
	 * @return The root every provider of this jar writes to
	 */
	public PackOutput output() {
		return this.output;
	}

	/**
	 * @return The helper the ModKit-backed providers are created through
	 */
	public ScopedDataHelper helper() {
		return this.helper;
	}

	/**
	 * @return The lookup a provider resolves registry entries through
	 */
	public CompletableFuture<HolderLookup.Provider> lookup() {
		return this.event.getLookupProvider();
	}

}
