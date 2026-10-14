package forestry.apiimpl.client;

import forestry.api.client.IForestryClientApi;
import forestry.api.client.ITextureManager;
import forestry.api.client.apiculture.IBeeClientManager;
import forestry.api.client.arboriculture.ITreeClientManager;
import forestry.api.client.genetics.IGeneticClientManager;
import forestry.api.client.lepidopterology.IButterflyClientManager;
import forestry.api.client.plugin.IClientHelper;
import forestry.arboriculture.client.plugin.ClientHelper;
import forestry.core.platform.render.ForestryTextureManager;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public class ForestryClientApiImpl implements IForestryClientApi {
	// Constructed at field init rather than installed by a lifecycle hook, because ForestryLeafSprites
	// resolves the helper from a static initializer and runs before any of them
	private final IClientHelper helper = new ClientHelper();

	@Nullable
	private ITextureManager textureManager;
	@Nullable
	private IGeneticClientManager geneticManager;
	// All three are built from base classes during client plugin registration, butterflies included:
	// ButterflyClientManager keys textures by id and resolves an unregistered one by naming convention,
	// so an absent butterflies jar leaves it empty rather than absent. See PluginManager.registerClient
	@Nullable
	private IBeeClientManager beeManager;
	@Nullable
	private ITreeClientManager treeManager;
	@Nullable
	private IButterflyClientManager butterflyManager;

	@Override
	public ITextureManager getTextureManager() {
		if (this.textureManager == null) {
			throw new IllegalStateException("ITextureManager not initialized yet. Please wait until Minecraft constructor has been called");
		}
		return this.textureManager;
	}

	@Override
	public IGeneticClientManager getGeneticManager() {
		IGeneticClientManager manager = this.geneticManager;
		if (manager == null) {
			throw new IllegalStateException("IGeneticClientManager not initialized yet");
		}
		return manager;
	}

	public IBeeClientManager getBeeManager() {
		IBeeClientManager manager = this.beeManager;
		if (manager == null) {
			throw new IllegalStateException("IBeeClientManager not initialized yet");
		}
		return manager;
	}

	@Override
	public ITreeClientManager getTreeManager() {
		ITreeClientManager manager = this.treeManager;
		if (manager == null) {
			throw new IllegalStateException("ITreeClientManager not initialized yet");
		}
		return manager;
	}

	@Override
	public IButterflyClientManager getButterflyManager() {
		IButterflyClientManager manager = this.butterflyManager;
		if (manager == null) {
			throw new IllegalStateException("IButterflyClientManager not initialized yet");
		}
		return manager;
	}

	@Override
	public IClientHelper getHelper() {
		return this.helper;
	}

	// Must be called after textureManager is initialized in Minecraft's constructor.
	public void initializeTextureManager(AddClientReloadListenersEvent event) {
		this.textureManager = new ForestryTextureManager();
	}

	@ApiStatus.Internal
	public void setGeneticsManager(IGeneticClientManager treeManager) {
		this.geneticManager = treeManager;
	}

	@ApiStatus.Internal
	public void setTreeManager(ITreeClientManager treeManager) {
		this.treeManager = treeManager;
	}

	@ApiStatus.Internal
	public void setButterflyManager(IButterflyClientManager butterflyManager) {
		this.butterflyManager = butterflyManager;
	}

	@ApiStatus.Internal
	public void setBeeManager(IBeeClientManager beeManager) {
		this.beeManager = beeManager;
	}
}
