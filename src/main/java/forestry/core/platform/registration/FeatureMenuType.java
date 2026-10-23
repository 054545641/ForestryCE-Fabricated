package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import forestry.core.platform.network.MenuOpeningData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

public class FeatureMenuType<M extends AbstractContainerMenu> extends ModFeature implements IMenuTypeFeature<M> {
	private final RegistryEntry<MenuType<?>, MenuType<M>> menuTypeObject;
    @FunctionalInterface public interface MenuFactory<M extends AbstractContainerMenu> {
        M create(int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.RegistryFriendlyByteBuf openingData);
    }

	public FeatureMenuType(IFeatureRegistry registry, Identifier moduleId, String identifier, MenuFactory<M> containerFactory) {
		super(moduleId, identifier);
		this.menuTypeObject = registry.getRegistry(Registries.MENU).register(identifier, () -> new ExtendedMenuType<>((id, inventory, data) -> {
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data.bytes()), inventory.player.registryAccess());
            try { return containerFactory.create(id, inventory, buffer); } finally { buffer.release(); }
        }, MenuOpeningData.CODEC));
	}

	@Override
	public ResourceKey<? extends Registry<?>> getRegistry() {
		return Registries.MENU;
	}

	@Override
	public MenuType<M> menuType() {
		return this.menuTypeObject.get();
	}
}
