package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class FeatureEntityType<T extends Entity> extends ModFeature implements IEntityTypeFeature<T> {
	protected final Supplier<AttributeSupplier.Builder> attributes;
	protected final EntityType.EntityFactory<T> factory;
	protected final MobCategory classification;
	private final DeferredHolder<EntityType<?>, EntityType<T>> entityTypeObject;

	public FeatureEntityType(IFeatureRegistry registry, Identifier moduleId, String name, UnaryOperator<EntityType.Builder<T>> consumer, EntityType.EntityFactory<T> factory, MobCategory classification, Supplier<AttributeSupplier.Builder> attributes) {
		super(moduleId, name);
		this.factory = factory;
		this.attributes = attributes;
		this.classification = classification;
		this.entityTypeObject = registry.getRegistry(Registries.ENTITY_TYPE).register(name, () -> consumer.apply(EntityType.Builder.of(factory, classification)).build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(getModuleId().getNamespace(), name))));
	}

	@Override
	public AttributeSupplier.Builder createAttributes() {
		return this.attributes.get();
	}

	@Override
	public EntityType<T> entityType() {
		return this.entityTypeObject.get();
	}

	@Override
	public ResourceKey<? extends Registry<?>> getRegistry() {
		return Registries.ENTITY_TYPE;
	}
}
