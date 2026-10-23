package forestry.arboriculture.features;

import forestry.arboriculture.entities.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/** Registers the real production classes under test-only IDs. */
public final class ArboricultureEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "forestry_environment");
    public static final Feature<ForestryBoat> BOAT = new Feature<>(TYPES.register("n5_boat", id ->
        EntityType.Builder.<ForestryBoat>of(ForestryBoat::new, MobCategory.MISC).sized(1.375f, .5625f).build(ResourceKey.create(Registries.ENTITY_TYPE,id))));
    public static final Feature<ForestryChestBoat> CHEST_BOAT = new Feature<>(TYPES.register("n5_chest_boat", id ->
        EntityType.Builder.<ForestryChestBoat>of(ForestryChestBoat::new, MobCategory.MISC).sized(1.375f, .5625f).build(ResourceKey.create(Registries.ENTITY_TYPE,id))));
    public record Feature<T extends Entity>(Supplier<EntityType<T>> supplier) { public EntityType<T> entityType() { return supplier.get(); } }
}

