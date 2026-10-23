package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Collection;
import java.util.function.Supplier;

public class FeatureTileType<T extends BlockEntity> extends ModFeature implements ITileTypeFeature<T> {
	private final RegistryEntry<BlockEntityType<?>, BlockEntityType<T>> blockEntityObject;

	public FeatureTileType(IFeatureRegistry registry, Identifier moduleId, String name, BlockEntityType.BlockEntitySupplier<T> constructorTileEntity, Supplier<Collection<? extends Block>> validBlocks) {
		super(moduleId, name);
		this.blockEntityObject = registry.getRegistry(Registries.BLOCK_ENTITY_TYPE).register(name, () -> new BlockEntityType<>(constructorTileEntity, java.util.Set.copyOf(validBlocks.get())));
	}

	@Override
	public ResourceKey<? extends Registry<?>> getRegistry() {
		return Registries.BLOCK_ENTITY_TYPE;
	}

	@Override
	public BlockEntityType<T> tileType() {
		return this.blockEntityObject.get();
	}
}
