package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import forestry.core.platform.registration.RegistryEntry;

import java.util.Objects;
import java.util.function.Consumer;

public class FeatureCreativeTab extends ModFeature {
	private final RegistryEntry<CreativeModeTab, CreativeModeTab> creativeTabObject;

	public FeatureCreativeTab(IFeatureRegistry registry, Identifier moduleId, String name, Consumer<CreativeModeTab.Builder> builder) {
		super(moduleId, name);

		this.creativeTabObject = registry.getRegistry(Registries.CREATIVE_MODE_TAB).register(name, () -> {
			CreativeModeTab.Builder tab = net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder().title(Component.translatable("itemGroup." + name));
			builder.accept(tab);
			return tab.build();
		});
	}

	public CreativeModeTab creativeTab() {
		return this.creativeTabObject.get();
	}

	@Override
	public ResourceKey<? extends Registry<?>> getRegistry() {
		return Registries.CREATIVE_MODE_TAB;
	}

	public ResourceKey<CreativeModeTab> getKey() {
		return Objects.requireNonNull(this.creativeTabObject.getKey());
	}
}
