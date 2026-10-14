package forestry.core.data;

import java.util.Set;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.data.event.GatherDataEvent;


import forestry.api.ForestryConstants;

/**
 * The assembly every content jar's entry point shares. A subclass names its jar, the modules it
 * ships and its providers; the root, the registrar and the helper are the same three lines in each
 * of them.
 */
public abstract class ContentJarData implements IForestryDataProvider {
	private final String jar;
	private final String root;
	private final Set<Identifier> moduleIds;

	/**
	 * @param jar       The jar name the generator keys this jar's providers by. Ex. {@code "mail"}
	 * @param root      The generated resource root, one of the {@link DataRoots} constants
	 * @param moduleIds The modules this jar ships, which decide the ids it names
	 */
	protected ContentJarData(String jar, String root, Set<Identifier> moduleIds) {
		// A jar that claims nothing generates nothing and hands every one of its ids back to core,
		// which is never what was meant
		if (moduleIds.isEmpty()) {
			throw new IllegalArgumentException("Content jar " + jar + " declares no modules");
		}
		this.jar = jar;
		this.root = root;
		this.moduleIds = moduleIds;
	}

	@Override
	public final Set<Identifier> moduleIds() {
		return this.moduleIds;
	}

	@Override
	public final void gather(GatherDataEvent event) {
		PackOutput output = DataRoots.of(event, this.root);

		// The generator keys a provider by its name and rejects a duplicate, and core has already
		// registered a provider of most of these kinds under the name the kind gives itself. Every
		// provider here goes in under the jar's name instead
		ProviderRegistrar registrar = RenamedProvider.under(this.jar);

		// An item model is generated for an id this jar's modules registered and for nothing else, so
		// the model lands in the jar shipping the item it draws. English is not scoped this way; base
		// writes every key, content ids included. See Data#gatherData
		Set<Identifier> owned = JarModules.ownedIds(this.moduleIds);
		ScopedDataHelper helper = new ScopedDataHelper(ForestryConstants.MOD_ID, event, output, this.jar);

		addProviders(new JarScope(event, output, helper, registrar));
		registrar.addProvider(event.getGenerator(), true, new forestry.core.data.models.ForestryItemDefinitions(output, owned::contains));
		helper.attach();
	}

	/**
	 * Called once during core's gather event, with everything already scoped to this jar.
	 *
	 * @param jar The pieces this jar's providers are registered from
	 */
	protected abstract void addProviders(JarScope jar);
}
