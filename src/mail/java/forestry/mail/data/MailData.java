package forestry.mail.data;

import java.util.Set;

import net.minecraft.core.registries.Registries;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.data.ContentJarData;
import forestry.core.data.DataRoots;
import forestry.core.data.JarLootTableProvider;
import forestry.core.data.JarScope;

/**
 * Registers every provider that writes into the mail jar. Loaded by core through
 * {@code META-INF/services/forestry.core.data.IForestryDataProvider}.
 */
public class MailData extends ContentJarData {
	public MailData() {
		super("mail", DataRoots.MAIL, Set.of(ForestryModuleIds.MAIL));
	}

	@Override
	protected void addProviders(JarScope jar) {
		jar.helper().createTags(Registries.BLOCK, MailBlockTagsProvider::addTags);
		jar.helper().createTags(Registries.ITEM, MailItemTagsProvider::addTags);
		jar.helper().createForestryRecipes(MailRecipeProvider::addRecipes);
		jar.helper().createForestryItemModels(MailItemModels::addModels);

		jar.addServer(new JarLootTableProvider(jar.output(), jar.lookup(), MailBlockLootTables::new));
		jar.addServer(new MailDataMapProvider(jar.output(), jar.lookup()));
		jar.addClient(new MailBlockStateProvider(jar.output()));

	}
}
